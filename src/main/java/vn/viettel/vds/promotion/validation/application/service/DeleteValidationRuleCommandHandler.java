package vn.viettel.vds.promotion.validation.application.service;

import com.promix.platform.core.exception.BusinessException;
import com.promix.platform.core.exception.factory.ExceptionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.viettel.vds.promotion.validation.application.port.out.RuleBindingPersistencePort;
import vn.viettel.vds.promotion.validation.application.port.out.ValidationEnginePort;
import vn.viettel.vds.promotion.validation.application.port.out.ValidationRuleRepositoryPort;
import vn.viettel.vds.promotion.validation.domain.model.RuleBinding;
import vn.viettel.vds.promotion.validation.command.DeleteValidationRuleCommand;
import vn.viettel.vds.promotion.validation.command.DeleteValidationRuleCommand.DeleteValidationRuleCommandPayload;
import vn.viettel.vds.promotion.validation.domain.exception.ValidationException;

import java.time.Instant;
import java.util.List;

/**
 * Service to handle DeleteValidationRuleCommand for soft deleting rule bindings.
 * <p>
 * Refactored to use unified RuleBinding model.
 */
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

            boolean success = executeDelete(campaignId, validationRuleId, deleteAll);

            if (!success) {
                String errorCode = "DELETE_FAILED";
                String errorMessage = "Failed to delete validation rule binding";
                publishDeleteErrorEvent(commandId, campaignId, errorCode, errorMessage);
                logger.error("Failed to process DeleteValidationRuleCommand: commandId={}", commandId);
                throw ExceptionFactory.createValidationException(errorCode, errorMessage);
            }

            idempotencyService.markAsProcessed(commandId, "Delete completed successfully");
            publishDeleteSuccessEvent(commandId, campaignId, validationRuleId);

            logger.info("Successfully processed DeleteValidationRuleCommand: commandId={}", commandId);
            return true;

        } catch (BusinessException e) {
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
            throw ExceptionFactory.createValidationException("INVALID_COMMAND", "Command or payload is null");
        }

        if (command.getPayload().getCampaignId() == null || command.getPayload().getCampaignId().isBlank()) {
            logger.error("DeleteValidationRuleCommand validation failed: campaignId is required");
            throw ExceptionFactory.createValidationException("INVALID_CAMPAIGN_ID", "campaignId is required");
        }

        logger.debug("DeleteValidationRuleCommand validation passed: commandId={}", command.getId());
    }

    private boolean executeDelete(String campaignId, String validationRuleId, boolean deleteAll) {
        try {
            // Find bindings by object (campaign)
            List<RuleBinding> bindings = ruleBindingPort.findByObject("campaign", campaignId);

            if (bindings.isEmpty()) {
                logger.warn("No bindings found for campaign: campaignId={}", campaignId);
                return true;
            }

            // Filter if specific binding ID provided
            if (!deleteAll && validationRuleId != null) {
                bindings = bindings.stream()
                        .filter(b -> validationRuleId.equals(b.getId()))
                        .toList();

                if (bindings.isEmpty()) {
                    logger.warn("No binding found with ID: {}", validationRuleId);
                    return true;
                }
            }

            logger.info("Found {} binding(s) to delete for campaign: {}", bindings.size(), campaignId);

            // Soft delete each binding
            for (RuleBinding binding : bindings) {
                deleteBinding(binding);
            }

            return true;

        } catch (Exception e) {
            logger.error("Error executing delete for campaign: {}", campaignId, e);
            return false;
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
            throw new ValidationException("Failed to delete binding: " + binding.getId(), e);
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

    private void publishDeleteSuccessEvent(String commandId, String campaignId, String validationRuleId) {
        try {
            eventPublisher.publishDeleteSuccessEvent(commandId, campaignId, validationRuleId);
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
        logger.error("Processing dead letter DeleteValidationRuleCommand: commandId={}", command.getId());

        publishDeleteErrorEvent(
                command.getId(),
                command.getPayload() != null ? command.getPayload().getCampaignId() : null,
                "DLQ_PROCESSING",
                "Command moved to dead letter queue"
        );
    }
}
