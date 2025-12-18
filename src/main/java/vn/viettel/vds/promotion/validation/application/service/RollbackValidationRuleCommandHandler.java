package vn.viettel.vds.promotion.validation.application.service;

import com.promix.platform.core.error.ErrorDetail;
import com.promix.platform.core.exception.BusinessException;
import com.promix.platform.core.exception.factory.ExceptionFactory;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.viettel.vds.promotion.validation.adapter.in.messaging.dto.RollbackValidationRuleCommandDTO;
import vn.viettel.vds.promotion.validation.adapter.in.messaging.mapper.RollbackValidationRuleCommandDTOMapper;
import vn.viettel.vds.promotion.validation.adapter.out.integration.ValidationEngineDeploymentService;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.AssignmentEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.AssignmentJpaRepository;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.ValidationRuleJpaRepository;
import vn.viettel.vds.promotion.validation.command.RollbackValidationRuleCommand;
import vn.viettel.vds.promotion.validation.command.RollbackValidationRuleCommand.RollbackValidationRuleCommandPayload;
import vn.viettel.vds.promotion.validation.domain.exception.ValidationException;

import java.time.Instant;
import java.util.List;
import java.util.Set;

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
    private final Validator validator;
    private final RollbackValidationRuleCommandDTOMapper dtoMapper;

    public RollbackValidationRuleCommandHandler(
            AssignmentJpaRepository assignmentRepository,
            ValidationRuleJpaRepository validationRuleRepository,
            ValidationEngineDeploymentService validationEngineClient,
            SettingValidationRuleEventPublisher eventPublisher,
            IdempotencyService idempotencyService,
            Validator validator,
            RollbackValidationRuleCommandDTOMapper dtoMapper) {
        this.assignmentRepository = assignmentRepository;
        this.validationRuleRepository = validationRuleRepository;
        this.validationEngineClient = validationEngineClient;
        this.eventPublisher = eventPublisher;
        this.idempotencyService = idempotencyService;
        this.validator = validator;
        this.dtoMapper = dtoMapper;
    }

    /**
     * Handle rollback command from campaign service during saga compensation
     *
     * @param command The rollback command containing campaign ID and rollback details
     * @return true if rollback successful, false otherwise
     */
    @SuppressWarnings("java:S2139") // Exception is properly logged before rethrowing
    public boolean handleRollback(RollbackValidationRuleCommand command) {
        String commandId = command.getId();

        try {
            logger.info("Processing RollbackValidationRuleCommand: commandId={}", commandId);

            // Check idempotency - if already processed, return success immediately
            if (idempotencyService.isProcessed(commandId)) {
                logger.info("Rollback command already processed (idempotent check): commandId={}", commandId);
                return true;
            }

            // Step 1: Validate command using Bean Validation
            validateCommand(command);

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

            // Handle failure - throw BusinessException with specific error code
            if (!success) {
                String errorCode = "ROLLBACK_FAILED";
                String errorMessage = "Failed to rollback validation rule assignment";
                publishRollbackErrorEvent(commandId, errorCode, errorMessage);
                logger.error("Failed to process RollbackValidationRuleCommand: commandId={}, campaignId={}, errorCode={}",
                        commandId, campaignId, errorCode);
                throw ExceptionFactory.createValidationException(errorCode, errorMessage);
            }

            // Mark as processed after successful rollback
            idempotencyService.markAsProcessed(commandId, "Rollback completed successfully");

            // Publish success event
            publishRollbackSuccessEvent(commandId, campaignId, validationRuleId);

            logger.info("Successfully processed RollbackValidationRuleCommand: commandId={}, campaignId={}",
                    commandId, campaignId);
            return true;

        } catch (BusinessException e) {
            // Re-throw BusinessException (validation errors) to let promix-messaging handle it
            // BusinessException with BAD_REQUEST → DLQ immediately (non-retryable)
            logger.error("Validation failed for RollbackValidationRuleCommand: commandId={}, error={}",
                    commandId, e.getMessage(), e);
            throw e; // NOSONAR - Exception is logged before rethrowing for proper error tracking
        } catch (Exception e) {
            logger.error("Unexpected error processing RollbackValidationRuleCommand: commandId={}", commandId, e);
            publishRollbackErrorEvent(commandId, "PROCESSING_ERROR", "Unexpected error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Validate command using Bean Validation annotations on DTO.
     * <p>
     * Validation flow:
     * 1. Check command and payload not null
     * 2. Convert command to DTO
     * 3. Run Bean Validation with group sequence
     * 4. Throw ValidationException if validation fails
     *
     * @param command the command to validate
     * @throws RuntimeException if validation fails with specific error codes
     */
    private void validateCommand(RollbackValidationRuleCommand command) {
        // Step 1: Null check
        if (command == null || command.getPayload() == null) {
            logger.error("Received null command or null payload");
            throw ExceptionFactory.createValidationException(
                    "INVALID_COMMAND",
                    "Command or payload is null"
            );
        }

        // Step 2: Convert to DTO
        RollbackValidationRuleCommandDTO dto = dtoMapper.toDTO(command);
        if (dto == null) {
            logger.error("Failed to convert command to DTO: commandId={}", command.getId());
            throw ExceptionFactory.createValidationException(
                    "INVALID_COMMAND",
                    "Failed to convert command to DTO"
            );
        }

        // Step 3: Bean Validation
        Set<ConstraintViolation<RollbackValidationRuleCommandDTO>> violations = validator.validate(dto);

        // Step 4: Handle validation errors
        if (!violations.isEmpty()) {
            // Build ErrorDetail list from all violations
            List<ErrorDetail> errorDetails = violations.stream()
                    .map(violation -> ErrorDetail.of(
                            violation.getPropertyPath().toString(),  // field
                            violation.getMessage(),                  // errorCode (from annotation)
                            String.format("Invalid value: %s", violation.getInvalidValue()),  // message
                            violation.getInvalidValue()             // details
                    ))
                    .toList();

            // Build summary error message
            String errorMessage = String.format("Validation failed with %d error(s)", violations.size());

            logger.error("RollbackValidationRuleCommand validation failed: commandId={}, errorCount={}, errors={}",
                    command.getId(), violations.size(), errorDetails);

            throw ExceptionFactory.createValidationException(
                    "METHOD_ARGUMENT_NOT_VALID",
                    errorMessage,
                    errorDetails.toArray(new ErrorDetail[0])
            );
        }

        logger.debug("RollbackValidationRuleCommand validation passed: commandId={}", command.getId());
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
        // Use optimized query method instead of findAll() + filter in memory
        return assignmentRepository.findByEntityTypeAndEntityId("campaign", campaignId);
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
