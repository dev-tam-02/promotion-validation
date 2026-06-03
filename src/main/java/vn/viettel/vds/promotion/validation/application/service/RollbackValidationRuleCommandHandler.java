package vn.viettel.vds.promotion.validation.application.service;

import com.promix.platform.core.exception.BusinessRuleException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.viettel.vds.promotion.validation.adapter.in.messaging.dto.RollbackValidationRuleCommandDTO;
import vn.viettel.vds.promotion.validation.adapter.in.messaging.mapper.RollbackValidationRuleCommandDTOMapper;
import vn.viettel.vds.promotion.validation.application.port.out.RuleBindingPersistencePort;
import vn.viettel.vds.promotion.validation.application.port.out.RulePersistencePort;
import vn.viettel.vds.promotion.validation.command.RollbackValidationRuleCommand;
import vn.viettel.vds.promotion.validation.command.RollbackValidationRuleCommand.RollbackValidationRuleCommandPayload;
import vn.viettel.vds.promotion.validation.domain.exception.InvalidCommandDataException;
import vn.viettel.vds.promotion.validation.domain.model.RuleBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Service to handle RollbackValidationRuleCommand for saga compensation.
 * <p>
 * Refactored to use unified RuleBinding model.
 */
@ConditionalOnProperty(prefix = "promix.messaging", name = "enabled", havingValue = "true")
@Service
@Transactional
public class RollbackValidationRuleCommandHandler {

    private static final Logger logger = LoggerFactory.getLogger(RollbackValidationRuleCommandHandler.class);
    // Canonical object types that can be bound to a campaign-level object.
    private static final List<String> CAMPAIGN_OBJECT_TYPES = List.of("CAMPAIGN", "DISCOUNT_COUPON", "CASHBACK");
    private final RuleBindingPersistencePort ruleBindingPort;
    private final RulePersistencePort rulePort;
    private final SettingValidationRuleEventPublisher eventPublisher;
    private final IdempotencyService idempotencyService;
    private final Validator validator;
    private final RollbackValidationRuleCommandDTOMapper dtoMapper;

    public RollbackValidationRuleCommandHandler(
            RuleBindingPersistencePort ruleBindingPort,
            RulePersistencePort rulePort,
            SettingValidationRuleEventPublisher eventPublisher,
            IdempotencyService idempotencyService,
            Validator validator,
            RollbackValidationRuleCommandDTOMapper dtoMapper) {
        this.ruleBindingPort = ruleBindingPort;
        this.rulePort = rulePort;
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
                throw new InvalidCommandDataException(errorCode, errorMessage);
            }

            idempotencyService.markAsProcessed(commandId, "Rollback completed successfully");
            publishRollbackSuccessEvent(commandId, campaignId, validationRuleId);

            logger.info("Successfully processed RollbackValidationRuleCommand: commandId={}", commandId);
            return true;

        } catch (BusinessRuleException e) {
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
            throw new InvalidCommandDataException("INVALID_COMMAND", "Command or payload is null");
        }

        RollbackValidationRuleCommandDTO dto = dtoMapper.toDTO(command);
        if (dto == null) {
            logger.error("Failed to convert command to DTO: commandId={}", command.getId());
            throw new InvalidCommandDataException("INVALID_COMMAND", "Failed to convert command to DTO");
        }

        Set<ConstraintViolation<RollbackValidationRuleCommandDTO>> violations = validator.validate(dto);

        if (!violations.isEmpty()) {
            String errorDetails = violations.stream()
                    .map(violation -> String.format("%s: %s (value: %s)",
                            violation.getPropertyPath().toString(),
                            violation.getMessage(),
                            violation.getInvalidValue()))
                    .collect(java.util.stream.Collectors.joining("; "));

            String errorMessage = String.format("Validation failed with %d error(s): %s", violations.size(), errorDetails);

            logger.error("RollbackValidationRuleCommand validation failed: commandId={}, errors={}",
                    command.getId(), errorDetails);

            throw new InvalidCommandDataException("METHOD_ARGUMENT_NOT_VALID", errorMessage);
        }

        logger.debug("RollbackValidationRuleCommand validation passed: commandId={}", command.getId());
    }

    private boolean executeRollback(String campaignId, String validationRuleId, boolean rollbackAll) {
        try {
            if (rollbackAll || validationRuleId == null) {
                // Delete all bindings for this campaign across all supported object types
                // (case-insensitive match covers any mixed-case rows from legacy writes)
                int total = 0;
                for (String objectType : CAMPAIGN_OBJECT_TYPES) {
                    total += ruleBindingPort.deleteByObjectIgnoreCase(objectType, campaignId);
                }
                logger.info("Deleted {} rule_bindings for campaign: {}", total, campaignId);
            } else {
                // Find bindings across all supported object types (case-insensitive)
                List<RuleBinding> bindings = new ArrayList<>();
                for (String objectType : CAMPAIGN_OBJECT_TYPES) {
                    bindings.addAll(ruleBindingPort.findByObjectIgnoreCase(objectType, campaignId));
                }

                // Filter to target binding by ruleId only (F4: drop ambiguous id OR clause)
                List<RuleBinding> toDelete = bindings.stream()
                        .filter(b -> validationRuleId.equals(b.getRuleId()))
                        .toList();

                if (toDelete.isEmpty()) {
                    logger.warn("No binding found matching validationRuleId={} for campaign={}", validationRuleId, campaignId);
                } else {
                    logger.info("Found {} binding(s) to delete for campaign: {}", toDelete.size(), campaignId);
                    for (RuleBinding binding : toDelete) {
                        deleteBinding(binding);
                    }
                }
            }

            // Compensation must also remove the saga-auto-generated timeframe-gate rule
            // (code = CAMPAIGN_<id>, Path B in SettingValidationRuleCommandHandler). Deleting
            // the binding alone leaves that rule's row behind, keeping its UNIQUE code
            // occupied — so a later re-create (editing the campaign out of ERROR) fails with
            // "Duplicate entry 'CAMPAIGN_...' for key 'idx_validation_rules_code'". User-picked
            // shared rules (Path A) keep their own code and are never matched here.
            deleteAutoGeneratedCampaignRule(campaignId);

            return true;

        } catch (Exception e) {
            logger.error("Error executing rollback for campaign: {}", campaignId, e);
            return false;
        }
    }

    /**
     * Hard-delete the campaign's auto-generated validation rule (and its nodes), if present.
     *
     * <p>The code is derived exactly as {@code SettingValidationRuleCommandHandler} Path B
     * ({@code "CAMPAIGN_" + objectId-without-dashes, capped at 20 chars}). That synthetic code
     * is itself the ownership proof — it embeds the campaignId and a user-picked shared rule
     * (Path A) can never carry it — so matching by code is sufficient. (We do NOT gate on
     * {@code rule.getCampaignId()}: that column is not persisted on validation_rules and reads
     * back null.)
     */
    private void deleteAutoGeneratedCampaignRule(String campaignId) {
        if (campaignId == null || campaignId.isBlank()) {
            return;
        }
        String normalized = campaignId.replace("-", "");
        String code = "CAMPAIGN_" + normalized.substring(0, Math.min(normalized.length(), 20));

        rulePort.findByCode(code).ifPresent(rule -> {
            rulePort.deleteNodesByRuleId(rule.getId());
            rulePort.deleteById(rule.getId());
            logger.info("Deleted auto-generated campaign rule on rollback: code={}, ruleId={}, campaignId={}",
                    code, rule.getId(), campaignId);
        });
    }

    private void deleteBinding(RuleBinding binding) {
        logger.info("Deleting rule_binding: bindingId={}, ruleId={}", binding.getId(), binding.getRuleId());
        ruleBindingPort.deleteById(binding.getId());
        logger.info("Deleted rule_binding: bindingId={}", binding.getId());
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
        String commandId = command.getId();
        logger.error("Processing dead letter RollbackValidationRuleCommand: commandId={}", commandId);

        // If already successfully processed, skip failure event to prevent duplicate SUCCESS+FAILURE
        if (idempotencyService.isProcessed(commandId)) {
            logger.info("Rollback command already processed successfully, skipping DLQ failure event: commandId={}", commandId);
            return;
        }

        publishRollbackErrorEvent(
                commandId,
                "DLQ_PROCESSING",
                "Command moved to dead letter queue"
        );
    }
}
