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
    public CompletableFuture<Void> synchronizeAllRules() {
        logger.info("Starting full rule synchronization with validation-engine");

        try {
            // Get all active assignments (validFrom/validTo removed from schema)
            List<AssignmentEntity> activeAssignments = assignmentRepository.findByActive(true);

            int successCount = 0;
            int failureCount = 0;

            for (AssignmentEntity assignment : activeAssignments) {
                try {
                    // Get the validation rule
                    var validationRuleOpt = validationRuleRepository.findById(assignment.getId());
                    if (validationRuleOpt.isEmpty()) {
                        logger.warn("Validation rule not found for active assignment: ruleId={}, assignmentId={}",
                                assignment.getId(), assignment.getId());
                        failureCount++;
                        continue;
                    }

                    ValidationRuleEntity rule = validationRuleOpt.get();

                    // Deploy to validation-engine
                    boolean deployed = validationEngineClient.deployRule(rule);
                    if (deployed) {
                        successCount++;
                        logger.debug("Successfully synchronized rule: ruleId={}, assignmentId={}",
                                rule.getId(), assignment.getId());
                    } else {
                        failureCount++;
                        logger.error("Failed to synchronize rule: ruleId={}, assignmentId={}",
                                rule.getId(), assignment.getId());
                    }

                } catch (Exception e) {
                    failureCount++;
                    logger.error("Error synchronizing rule for assignment: assignmentId={}",
                            assignment.getId(), e);
                }
            }

            logger.info("Rule synchronization completed: success={}, failures={}, total={}",
                    successCount, failureCount, activeAssignments.size());

            // Trigger rules reload in validation-engine
            try {
                validationEngineClient.reloadRules();
                logger.info("Triggered rule reload in validation-engine");
            } catch (Exception e) {
                logger.error("Failed to trigger rule reload in validation-engine", e);
            }

        } catch (Exception e) {
            logger.error("Error during rule synchronization", e);
        }

        return CompletableFuture.completedFuture(null);
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

            // Deploy to validation-engine
            boolean deployed = validationEngineClient.deployRule(rule);
            if (deployed) {
                logger.info("Successfully deployed rule assignment: ruleId={}, assignmentId={}",
                        rule.getId(), assignmentId);
            } else {
                logger.error("Failed to deploy rule assignment: ruleId={}, assignmentId={}",
                        rule.getId(), assignmentId);
            }

            return CompletableFuture.completedFuture(deployed);

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
            boolean removed = validationEngineClient.removeRule(ruleId);
            if (removed) {
                logger.info("Successfully removed rule from validation-engine: ruleId={}", ruleId);
            } else {
                logger.error("Failed to remove rule from validation-engine: ruleId={}", ruleId);
            }

            return CompletableFuture.completedFuture(removed);

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
                    engineHealthy ? "Connected" : "Disconnected"
            );

        } catch (Exception e) {
            logger.error("Error getting synchronization status", e);
            return new SynchronizationStatus(0, false, "Error: " + e.getMessage());
        }
    }

    /**
     * Synchronization status information
     */
    public static class SynchronizationStatus {
        private final long activeRuleAssignments;
        private final boolean validationEngineHealthy;
        private final String status;

        public SynchronizationStatus(long activeRuleAssignments, boolean validationEngineHealthy, String status) {
            this.activeRuleAssignments = activeRuleAssignments;
            this.validationEngineHealthy = validationEngineHealthy;
            this.status = status;
        }

        public long getActiveRuleAssignments() {
            return activeRuleAssignments;
        }

        public boolean isValidationEngineHealthy() {
            return validationEngineHealthy;
        }

        public String getStatus() {
            return status;
        }
    }
}