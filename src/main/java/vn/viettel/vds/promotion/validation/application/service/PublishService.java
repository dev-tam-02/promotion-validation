package vn.viettel.vds.promotion.validation.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.viettel.vds.promotion.validation.application.port.out.PublishJobPersistencePort;
import vn.viettel.vds.promotion.validation.application.port.out.RuleVersionPersistencePort;
import vn.viettel.vds.promotion.validation.domain.exception.*;
import vn.viettel.vds.promotion.validation.domain.model.PublishJob;
import vn.viettel.vds.promotion.validation.domain.model.Rule;
import vn.viettel.vds.promotion.validation.domain.model.RuleNode;
import vn.viettel.vds.promotion.validation.domain.model.RuleVersion;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
@Transactional
public class PublishService {

    private static final Logger logger = LoggerFactory.getLogger(PublishService.class);

    private final RuleService ruleService;
    private final RuleVersionPersistencePort ruleVersionPersistencePort;
    private final PublishJobPersistencePort publishJobPersistencePort;
    private final OperatorService operatorService;
    private final RuleValidationService ruleValidationService;
    private final PublishService self;

    public PublishService(RuleService ruleService,
                          RuleVersionPersistencePort ruleVersionPersistencePort,
                          PublishJobPersistencePort publishJobPersistencePort,
                          OperatorService operatorService,
                          RuleValidationService ruleValidationService,
                          @org.springframework.context.annotation.Lazy PublishService self) {
        this.ruleService = ruleService;
        this.ruleVersionPersistencePort = ruleVersionPersistencePort;
        this.publishJobPersistencePort = publishJobPersistencePort;
        this.operatorService = operatorService;
        this.ruleValidationService = ruleValidationService;
        this.self = self;
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
        Integer nextVersion = getNextVersionNumber(ruleId);

        // Check if job already exists for this version
        if (publishJobPersistencePort.existsByRuleIdAndTargetVersion(ruleId, nextVersion)) {
            throw new PublishJobAlreadyExistsException(ruleId, nextVersion);
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
                .orElseThrow(() -> new PublishJobNotFoundException(jobId));
    }

    /**
     * Get publish jobs for a rule
     */
    @Transactional(readOnly = true)
    public List<PublishJob> getPublishJobsForRule(String ruleId) {
        return publishJobPersistencePort.findByRuleIdOrderByTargetVersionDesc(ruleId);
    }

    /**
     * Cancel a running publish job
     */
    public PublishJob cancelPublishJob(String jobId, String cancelledBy) {
        logger.info("Cancelling publish job: id={}, cancelledBy={}", jobId, cancelledBy);

        PublishJob job = self.getPublishJob(jobId);

        if (job.getStatus() != PublishJob.JobStatus.RUNNING) {
            throw new InvalidPublishJobStateException("cancel", job.getStatus().name());
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
        // VRUL001: no lifecycle state — an inactive (archived) rule cannot publish.
        if (!rule.isActive()) {
            throw new ArchivedRulePublishException(rule.getId());
        }

        // Validate rule structure and operators
        RuleValidationService.ValidationResult validation =
                ruleValidationService.validateRuleForPublishing(rule);

        if (!validation.isValid()) {
            String errors = validation.getIssues().stream()
                    .map(issue -> issue.getPath() + ": " + issue.getMessage())
                    .reduce((a, b) -> a + "; " + b)
                    .orElse("Unknown validation error");

            throw new RuleValidationFailedException(rule.getId(), errors);
        }
    }

    private Integer getNextVersionNumber(String ruleId) {
        return ruleVersionPersistencePort.findFirstByRuleIdOrderByVersionDesc(ruleId)
                .map(rv -> rv.getVersion() + 1)
                .orElse(1);
    }

    private PublishJob createPublishJob(Rule rule, Integer targetVersion, String publishedBy, PublishOptions options) {
        Instant now = Instant.now();

        // Calculate operators fingerprint
        List<String> operatorNames = rule.getNodes().stream()
                .filter(node -> node.getType() == RuleNode.NodeType.COND)
                .map(RuleNode::getOperatorName)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        String operatorsFingerprint = operatorService.calculateOperatorsFingerprint("default", operatorNames);

        // Set up compilation info
        PublishJob.CompileJobInfo compileInfo = PublishJob.CompileJobInfo.builder()
                .compilerId(options != null ? options.getCompilerId() : "default_compiler_v1")
                .operatorsFingerprint(operatorsFingerprint)
                .build();

        PublishJob job = PublishJob.builder()
                .id(generateJobId(rule.getId(), targetVersion))
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

            PublishJob job = self.getPublishJob(jobId);
            Rule rule = ruleService.getRuleById(job.getId());

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

                // Không phát event "rule.published" nữa: đó là event orphan (payload Map thô,
                // type không đăng ký trong ValidationEvent @JsonSubTypes, không consumer nào) —
                // nếu phát sẽ gây lỗi consumer rule-engine trên topic validation dùng chung.
                // Việc publish rule đã đổi trạng thái qua ruleService.markRuleAsPublished ở trên.

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
                PublishJob job = self.getPublishJob(jobId);
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
                .id(generateRuleVersionId(rule.getCode(), job.getTargetVersion()))
                .ruleId(rule.getId())
                .code(rule.getCode())
                .version(job.getTargetVersion())
                .logic(rule.getLogic() != null ? RuleVersion.LogicType.valueOf(rule.getLogic().name()) : null)
                .limits(convertUsageLimitsToMap(rule.getLimits()))
                .nodes(nodesMaps)
                .operatorsFingerprint(job.getCompile().getOperatorsFingerprint())
                .publishedAt(now)
                .publishedBy(job.getRequestedBy())
                .createdAt(now)
                .entityVersion(0L)
                .build();

        return ruleVersionPersistencePort.save(ruleVersion);
    }

    private Map<String, Object> convertUsageLimitsToMap(Rule.UsageLimits limits) {
        if (limits == null) return new HashMap<>();

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

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Compilation simulation interrupted", e);
            return false;
        } catch (Exception e) {
            logger.error("Compilation simulation failed", e);
            return false;
        }
    }

    private String generateJobId(String ruleId, Integer version) {
        return "pj_" + ruleId + "_v" + version;
    }

    private String generateRuleVersionId(String code, Integer version) {
        return "rv_" + code + "_v" + version;
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