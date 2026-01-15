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
import vn.viettel.vds.promotion.validation.application.port.out.RuleBindingPersistencePort;
import vn.viettel.vds.promotion.validation.application.port.out.ValidationEnginePort;
import vn.viettel.vds.promotion.validation.application.port.out.ValidationRuleRepositoryPort;
import vn.viettel.vds.promotion.validation.domain.model.RuleBinding;
import vn.viettel.vds.promotion.validation.command.RollbackValidationRuleCommand;
import vn.viettel.vds.promotion.validation.command.RollbackValidationRuleCommand.RollbackValidationRuleCommandPayload;
import vn.viettel.vds.promotion.validation.domain.exception.ValidationException;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Service to handle RollbackValidationRuleCommand for saga compensation.
 * <p>
 * Refactored to use unified RuleBinding model.
 */
@Service
@Transactional
public class RollbackValidationRuleCommandHandler {

    private static final Logger logger = LoggerFactory.getLogger(RollbackValidationRuleCommandHandler.class);

    private final RuleBindingPersistencePort ruleBindingPort;
    private final ValidationRuleRepositoryPort validationRulePort;
    @SuppressWarnings("unused")
    private final ValidationEnginePort validationEnginePort;
    private final SettingValidationRuleEventPublisher eventPublisher;
    private final IdempotencyService idempotencyService;
    private final Validator validator;
    private final RollbackValidationRuleCommandDTOMapper dtoMapper;

    public RollbackValidationRuleCommandHandler(
            RuleBindingPersistencePort ruleBindingPort,
            ValidationRuleRepositoryPort validationRulePort,
            ValidationEnginePort validationEnginePort,
            SettingValidationRuleEventPublisher eventPublisher,
            IdempotencyService idempotencyService,
            Validator validator,
            RollbackValidationRuleCommandDTOMapper dtoMapper) {
        this.ruleBindingPort = ruleBindingPort;
        this.validationRulePort = validationRulePort;
        this.validationEnginePort = validationEnginePort;
        this.eventPublisher = eventPublisher;
        this.idempotencyService = idempotencyService;
        this.validator = validator;
        this.dtoMapper = dtoMapper;
    }

    @SuppressWarnings("java:S2139")
    public boolean handleRollback(RollbackValidationRuleCommand command) {
        String commandId = command.getId();

        try {
            logger.info("Processing RollbackValidationRuleCommand: commandId={}", commandId);

            if (idempotencyService.isProcessed(commandId)) {
                logger.info("Rollback command already processed: commandId={}", commandId);
                return true;
            }

            validateCommand(command);

            RollbackValidationRuleCommandPayload payload = command.getPayload();
            if (payload == null) {
                logger.error("Rollback command payload is null: commandId={}", commandId);
                publishRollbackErrorEvent(commandId, "INVALID_PAYLOAD", "Command payload is missing");
                return false;
            }

            String campaignId = payload.getCampaignId();
            String validationRuleId = payload.getValidationRuleId();
            boolean rollbackAll = payload.getRollbackAll();

            logger.info("Rollback request: campaignId={}, validationRuleId={}, rollbackAll={}",
                    campaignId, validationRuleId, rollbackAll);

            boolean success = executeRollback(campaignId, validationRuleId, rollbackAll);

            if (!success) {
                String errorCode = "ROLLBACK_FAILED";
                String errorMessage = "Failed to rollback validation rule binding";
                publishRollbackErrorEvent(commandId, errorCode, errorMessage);
                logger.error("Failed to process RollbackValidationRuleCommand: commandId={}", commandId);
                throw ExceptionFactory.createValidationException(errorCode, errorMessage);
            }

            idempotencyService.markAsProcessed(commandId, "Rollback completed successfully");
            publishRollbackSuccessEvent(commandId, campaignId, validationRuleId);

            logger.info("Successfully processed RollbackValidationRuleCommand: commandId={}", commandId);
            return true;

        } catch (BusinessException e) {
            logger.error("Validation failed: commandId={}, error={}", commandId, e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error: commandId={}", commandId, e);
            publishRollbackErrorEvent(commandId, "PROCESSING_ERROR", "Unexpected error: " + e.getMessage());
            return false;
        }
    }

    private void validateCommand(RollbackValidationRuleCommand command) {
        if (command == null || command.getPayload() == null) {
            logger.error("Received null command or null payload");
            throw ExceptionFactory.createValidationException("INVALID_COMMAND", "Command or payload is null");
        }

        RollbackValidationRuleCommandDTO dto = dtoMapper.toDTO(command);
        if (dto == null) {
            logger.error("Failed to convert command to DTO: commandId={}", command.getId());
            throw ExceptionFactory.createValidationException("INVALID_COMMAND", "Failed to convert command to DTO");
        }

        Set<ConstraintViolation<RollbackValidationRuleCommandDTO>> violations = validator.validate(dto);

        if (!violations.isEmpty()) {
            List<ErrorDetail> errorDetails = violations.stream()
                    .map(violation -> ErrorDetail.of(
                            violation.getPropertyPath().toString(),
                            violation.getMessage(),
                            String.format("Invalid value: %s", violation.getInvalidValue()),
                            violation.getInvalidValue()
                    ))
                    .toList();

            String errorMessage = String.format("Validation failed with %d error(s)", violations.size());

            logger.error("RollbackValidationRuleCommand validation failed: commandId={}, errors={}",
                    command.getId(), errorDetails);

            throw ExceptionFactory.createValidationException(
                    "METHOD_ARGUMENT_NOT_VALID",
                    errorMessage,
                    errorDetails.toArray(new ErrorDetail[0])
            );
        }

        logger.debug("RollbackValidationRuleCommand validation passed: commandId={}", command.getId());
    }

    private boolean executeRollback(String campaignId, String validationRuleId, boolean rollbackAll) {
        try {
            // Find bindings by target
            List<RuleBinding> bindings = ruleBindingPort.findByTarget("campaign", campaignId);

            if (bindings.isEmpty()) {
                logger.warn("No bindings found for campaign: {}", campaignId);
                return true;
            }

            // Filter if specific binding ID
            if (!rollbackAll && validationRuleId != null) {
                bindings = bindings.stream()
                        .filter(b -> validationRuleId.equals(b.getId()))
                        .toList();

                if (bindings.isEmpty()) {
                    logger.warn("No binding found with ID: {}", validationRuleId);
                    return true;
                }
            }

            logger.info("Found {} binding(s) to rollback for campaign: {}", bindings.size(), campaignId);

            // Rollback each binding
            for (RuleBinding binding : bindings) {
                rollbackBinding(binding);
            }

            return true;

        } catch (Exception e) {
            logger.error("Error executing rollback for campaign: {}", campaignId, e);
            return false;
        }
    }

    private void rollbackBinding(RuleBinding binding) {
        try {
            logger.info("Rolling back binding: bindingId={}, ruleId={}", binding.getId(), binding.getRuleId());

            // Deactivate binding
            ruleBindingPort.deactivate(binding.getId(), "system");

            logger.info("Rolled back binding: bindingId={}", binding.getId());

        } catch (Exception e) {
            throw new ValidationException("Failed to rollback binding: " + binding.getId(), e);
        }
    }

    private void publishRollbackSuccessEvent(String commandId, String campaignId, String validationRuleId) {
        try {
            eventPublisher.publishRollbackSuccessEvent(commandId, campaignId, validationRuleId);
        } catch (Exception e) {
            logger.error("Failed to publish rollback success event: commandId={}", commandId, e);
        }
    }

    private void publishRollbackErrorEvent(String commandId, String errorCode, String errorMessage) {
        try {
            eventPublisher.publishRollbackErrorEvent(commandId, errorCode, errorMessage);
        } catch (Exception e) {
            logger.error("Failed to publish rollback error event: commandId={}", commandId, e);
        }
    }

    public void handleDeadLetterCommand(RollbackValidationRuleCommand command) {
        logger.error("Processing dead letter RollbackValidationRuleCommand: commandId={}", command.getId());

        publishRollbackErrorEvent(
                command.getId(),
                "DLQ_PROCESSING",
                "Command moved to dead letter queue"
        );
    }
}
