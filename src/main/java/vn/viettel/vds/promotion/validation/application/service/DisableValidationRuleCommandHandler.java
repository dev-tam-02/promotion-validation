package vn.viettel.vds.promotion.validation.application.service;

import com.promix.platform.core.exception.BusinessRuleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.viettel.vds.promotion.validation.application.port.out.RuleBindingPersistencePort;
import vn.viettel.vds.promotion.validation.command.DisableValidationRuleCommand;
import vn.viettel.vds.promotion.validation.command.DisableValidationRuleCommand.DisableValidationRuleCommandPayload;
import vn.viettel.vds.promotion.validation.domain.exception.InvalidCommandDataException;
import vn.viettel.vds.promotion.validation.domain.model.RuleBinding;

import java.util.List;

/**
 * Service to handle DisableValidationRuleCommand for disabling rule bindings.
 * <p>
 * Refactored to use unified RuleBinding model.
 */
@ConditionalOnProperty(prefix = "promix.messaging", name = "enabled", havingValue = "true")
@Service
@Transactional
public class DisableValidationRuleCommandHandler {

    private static final Logger logger = LoggerFactory.getLogger(DisableValidationRuleCommandHandler.class);

    private final RuleBindingPersistencePort ruleBindingPort;
    private final SettingValidationRuleEventPublisher eventPublisher;
    private final IdempotencyService idempotencyService;

    public DisableValidationRuleCommandHandler(
            RuleBindingPersistencePort ruleBindingPort,
            SettingValidationRuleEventPublisher eventPublisher,
            IdempotencyService idempotencyService) {
        this.ruleBindingPort = ruleBindingPort;
        this.eventPublisher = eventPublisher;
        this.idempotencyService = idempotencyService;
    }

    @SuppressWarnings("java:S2139")
    public boolean handleCommand(DisableValidationRuleCommand command) {
        String commandId = command.getId();

        try {
            logger.info("Processing DisableValidationRuleCommand: commandId={}", commandId);

            if (idempotencyService.isProcessed(commandId)) {
                logger.info("Disable command already processed: commandId={}", commandId);
                return true;
            }

            validateCommand(command);

            DisableValidationRuleCommandPayload payload = command.getPayload();
            if (payload == null) {
                logger.error("Disable command payload is null: commandId={}", commandId);
                publishDisableErrorEvent(commandId, null, "INVALID_PAYLOAD", "Command payload is missing");
                return false;
            }

            String campaignId = payload.getCampaignId();
            String validationRuleId = payload.getValidationRuleId();

            logger.info("Disable request: campaignId={}, validationRuleId={}", campaignId, validationRuleId);

            RuleBinding disabledBinding = executeDisable(campaignId, validationRuleId);

            if (disabledBinding == null) {
                String errorCode = "DISABLE_FAILED";
                String errorMessage = "Failed to disable validation rule binding";
                publishDisableErrorEvent(commandId, campaignId, errorCode, errorMessage);
                logger.error("Failed to process DisableValidationRuleCommand: commandId={}", commandId);
                throw new InvalidCommandDataException(errorCode, errorMessage);
            }

            idempotencyService.markAsProcessed(commandId, "Disable completed successfully");
            publishDisableSuccessEvent(commandId, campaignId, disabledBinding);

            logger.info("Successfully processed DisableValidationRuleCommand: commandId={}", commandId);
            return true;

        } catch (BusinessRuleException e) {
            logger.error("Validation failed: commandId={}, error={}", commandId, e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error: commandId={}", commandId, e);
            String campaignId = command.getPayload() != null ? command.getPayload().getCampaignId() : null;
            publishDisableErrorEvent(commandId, campaignId, "PROCESSING_ERROR", "Unexpected error: " + e.getMessage());
            return false;
        }
    }

    private void validateCommand(DisableValidationRuleCommand command) {
        if (command == null || command.getPayload() == null) {
            logger.error("Received null command or null payload");
            throw new InvalidCommandDataException("INVALID_COMMAND", "Command or payload is null");
        }

        DisableValidationRuleCommandPayload payload = command.getPayload();
        if (payload.getCampaignId() == null || payload.getCampaignId().isBlank()) {
            throw new InvalidCommandDataException("INVALID_CAMPAIGN_ID", "campaignId is required");
        }

        if (payload.getValidationRuleId() == null || payload.getValidationRuleId().isBlank()) {
            throw new InvalidCommandDataException("INVALID_VALIDATION_RULE_ID", "validationRuleId is required");
        }

        logger.debug("DisableValidationRuleCommand validation passed: commandId={}", command.getId());
    }

    private RuleBinding executeDisable(String campaignId, String validationRuleId) {
        try {
            // Find binding by ID first
            RuleBinding binding = ruleBindingPort.findById(validationRuleId).orElse(null);

            // If not found by ID, search by object
            if (binding == null) {
                List<RuleBinding> bindings = ruleBindingPort.findByObject("campaign", campaignId);
                if (bindings.isEmpty()) {
                    logger.warn("No binding found for campaign: {}", campaignId);
                    return null;
                }
                binding = bindings.get(0);
            }

            logger.info("Found binding to disable: bindingId={}, ruleId={}, currentActive={}",
                    binding.getId(), binding.getRuleId(), binding.getActive());

            // Disable binding
            ruleBindingPort.deactivate(binding.getId(), "system");

            logger.info("Disabled binding: bindingId={}", binding.getId());

            return binding;

        } catch (Exception e) {
            logger.error("Error executing disable: campaignId={}, validationRuleId={}", campaignId, validationRuleId, e);
            return null;
        }
    }

    private void publishDisableSuccessEvent(String commandId, String campaignId, RuleBinding binding) {
        try {
            eventPublisher.publishDisableSuccessEvent(commandId, campaignId, binding);
        } catch (Exception e) {
            logger.error("Failed to publish disable success event: commandId={}", commandId, e);
        }
    }

    private void publishDisableErrorEvent(String commandId, String campaignId, String errorCode, String errorMessage) {
        try {
            eventPublisher.publishDisableErrorEvent(commandId, campaignId, errorCode, errorMessage);
        } catch (Exception e) {
            logger.error("Failed to publish disable error event: commandId={}", commandId, e);
        }
    }

    public void handleDeadLetterCommand(DisableValidationRuleCommand command) {
        String commandId = command.getId();
        logger.error("Processing dead letter DisableValidationRuleCommand: commandId={}", commandId);

        // If already successfully processed, skip failure event to prevent duplicate SUCCESS+FAILURE
        if (idempotencyService.isProcessed(commandId)) {
            logger.info("Disable command already processed successfully, skipping DLQ failure event: commandId={}", commandId);
            return;
        }

        publishDisableErrorEvent(
                commandId,
                command.getPayload() != null ? command.getPayload().getCampaignId() : null,
                "DLQ_PROCESSING",
                "Command moved to dead letter queue"
        );
    }
}
