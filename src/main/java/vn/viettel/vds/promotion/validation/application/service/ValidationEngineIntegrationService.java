package vn.viettel.vds.promotion.validation.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import vn.viettel.vds.promotion.validation.adapter.out.integration.ValidationEngineDeploymentService;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.AssignmentEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.ValidationRuleEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.AssignmentJpaRepository;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.ValidationRuleJpaRepository;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service for managing integration and synchronization with validation-engine
 */
@Service
public class ValidationEngineIntegrationService {

    private static final Logger logger = LoggerFactory.getLogger(ValidationEngineIntegrationService.class);

    private static final String STATUS_CONNECTED = "Connected";
    private static final String STATUS_DISCONNECTED = "Disconnected";
    private static final String STATUS_ERROR_PREFIX = "Error: ";

    private final ValidationEngineDeploymentService validationEngineClient;
    private final ValidationRuleJpaRepository validationRuleRepository;
    private final AssignmentJpaRepository assignmentRepository;

    public ValidationEngineIntegrationService(
            ValidationEngineDeploymentService validationEngineClient,
            ValidationRuleJpaRepository validationRuleRepository,
            AssignmentJpaRepository assignmentRepository) {
        this.validationEngineClient = validationEngineClient;
        this.validationRuleRepository = validationRuleRepository;
        this.assignmentRepository = assignmentRepository;
    }

    /**
     * Synchronize all active rules with validation-engine
     */
    @Async("validationEngineExecutor")
    public void synchronizeAllRules() {
        logger.info("Starting full rule synchronization with validation-engine");

        List<AssignmentEntity> activeAssignments = assignmentRepository.findByActive(true);

        long successCount = activeAssignments.stream()
                .filter(this::synchronizeAssignment)
                .count();

        logger.info("Rule synchronization completed: success={}, failures={}, total={}",
                successCount, activeAssignments.size() - successCount, activeAssignments.size());

        // Note: reloadRules() is deprecated and does nothing.
        // Bundle management is now automatic in validation-engine.
    }

    private boolean synchronizeAssignment(AssignmentEntity assignment) {
        try {
            var validationRuleOpt = validationRuleRepository.findById(assignment.getId());
            if (validationRuleOpt.isEmpty()) {
                logger.warn("Validation rule not found for active assignment: ruleId={}, assignmentId={}",
                        assignment.getId(), assignment.getId());
                return false;
            }

            ValidationRuleEntity rule = validationRuleOpt.get();

            // Note: deployRule() is deprecated and does nothing.
            // Actual rule deployment is handled by RulePublishingService.publishRule()
            logger.debug("Rule assignment found for synchronization: ruleId={}, assignmentId={}",
                    rule.getId(), assignment.getId());
            return true;

        } catch (Exception e) {
            logger.error("Error synchronizing rule for assignment: assignmentId={}",
                    assignment.getId(), e);
            return false;
        }
    }

    /**
     * Deploy a specific rule assignment to validation-engine
     */
    @Async("validationEngineExecutor")
    public CompletableFuture<Boolean> deployRuleAssignment(String assignmentId) {
        logger.info("Deploying specific rule assignment: assignmentId={}", assignmentId);

        try {
            // Get the assignment
            var assignmentOpt = assignmentRepository.findById(assignmentId);
            if (assignmentOpt.isEmpty()) {
                logger.warn("Assignment not found: assignmentId={}", assignmentId);
                return CompletableFuture.completedFuture(false);
            }

            AssignmentEntity assignment = assignmentOpt.get();

            // Only deploy if active
            if (assignment.getActive() == null || !assignment.getActive()) {
                logger.info("Skipping deployment - assignment is not active: assignmentId={}", assignmentId);
                return CompletableFuture.completedFuture(false);
            }

            // Get the validation rule
            var validationRuleOpt = validationRuleRepository.findById(assignment.getId());
            if (validationRuleOpt.isEmpty()) {
                logger.warn("Validation rule not found for assignment: ruleId={}, assignmentId={}",
                        assignment.getId(), assignmentId);
                return CompletableFuture.completedFuture(false);
            }

            ValidationRuleEntity rule = validationRuleOpt.get();

            // Note: deployRule() is deprecated and does nothing.
            // Actual rule deployment is handled by RulePublishingService.publishRule()
            logger.info("Rule assignment ready for deployment: ruleId={}, assignmentId={}",
                    rule.getId(), assignmentId);

            return CompletableFuture.completedFuture(true);

        } catch (Exception e) {
            logger.error("Error deploying rule assignment: assignmentId={}", assignmentId, e);
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * Remove a rule from validation-engine
     */
    @Async("validationEngineExecutor")
    public CompletableFuture<Boolean> removeRule(String ruleId) {
        logger.info("Removing rule from validation-engine: ruleId={}", ruleId);

        try {
            // Note: removeRule() is deprecated and does nothing.
            // Bundle management is now automatic in validation-engine.
            logger.info("Rule removal requested (automatic management): ruleId={}", ruleId);
            return CompletableFuture.completedFuture(true);

        } catch (Exception e) {
            logger.error("Error removing rule from validation-engine: ruleId={}", ruleId, e);
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * Check validation-engine health and connectivity
     */
    public boolean isValidationEngineHealthy() {
        try {
            return validationEngineClient.isHealthy();
        } catch (Exception e) {
            logger.error("Error checking validation-engine health", e);
            return false;
        }
    }

    /**
     * Get synchronization status
     */
    public SynchronizationStatus getSynchronizationStatus() {
        try {
            // Count active assignments (tenantId removed from schema)
            long activeAssignments = assignmentRepository.countByActive(true);

            // Check validation-engine health
            boolean engineHealthy = isValidationEngineHealthy();

            return new SynchronizationStatus(
                    activeAssignments,
                    engineHealthy,
                    engineHealthy ? STATUS_CONNECTED : STATUS_DISCONNECTED
            );

        } catch (Exception e) {
            logger.error("Error getting synchronization status", e);
            return new SynchronizationStatus(0, false, STATUS_ERROR_PREFIX + e.getMessage());
        }
    }
}