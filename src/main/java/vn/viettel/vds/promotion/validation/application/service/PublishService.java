package vn.viettel.vds.promotion.validation.application.service;

import com.promix.platform.core.error.ResponseInfo;
import com.promix.platform.core.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.viettel.vds.promotion.validation.application.port.out.PublishJobPersistencePort;
import vn.viettel.vds.promotion.validation.application.port.out.RuleTemporalLinkPersistencePort;
import vn.viettel.vds.promotion.validation.application.port.out.RuleVersionPersistencePort;
import vn.viettel.vds.promotion.validation.domain.model.PublishJob;
import vn.viettel.vds.promotion.validation.domain.model.Rule;
import vn.viettel.vds.promotion.validation.domain.model.RuleNode;
import vn.viettel.vds.promotion.validation.domain.model.RuleTemporalLink;
import vn.viettel.vds.promotion.validation.domain.model.RuleVersion;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@Transactional
public class PublishService {

    private static final Logger logger = LoggerFactory.getLogger(PublishService.class);

    private final RuleService ruleService;
    private final RuleVersionPersistencePort ruleVersionPersistencePort;
    private final PublishJobPersistencePort publishJobPersistencePort;
    private final RuleTemporalLinkPersistencePort ruleTemporalLinkPersistencePort;
    private final OperatorService operatorService;
    private final RuleValidationService ruleValidationService;
    private final OutboxEventService outboxEventService;
    private final AuditService auditService;

    public PublishService(RuleService ruleService,
                          RuleVersionPersistencePort ruleVersionPersistencePort,
                          PublishJobPersistencePort publishJobPersistencePort,
                          RuleTemporalLinkPersistencePort ruleTemporalLinkPersistencePort,
                          OperatorService operatorService,
                          RuleValidationService ruleValidationService,
                          OutboxEventService outboxEventService,
                          AuditService auditService) {
        this.ruleService = ruleService;
        this.ruleVersionPersistencePort = ruleVersionPersistencePort;
        this.publishJobPersistencePort = publishJobPersistencePort;
        this.ruleTemporalLinkPersistencePort = ruleTemporalLinkPersistencePort;
        this.operatorService = operatorService;
        this.ruleValidationService = ruleValidationService;
        this.outboxEventService = outboxEventService;
        this.auditService = auditService;
    }

    /**
     * Publish a rule (create new version and compile)
     */
    public PublishJob publishRule(String ruleId, String publishedBy, PublishOptions options) {
        logger.info("Publishing rule: id={}, publishedBy={}", ruleId, publishedBy);

        Rule rule = ruleService.getRuleById(ruleId);

        // Validate rule can be published
        validateRuleForPublishing(rule);

        // Get next version number
        Integer nextVersion = getNextVersionNumber(rule.getTenantId(), ruleId);

        // Check if job already exists for this version
        if (publishJobPersistencePort.existsByTenantIdAndRuleIdAndTargetVersion(
                rule.getTenantId(), ruleId, nextVersion)) {
            throw new BusinessException(new ResponseInfo("PUBLISH_JOB_EXISTS",
                    "Publish job already exists for rule " + ruleId + " version " + nextVersion, 400));
        }

        // Create publish job
        PublishJob job = createPublishJob(rule, nextVersion, publishedBy, options);

        // Start async publishing process
        CompletableFuture.runAsync(() -> executePublishJob(job.getId()));

        return job;
    }

    /**
     * Get publish job status
     */
    @Transactional(readOnly = true)
    public PublishJob getPublishJob(String jobId) {
        return publishJobPersistencePort.findById(jobId)
                .orElseThrow(() -> new BusinessException(new ResponseInfo("PUBLISH_JOB_NOT_FOUND", "Publish job not found: " + jobId, 404)));
    }

    /**
     * Get publish jobs for a rule
     */
    @Transactional(readOnly = true)
    public List<PublishJob> getPublishJobsForRule(String tenantId, String ruleId) {
        return publishJobPersistencePort.findByTenantIdAndRuleIdOrderByTargetVersionDesc(tenantId, ruleId);
    }

    /**
     * Cancel a running publish job
     */
    public PublishJob cancelPublishJob(String jobId, String cancelledBy) {
        logger.info("Cancelling publish job: id={}, cancelledBy={}", jobId, cancelledBy);

        PublishJob job = getPublishJob(jobId);

        if (job.getStatus() != PublishJob.JobStatus.RUNNING) {
            throw new BusinessException(new ResponseInfo("CANNOT_CANCEL_JOB",
                    "Cannot cancel job in status: " + job.getStatus(), 400));
        }

        List<String> updatedErrors = new ArrayList<>(job.getErrors() != null ? job.getErrors() : List.of());
        updatedErrors.add("Cancelled by " + cancelledBy);

        PublishJob updatedJob = job.toBuilder()
                .status(PublishJob.JobStatus.FAILED)
                .completedAt(Instant.now())
                .errors(updatedErrors)
                .build();

        return publishJobPersistencePort.save(updatedJob);
    }

    private void validateRuleForPublishing(Rule rule) {
        // Validate rule state
        if (rule.getState() == Rule.RuleState.ARCHIVED) {
            throw new BusinessException(new ResponseInfo("CANNOT_PUBLISH_ARCHIVED",
                    "Cannot publish archived rule: " + rule.getId(), 400));
        }

        // Validate rule structure and operators
        RuleValidationService.ValidationResult validation =
                ruleValidationService.validateRuleForPublishing(rule.getTenantId(), rule);

        if (!validation.isValid()) {
            String errors = validation.getIssues().stream()
                    .map(issue -> issue.getPath() + ": " + issue.getMessage())
                    .reduce((a, b) -> a + "; " + b)
                    .orElse("Unknown validation error");

            throw new BusinessException(new ResponseInfo("RULE_VALIDATION_FAILED",
                    "Rule validation failed: " + errors, 400));
        }
    }

    private Integer getNextVersionNumber(String tenantId, String ruleId) {
        return ruleVersionPersistencePort.findFirstByTenantIdAndRuleIdOrderByVersionDesc(tenantId, ruleId)
                .map(rv -> rv.getRuleVersion() + 1)
                .orElse(1);
    }

    private PublishJob createPublishJob(Rule rule, Integer targetVersion, String publishedBy, PublishOptions options) {
        Instant now = Instant.now();

        // Calculate operators fingerprint
        List<String> operatorNames = rule.getNodes().stream()
                .filter(node -> node.getType() == RuleNode.NodeType.COND)
                .map(RuleNode::getOperatorName)
                .filter(name -> name != null)
                .distinct()
                .toList();

        String operatorsFingerprint = operatorService.calculateOperatorsFingerprint(rule.getTenantId(), operatorNames);

        // Set up compilation info
        PublishJob.CompileJobInfo compileInfo = PublishJob.CompileJobInfo.builder()
                .compilerId(options != null ? options.getCompilerId() : "default_compiler_v1")
                .operatorsFingerprint(operatorsFingerprint)
                .build();

        PublishJob job = PublishJob.builder()
                .id(generateJobId(rule.getTenantId(), rule.getId(), targetVersion))
                .tenantId(rule.getTenantId())
                .ruleId(rule.getId())
                .targetVersion(targetVersion)
                .status(PublishJob.JobStatus.RUNNING)
                .requestedBy(publishedBy)
                .requestedAt(now)
                .compile(compileInfo)
                .createdAt(now)
                .version(0L)
                .build();

        return publishJobPersistencePort.save(job);
    }

    private void executePublishJob(String jobId) {
        try {
            logger.info("Executing publish job: id={}", jobId);

            PublishJob job = getPublishJob(jobId);
            Rule rule = ruleService.getRuleById(job.getRuleId());

            // Create rule version snapshot
            RuleVersion ruleVersion = createRuleVersionSnapshot(rule, job);

            // Simulate compilation (in real implementation, this would call Artifact Service)
            boolean compilationSuccess = simulateCompilation(job, ruleVersion);

            if (compilationSuccess) {
                // Mark rule as published
                ruleService.markRuleAsPublished(rule.getId(), job.getTargetVersion());

                // Complete the job
                Instant completedTime = Instant.now();
                PublishJob completedJob = job.toBuilder()
                        .status(PublishJob.JobStatus.SUCCESS)
                        .completedAt(completedTime)
                        .build();
                publishJobPersistencePort.save(completedJob);

                // Publish outbox event
                Map<String, Object> eventPayload = Map.of(
                        "ruleId", rule.getId(),
                        "ruleVersion", job.getTargetVersion(),
                        "code", rule.getCode(),
                        "publishedBy", job.getRequestedBy(),
                        "publishedAt", job.getCompletedAt().toString()
                );

                outboxEventService.createEvent(
                        "Rule",                          // aggregateType
                        rule.getId(),                    // aggregateId
                        "rule.published",                // eventType
                        eventPayload,                    // payload
                        "http://validation-events",      // destination
                        Map.of("tenantId", rule.getTenantId()), // metadata
                        3                                // maxAttempts
                );

                // Log audit event
                auditService.logRulePublished(rule.getTenantId(), rule.getId(), job.getRequestedBy(),
                        Map.of("version", job.getTargetVersion(), "jobId", job.getId()));

                logger.info("Publish job completed successfully: id={}, version={}", jobId, job.getTargetVersion());

            } else {
                // Mark job as failed
                List<String> failureErrors = new ArrayList<>(job.getErrors() != null ? job.getErrors() : List.of());
                failureErrors.add("Compilation failed");

                PublishJob failedJob = job.toBuilder()
                        .status(PublishJob.JobStatus.FAILED)
                        .completedAt(Instant.now())
                        .errors(failureErrors)
                        .build();
                publishJobPersistencePort.save(failedJob);

                logger.error("Publish job failed: id={}", jobId);
            }

        } catch (Exception e) {
            logger.error("Error executing publish job: id={}", jobId, e);

            try {
                PublishJob job = getPublishJob(jobId);
                List<String> executionErrors = new ArrayList<>(job.getErrors() != null ? job.getErrors() : List.of());
                executionErrors.add("Execution error: " + e.getMessage());

                PublishJob errorJob = job.toBuilder()
                        .status(PublishJob.JobStatus.FAILED)
                        .completedAt(Instant.now())
                        .errors(executionErrors)
                        .build();
                publishJobPersistencePort.save(errorJob);
            } catch (Exception saveError) {
                logger.error("Error updating failed job status: id={}", jobId, saveError);
            }
        }
    }

    private RuleVersion createRuleVersionSnapshot(Rule rule, PublishJob job) {
        Instant now = Instant.now();

        // Convert RuleNode objects to Map representation for storage
        List<Map<String, Object>> nodesMaps = rule.getNodes() != null
                ? rule.getNodes().stream()
                        .map(this::convertRuleNodeToMap)
                        .toList()
                : List.of();

        RuleVersion ruleVersion = RuleVersion.builder()
                .id(generateRuleVersionId(rule.getTenantId(), rule.getCode(), job.getTargetVersion()))
                .tenantId(rule.getTenantId())
                .ruleId(rule.getId())
                .code(rule.getCode())
                .ruleVersion(job.getTargetVersion())
                .logic(rule.getLogic() != null ? RuleVersion.LogicType.valueOf(rule.getLogic().name()) : null)
                .limits(convertUsageLimitsToMap(rule.getLimits()))
                .nodes(nodesMaps)
                .operatorsFingerprint(job.getCompile().getOperatorsFingerprint())
                .publishedAt(now)
                .publishedBy(job.getRequestedBy())
                .createdAt(now)
                .version(0L)
                .build();

        return ruleVersionPersistencePort.save(ruleVersion);
    }

    private Map<String, Object> convertUsageLimitsToMap(Rule.UsageLimits limits) {
        if (limits == null) return null;

        Map<String, Object> map = new HashMap<>();
        if (limits.getPerCodeTotal() != null) {
            map.put("perCodeTotal", limits.getPerCodeTotal());
        }
        if (limits.getPerCustomer() != null) {
            map.put("perCustomer", limits.getPerCustomer());
        }
        if (limits.getPerDay() != null) {
            map.put("perDay", limits.getPerDay());
        }
        if (limits.getPerTransaction() != null) {
            map.put("perTransaction", limits.getPerTransaction());
        }
        if (limits.getRemaining() != null) {
            map.put("remaining", limits.getRemaining());
        }
        return map;
    }

    private Map<String, Object> convertRuleNodeToMap(RuleNode node) {
        Map<String, Object> map = new HashMap<>();
        if (node.getNodeId() != null) map.put("nodeId", node.getNodeId());
        if (node.getField() != null) map.put("field", node.getField());
        if (node.getOperator() != null) map.put("operator", node.getOperator());
        if (node.getValue() != null) map.put("value", node.getValue());
        if (node.getLogicType() != null) map.put("logicType", node.getLogicType().name());
        if (node.getType() != null) map.put("type", node.getType().name());
        if (node.getOperatorName() != null) map.put("operatorName", node.getOperatorName());
        if (node.getReasonCode() != null) map.put("reasonCode", node.getReasonCode());
        if (node.getDescription() != null) map.put("description", node.getDescription());
        if (node.getParams() != null) map.put("params", node.getParams());
        if (node.getChildren() != null && !node.getChildren().isEmpty()) {
            List<Map<String, Object>> childrenMaps = node.getChildren().stream()
                    .map(this::convertRuleNodeToMap)
                    .toList();
            map.put("children", childrenMaps);
        }
        return map;
    }

    private boolean simulateCompilation(PublishJob job, RuleVersion ruleVersion) {
        // In a real implementation, this would:
        // 1. Call Artifact Service to compile the rule
        // 2. Get back bundle hash and compilation logs
        // 3. Update the rule version and job with compilation results

        try {
            // Simulate compilation delay
            Thread.sleep(2000);

            // Simulate successful compilation
            String bundleHash = "sha256:" + java.util.UUID.randomUUID().toString().replace("-", "");
            List<String> compilationLogs = List.of("Compilation started", "Operators resolved", "Bundle created successfully");

            // Update rule version with compilation results
            RuleVersion.CompileInfo compileInfo = RuleVersion.CompileInfo.builder()
                    .status(RuleVersion.CompileInfo.CompileStatus.SUCCESS)
                    .compilerId(job.getCompile().getCompilerId())
                    .bundleHash(bundleHash)
                    .logs(compilationLogs)
                    .build();

            RuleVersion updatedRuleVersion = ruleVersion.toBuilder()
                    .compile(compileInfo)
                    .build();
            ruleVersionPersistencePort.save(updatedRuleVersion);

            return true;

        } catch (Exception e) {
            logger.error("Compilation simulation failed", e);
            return false;
        }
    }

    private String generateJobId(String tenantId, String ruleId, Integer version) {
        return "pj_" + tenantId + "_" + ruleId + "_v" + version;
    }

    private String generateRuleVersionId(String tenantId, String code, Integer version) {
        return "rv_" + tenantId + "_" + code + "_v" + version;
    }

    /**
     * Publishing options
     */
    public static class PublishOptions {
        private String compilerId;
        private boolean pinTemporalLinks = true;
        private boolean pinOperatorFingerprint = true;
        private String note;

        // Getters and setters
        public String getCompilerId() {
            return compilerId;
        }

        public void setCompilerId(String compilerId) {
            this.compilerId = compilerId;
        }

        public boolean isPinTemporalLinks() {
            return pinTemporalLinks;
        }

        public void setPinTemporalLinks(boolean pinTemporalLinks) {
            this.pinTemporalLinks = pinTemporalLinks;
        }

        public boolean isPinOperatorFingerprint() {
            return pinOperatorFingerprint;
        }

        public void setPinOperatorFingerprint(boolean pinOperatorFingerprint) {
            this.pinOperatorFingerprint = pinOperatorFingerprint;
        }

        public String getNote() {
            return note;
        }

        public void setNote(String note) {
            this.note = note;
        }
    }
}