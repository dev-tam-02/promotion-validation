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
import vn.viettel.vds.promotion.validation.domain.model.RuleTemporalLink;
import vn.viettel.vds.promotion.validation.domain.model.RuleVersion;
import com.promix.platform.outbox.service.OutboxService;

import java.time.Instant;
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
    private final OutboxService outboxService;
    private final AuditService auditService;

    public PublishService(RuleService ruleService,
                          RuleVersionPersistencePort ruleVersionPersistencePort,
                          PublishJobPersistencePort publishJobPersistencePort,
                          RuleTemporalLinkPersistencePort ruleTemporalLinkPersistencePort,
                          OperatorService operatorService,
                          RuleValidationService ruleValidationService,
                          OutboxService outboxService,
                          AuditService auditService) {
        this.ruleService = ruleService;
        this.ruleVersionPersistencePort = ruleVersionPersistencePort;
        this.publishJobPersistencePort = publishJobPersistencePort;
        this.ruleTemporalLinkPersistencePort = ruleTemporalLinkPersistencePort;
        this.operatorService = operatorService;
        this.ruleValidationService = ruleValidationService;
        this.outboxService = outboxService;
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

        job.setStatus(PublishJob.JobStatus.FAILED);
        job.setCompletedAt(Instant.now());
        job.getErrors().add("Cancelled by " + cancelledBy);

        return publishJobPersistencePort.save(job);
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
                .map(rv -> rv.getVersion() + 1)
                .orElse(1);
    }

    private PublishJob createPublishJob(Rule rule, Integer targetVersion, String publishedBy, PublishOptions options) {
        PublishJob job = new PublishJob();
        job.setId(generateJobId(rule.getTenantId(), rule.getId(), targetVersion));
        job.setTenantId(rule.getTenantId());
        job.setRuleId(rule.getId());
        job.setTargetVersion(targetVersion);
        job.setStatus(PublishJob.JobStatus.RUNNING);
        job.setRequestedBy(publishedBy);
        job.setRequestedAt(Instant.now());

        // Set up compilation info
        PublishJob.CompileJobInfo compileInfo = new PublishJob.CompileJobInfo();
        compileInfo.setCompilerId(options != null ? options.getCompilerId() : "default_compiler_v1");

        // Calculate operators fingerprint
        List<String> operatorNames = rule.getNodes().stream()
                .filter(node -> node.getType() == RuleNode.NodeType.COND)
                .map(RuleNode::getOperatorName)
                .filter(name -> name != null)
                .distinct()
                .toList();

        String operatorsFingerprint = operatorService.calculateOperatorsFingerprint(rule.getTenantId(), operatorNames);
        compileInfo.setOperatorsFingerprint(operatorsFingerprint);

        job.setCompile(compileInfo);

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
                job.setStatus(PublishJob.JobStatus.SUCCESS);
                job.setCompletedAt(Instant.now());
                publishJobPersistencePort.save(job);

                // Publish outbox event
                Map<String, Object> eventPayload = Map.of(
                        "ruleId", rule.getId(),
                        "ruleVersion", job.getTargetVersion(),
                        "code", rule.getCode(),
                        "publishedBy", job.getRequestedBy(),
                        "publishedAt", job.getCompletedAt().toString()
                );

                outboxService.createEvent(
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
                job.setStatus(PublishJob.JobStatus.FAILED);
                job.setCompletedAt(Instant.now());
                job.getErrors().add("Compilation failed");
                publishJobPersistencePort.save(job);

                logger.error("Publish job failed: id={}", jobId);
            }

        } catch (Exception e) {
            logger.error("Error executing publish job: id={}", jobId, e);

            try {
                PublishJob job = getPublishJob(jobId);
                job.setStatus(PublishJob.JobStatus.FAILED);
                job.setCompletedAt(Instant.now());
                job.getErrors().add("Execution error: " + e.getMessage());
                publishJobPersistencePort.save(job);
            } catch (Exception saveError) {
                logger.error("Error updating failed job status: id={}", jobId, saveError);
            }
        }
    }

    private RuleVersion createRuleVersionSnapshot(Rule rule, PublishJob job) {
        RuleVersion ruleVersion = new RuleVersion();
        ruleVersion.setId(generateRuleVersionId(rule.getTenantId(), rule.getCode(), job.getTargetVersion()));
        ruleVersion.setTenantId(rule.getTenantId());
        ruleVersion.setRuleId(rule.getId());
        ruleVersion.setCode(rule.getCode());
        ruleVersion.setVersion(job.getTargetVersion());
        ruleVersion.setLogic(rule.getLogic());
        ruleVersion.setLimits(rule.getLimits());
        ruleVersion.setNodes(rule.getNodes()); // Deep copy in real implementation
        ruleVersion.setOperatorsFingerprint(job.getCompile().getOperatorsFingerprint());

        // Snapshot temporal links
        List<RuleTemporalLink> currentLinks = ruleTemporalLinkPersistencePort.findByRuleId(
                rule.getId());

        List<RuleVersion.TimeLink> timeLinks = currentLinks.stream()
                .map(link -> {
                    RuleVersion.TimeLink timeLink = new RuleVersion.TimeLink();
                    timeLink.setPolicyId(link.getPolicyId());
                    timeLink.setMode(RuleVersion.TimeLink.TimeLinkMode.valueOf(link.getMode().name()));
                    return timeLink;
                })
                .toList();

        ruleVersion.setTimeLinks(timeLinks);
        ruleVersion.setPublishedAt(Instant.now());
        ruleVersion.setPublishedBy(job.getRequestedBy());

        return ruleVersionPersistencePort.save(ruleVersion);
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

            // Update rule version with compilation results
            RuleVersion.CompileInfo compileInfo = new RuleVersion.CompileInfo();
            compileInfo.setStatus(RuleVersion.CompileInfo.CompileStatus.SUCCESS);
            compileInfo.setCompilerId(job.getCompile().getCompilerId());
            compileInfo.setBundleHash(bundleHash);
            compileInfo.setLogs(List.of("Compilation started", "Operators resolved", "Bundle created successfully"));

            ruleVersion.setCompile(compileInfo);
            ruleVersionPersistencePort.save(ruleVersion);

            // Update job with bundle hash
            job.getCompile().setBundleHash(bundleHash);
            job.getCompile().setLogs(compileInfo.getLogs());

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