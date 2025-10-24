package vn.viettel.vds.promotion.validation.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.viettel.vds.promotion.validation.adapter.out.integration.ValidationEngineDeploymentService;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.AssignmentEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.AssignmentJpaRepository;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.ValidationRuleJpaRepository;
import vn.viettel.vds.promotion.validation.command.RollbackValidationRuleCommand;
import vn.viettel.vds.promotion.validation.command.RollbackValidationRuleCommand.RollbackValidationRuleCommandPayload;
import vn.viettel.vds.promotion.validation.domain.exception.ValidationException;

import java.time.Instant;
import java.util.List;

/**
 * Service to handle RollbackValidationRuleCommand for saga compensation
 * Responsible for rolling back validation rule assignments when saga fails
 */
@Service
@Transactional
public class RollbackValidationRuleCommandHandler {

    private static final Logger logger = LoggerFactory.getLogger(RollbackValidationRuleCommandHandler.class);

    private final AssignmentJpaRepository assignmentRepository;
    private final ValidationRuleJpaRepository validationRuleRepository;
    @SuppressWarnings("unused") // Reserved for future use
    private final ValidationEngineDeploymentService validationEngineClient;
    private final SettingValidationRuleEventPublisher eventPublisher;
    private final IdempotencyService idempotencyService;

    public RollbackValidationRuleCommandHandler(
            AssignmentJpaRepository assignmentRepository,
            ValidationRuleJpaRepository validationRuleRepository,
            ValidationEngineDeploymentService validationEngineClient,
            SettingValidationRuleEventPublisher eventPublisher,
            IdempotencyService idempotencyService) {
        this.assignmentRepository = assignmentRepository;
        this.validationRuleRepository = validationRuleRepository;
        this.validationEngineClient = validationEngineClient;
        this.eventPublisher = eventPublisher;
        this.idempotencyService = idempotencyService;
    }

    /**
     * Handle rollback command from campaign service during saga compensation
     *
     * @param command The rollback command containing campaign ID and rollback details
     * @return true if rollback successful, false otherwise
     */
    public boolean handleRollback(RollbackValidationRuleCommand command) {
        String commandId = command.getId();

        try {
            logger.info("Processing RollbackValidationRuleCommand: commandId={}", commandId);

            // Check idempotency - if already processed, return success immediately
            if (idempotencyService.isProcessed(commandId)) {
                logger.info("Rollback command already processed (idempotent check): commandId={}", commandId);
                return true;
            }

            // Extract command payload
            RollbackValidationRuleCommandPayload payload = command.getPayload();
            if (payload == null) {
                logger.error("Rollback command payload is null: commandId={}", commandId);
                publishRollbackErrorEvent(commandId, "INVALID_PAYLOAD", "Command payload is missing");
                return false;
            }

            String campaignId = payload.getCampaignId();
            String validationRuleId = payload.getValidationRuleId() != null ?
                    payload.getValidationRuleId() : null;
            boolean rollbackAll = payload.getRollbackAll();

            logger.info("Rollback request: campaignId={}, validationRuleId={}, rollbackAll={}",
                    campaignId, validationRuleId, rollbackAll);

            // Execute rollback logic
            boolean success = executeRollback(campaignId, validationRuleId, rollbackAll);

            if (success) {
                // Mark as processed after successful rollback
                idempotencyService.markAsProcessed(commandId, "Rollback completed successfully");

                // Publish success event
                publishRollbackSuccessEvent(commandId, campaignId, validationRuleId);

                logger.info("Successfully processed RollbackValidationRuleCommand: commandId={}, campaignId={}",
                        commandId, campaignId);
                return true;
            } else {
                publishRollbackErrorEvent(commandId, "ROLLBACK_FAILED", "Failed to rollback validation rule assignment");
                logger.error("Failed to process RollbackValidationRuleCommand: commandId={}, campaignId={}",
                        commandId, campaignId);
                return false;
            }

        } catch (Exception e) {
            logger.error("Unexpected error processing RollbackValidationRuleCommand: commandId={}", commandId, e);
            publishRollbackErrorEvent(commandId, "PROCESSING_ERROR", "Unexpected error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Execute the actual rollback logic
     *
     * @param campaignId       The campaign ID to rollback assignments for
     * @param validationRuleId Specific assignment ID to rollback (optional)
     * @param rollbackAll      If true, rollback all assignments for the campaign
     * @return true if successful, false otherwise
     */
    private boolean executeRollback(String campaignId, String validationRuleId, boolean rollbackAll) {
        try {
            // Find assignments by campaign ID
            List<AssignmentEntity> assignments = findAssignmentsByCampaignId(campaignId);

            if (assignments.isEmpty()) {
                logger.warn("No assignments found for campaign: campaignId={}", campaignId);
                // Not an error - assignment might not have been created yet
                return true;
            }

            // Filter assignments if specific ID provided
            if (!rollbackAll && validationRuleId != null) {
                assignments = assignments.stream()
                        .filter(a -> validationRuleId.equals(a.getId()))
                        .toList();

                if (assignments.isEmpty()) {
                    logger.warn("No assignment found with specific ID: validationRuleId={}", validationRuleId);
                    return true;
                }
            }

            logger.info("Found {} assignment(s) to rollback for campaign: campaignId={}", assignments.size(), campaignId);

            // Rollback each assignment
            for (AssignmentEntity assignment : assignments) {
                rollbackAssignment(assignment);
            }

            return true;

        } catch (Exception e) {
            logger.error("Error executing rollback for campaign: campaignId={}", campaignId, e);
            return false;
        }
    }

    /**
     * Rollback a single assignment
     * Marks it as INACTIVE and undeploys from validation-engine
     *
     * @param assignment The assignment to rollback
     */
    private void rollbackAssignment(AssignmentEntity assignment) {
        try {
            logger.info("Rolling back assignment: assignmentId={}, ruleId={}, campaignId={}",
                    assignment.getId(), assignment.getId(), assignment.getEntityId());

            // Mark assignment as INACTIVE (soft delete)
            assignment.setActive(false);
            assignment.setUpdatedAt(Instant.now());
            assignmentRepository.save(assignment);

            logger.info("Marked assignment as inactive: assignmentId={}", assignment.getId());

            // Undeploy rule from validation-engine
            undeployRuleFromEngine(assignment);

            logger.info("Successfully rolled back assignment: assignmentId={}", assignment.getId());

        } catch (Exception e) {
            throw new ValidationException("Failed to rollback assignment: " + assignment.getId() + " - " + e.getMessage(), e);
        }
    }

    /**
     * Find assignments by campaign ID
     * Searches for assignments where entityType = "campaign" AND entityId = campaignId
     *
     * @param campaignId The campaign ID to search for
     * @return List of assignments for the campaign
     */
    private List<AssignmentEntity> findAssignmentsByCampaignId(String campaignId) {
        // Query assignments where entityType = "campaign" AND entityId = campaignId
        return assignmentRepository.findAll().stream()
                .filter(a -> "campaign".equalsIgnoreCase(a.getEntityType()))
                .filter(a -> campaignId.equals(a.getEntityId()))
                .toList();
    }

    /**
     * Undeploy rule from validation-engine
     *
     * @param assignment The assignment containing the rule to undeploy
     */
    private void undeployRuleFromEngine(AssignmentEntity assignment) {
        try {
            String ruleId = assignment.getId();

            // Get the validation rule details
            var ruleOpt = validationRuleRepository.findById(ruleId);
            if (ruleOpt.isEmpty()) {
                handleMissingRule(ruleId);
                return;
            }
            // Rule validation handled within undeployRuleFromEngine
            removeRuleFromEngine(ruleId, assignment);

        } catch (Exception e) {
            handleUndeployException(e, assignment);
        }
    }

    private void handleMissingRule(String ruleId) {
        logger.warn("Validation rule not found for undeployment: ruleId={}", ruleId);
    }

    private void removeRuleFromEngine(String ruleId, AssignmentEntity assignment) {
        logger.info("Removing rule from validation-engine: ruleId={}, assignmentId={}",
                ruleId, assignment.getId());

        // Note: removeRule() is deprecated and does nothing.
        // Bundle management is now automatic in validation-engine.
        logger.info("Rule removal requested (automatic management): ruleId={}, assignmentId={}",
                ruleId, assignment.getId());
    }

    private void handleUndeployException(Exception e, AssignmentEntity assignment) {
        logger.error("Error undeploying rule from validation-engine: assignmentId={}",
                assignment.getId(), e);
        // Don't fail the entire rollback for undeployment issues
    }

    /**
     * Publish rollback success event
     */
    private void publishRollbackSuccessEvent(String commandId, String campaignId, String validationRuleId) {
        try {
            eventPublisher.publishRollbackSuccessEvent(commandId, campaignId, validationRuleId);
        } catch (Exception e) {
            logger.error("Failed to publish rollback success event: commandId={}", commandId, e);
        }
    }

    /**
     * Publish rollback error event
     */
    private void publishRollbackErrorEvent(String commandId, String errorCode, String errorMessage) {
        try {
            eventPublisher.publishRollbackErrorEvent(commandId, errorCode, errorMessage);
        } catch (Exception e) {
            logger.error("Failed to publish rollback error event: commandId={}", commandId, e);
        }
    }

    /**
     * Wrapper method for unified consumer interface compatibility.
     * Delegates to handleRollback for backward compatibility.
     *
     * @param command The rollback command
     * @return true if rollback successful, false otherwise
     */
    public boolean handleCommand(RollbackValidationRuleCommand command) {
        return handleRollback(command);
    }

    /**
     * Handle dead letter command from DLQ topic.
     * Logs the failed command for manual investigation and alerting.
     *
     * @param command The failed rollback command from DLQ
     */
    public void handleDeadLetterCommand(RollbackValidationRuleCommand command) {
        logger.error("Processing dead letter RollbackValidationRuleCommand: commandId={}, campaignId={}, reason={}",
                command.getId(),
                command.getPayload() != null ? command.getPayload().getCampaignId() : "unknown",
                command.getPayload() != null ? command.getPayload().getRollbackReason() : "unknown");

        // Log detailed information for debugging
        if (command.getPayload() != null) {
            logger.error("Dead letter rollback details: validationRuleId={}, rollbackAll={}, correlationId={}",
                    command.getPayload().getValidationRuleId(),
                    command.getPayload().getRollbackAll(),
                    command.getPayload().getCorrelationId());
        }

        // Could trigger alerting system, store in DB for manual processing, etc.
        // For now, just log and acknowledge
        publishRollbackErrorEvent(
                command.getId(),
                "DLQ_PROCESSING",
                "Command moved to dead letter queue after multiple retries"
        );
    }
}
