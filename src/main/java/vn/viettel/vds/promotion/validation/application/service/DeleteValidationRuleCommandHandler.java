package vn.viettel.vds.promotion.validation.application.service;

import com.promix.platform.core.exception.BusinessRuleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.viettel.vds.promotion.validation.application.port.out.RuleBindingPersistencePort;
import vn.viettel.vds.promotion.validation.application.port.out.ValidationEnginePort;
import vn.viettel.vds.promotion.validation.application.port.out.ValidationRuleRepositoryPort;
import vn.viettel.vds.promotion.validation.command.DeleteValidationRuleCommand;
import vn.viettel.vds.promotion.validation.command.DeleteValidationRuleCommand.DeleteValidationRuleCommandPayload;
import vn.viettel.vds.promotion.validation.domain.exception.BindingDeactivationException;
import vn.viettel.vds.promotion.validation.domain.exception.InvalidCommandDataException;
import vn.viettel.vds.promotion.validation.domain.model.RuleBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * Service to handle DeleteValidationRuleCommand for soft deleting rule bindings.
 * <p>
 * Refactored to use unified RuleBinding model.
 */
@ConditionalOnProperty(prefix = "promix.messaging", name = "enabled", havingValue = "true")
@Service
@Transactional
public class DeleteValidationRuleCommandHandler {

    private static final Logger logger = LoggerFactory.getLogger(DeleteValidationRuleCommandHandler.class);

    private final RuleBindingPersistencePort ruleBindingPort;
    private final ValidationRuleRepositoryPort validationRulePort;
    @SuppressWarnings("unused")
    private final ValidationEnginePort validationEnginePort;
    private final SettingValidationRuleEventPublisher eventPublisher;
    private final IdempotencyService idempotencyService;

    public DeleteValidationRuleCommandHandler(
            RuleBindingPersistencePort ruleBindingPort,
            ValidationRuleRepositoryPort validationRulePort,
            ValidationEnginePort validationEnginePort,
            SettingValidationRuleEventPublisher eventPublisher,
            IdempotencyService idempotencyService) {
        this.ruleBindingPort = ruleBindingPort;
        this.validationRulePort = validationRulePort;
        this.validationEnginePort = validationEnginePort;
        this.eventPublisher = eventPublisher;
        this.idempotencyService = idempotencyService;
    }

    @SuppressWarnings("java:S2139")
    public boolean handleCommand(DeleteValidationRuleCommand command) {
        String commandId = command.getId();

        try {
            logger.info("Processing DeleteValidationRuleCommand: commandId={}", commandId);

            if (idempotencyService.isProcessed(commandId)) {
                logger.info("Delete command already processed: commandId={}", commandId);
                return true;
            }

            validateCommand(command);

            DeleteValidationRuleCommandPayload payload = command.getPayload();
            if (payload == null) {
                logger.error("Delete command payload is null: commandId={}", commandId);
                publishDeleteErrorEvent(commandId, null, "INVALID_PAYLOAD", "Command payload is missing");
                return false;
            }

            String campaignId = payload.getCampaignId();
            String validationRuleId = payload.getValidationRuleId();
            boolean deleteAll = Boolean.TRUE.equals(payload.getDeleteAll());

            logger.info("Delete request: campaignId={}, validationRuleId={}, deleteAll={}",
                    campaignId, validationRuleId, deleteAll);

            List<RuleBinding> deletedBindings = executeDelete(campaignId, validationRuleId, deleteAll);

            if (deletedBindings == null) {
                String errorCode = "DELETE_FAILED";
                String errorMessage = "Failed to delete validation rule binding";
                publishDeleteErrorEvent(commandId, campaignId, errorCode, errorMessage);
                logger.error("Failed to process DeleteValidationRuleCommand: commandId={}", commandId);
                throw new InvalidCommandDataException(errorCode, errorMessage);
            }

            idempotencyService.markAsProcessed(commandId, "Delete completed successfully");
            publishDeleteSuccessEvent(commandId, campaignId, deletedBindings);

            logger.info("Successfully processed DeleteValidationRuleCommand: commandId={}", commandId);
            return true;

        } catch (BusinessRuleException e) {
            logger.error("Validation failed: commandId={}, error={}", commandId, e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error: commandId={}", commandId, e);
            String campaignId = command.getPayload() != null ? command.getPayload().getCampaignId() : null;
            publishDeleteErrorEvent(commandId, campaignId, "PROCESSING_ERROR", "Unexpected error: " + e.getMessage());
            return false;
        }
    }

    private void validateCommand(DeleteValidationRuleCommand command) {
        if (command == null || command.getPayload() == null) {
            logger.error("Received null command or null payload");
            throw new InvalidCommandDataException("INVALID_COMMAND", "Command or payload is null");
        }

        if (command.getPayload().getCampaignId() == null || command.getPayload().getCampaignId().isBlank()) {
            logger.error("DeleteValidationRuleCommand validation failed: campaignId is required");
            throw new InvalidCommandDataException("INVALID_CAMPAIGN_ID", "campaignId is required");
        }

        logger.debug("DeleteValidationRuleCommand validation passed: commandId={}", command.getId());
    }

    private List<RuleBinding> executeDelete(String campaignId, String validationRuleId, boolean deleteAll) {
        try {
            // Find bindings by object (campaign)
            List<RuleBinding> bindings = ruleBindingPort.findByObject("campaign", campaignId);

            if (bindings.isEmpty()) {
                logger.warn("No bindings found for campaign: campaignId={}", campaignId);
                return new ArrayList<>();
            }

            // Filter if specific binding ID provided
            if (!deleteAll && validationRuleId != null) {
                bindings = bindings.stream()
                        .filter(b -> validationRuleId.equals(b.getId()))
                        .toList();

                if (bindings.isEmpty()) {
                    logger.warn("No binding found with ID: {}", validationRuleId);
                    return new ArrayList<>();
                }
            }

            logger.info("Found {} binding(s) to delete for campaign: {}", bindings.size(), campaignId);

            // Soft delete each binding
            for (RuleBinding binding : bindings) {
                deleteBinding(binding);
            }

            return bindings;

        } catch (Exception e) {
            logger.error("Error executing delete for campaign: {}", campaignId, e);
            return null;
        }
    }

    private void deleteBinding(RuleBinding binding) {
        try {
            logger.info("Deleting binding: bindingId={}, ruleId={}, objectId={}",
                    binding.getId(), binding.getRuleId(), binding.getObjectId());

            // Soft delete: deactivate
            ruleBindingPort.deactivate(binding.getId(), "system");

            logger.info("Marked binding as inactive: bindingId={}", binding.getId());

            // Undeploy from validation-engine
            undeployRuleFromEngine(binding);

            logger.info("Successfully deleted binding: bindingId={}", binding.getId());

        } catch (Exception e) {
            throw new BindingDeactivationException(binding.getId(), e);
        }
    }

    private void undeployRuleFromEngine(RuleBinding binding) {
        try {
            String ruleId = binding.getRuleId();

            if (ruleId != null && validationRulePort.existsById(ruleId)) {
                logger.info("Rule removal requested: ruleId={}, bindingId={}", ruleId, binding.getId());
            } else {
                logger.warn("Rule not found for undeployment: ruleId={}", ruleId);
            }
        } catch (Exception e) {
            logger.error("Error undeploying rule: bindingId={}", binding.getId(), e);
        }
    }

    private void publishDeleteSuccessEvent(String commandId, String campaignId, List<RuleBinding> deletedBindings) {
        try {
            if (deletedBindings == null || deletedBindings.isEmpty()) {
                // Nothing matched — still notify the saga so it can progress; data plane no-ops.
                eventPublisher.publishDeleteSuccessEvent(commandId, campaignId, (RuleBinding) null);
                return;
            }
            for (RuleBinding binding : deletedBindings) {
                eventPublisher.publishDeleteSuccessEvent(commandId, campaignId, binding);
            }
        } catch (Exception e) {
            logger.error("Failed to publish delete success event: commandId={}", commandId, e);
        }
    }

    private void publishDeleteErrorEvent(String commandId, String campaignId, String errorCode, String errorMessage) {
        try {
            eventPublisher.publishDeleteErrorEvent(commandId, campaignId, errorCode, errorMessage);
        } catch (Exception e) {
            logger.error("Failed to publish delete error event: commandId={}", commandId, e);
        }
    }

    public void handleDeadLetterCommand(DeleteValidationRuleCommand command) {
        String commandId = command.getId();
        logger.error("Processing dead letter DeleteValidationRuleCommand: commandId={}", commandId);

        // If already successfully processed, skip failure event to prevent duplicate SUCCESS+FAILURE
        if (idempotencyService.isProcessed(commandId)) {
            logger.info("Delete command already processed successfully, skipping DLQ failure event: commandId={}", commandId);
            return;
        }

        publishDeleteErrorEvent(
                commandId,
                command.getPayload() != null ? command.getPayload().getCampaignId() : null,
                "DLQ_PROCESSING",
                "Command moved to dead letter queue"
        );
    }
}
