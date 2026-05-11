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
                // Empty / no-binding-found is an idempotent success state (the saga's
                // intent — "ensure no active binding for this campaign" — is already true).
                logger.info("No binding to disable for campaignId={} - idempotent success", campaignId);
                idempotencyService.markAsProcessed(commandId, "No binding to disable - idempotent success");
                publishDisableSuccessEvent(commandId, campaignId, null);
                return true;
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

        logger.debug("DisableValidationRuleCommand validation passed: commandId={}", command.getId());
    }

    /**
     * Disable rule bindings for the given campaign. When {@code validationRuleId}
     * is provided, only that specific binding is disabled. Otherwise ALL bindings
     * registered under {@code object_type='campaign' AND object_id=campaignId}
     * are deactivated — the disable saga calls this without a ruleId because the
     * campaign service does not track which validation rule(s) a campaign owns.
     * Returns the first deactivated binding so the existing success event format
     * can still be published, or {@code null} when no binding was found / all
     * deactivations failed.
     */
    private RuleBinding executeDisable(String campaignId, String validationRuleId) {
        try {
            if (validationRuleId != null && !validationRuleId.isBlank()) {
                RuleBinding binding = ruleBindingPort.findById(validationRuleId).orElse(null);
                if (binding != null) {
                    logger.info("Found binding to disable by id: bindingId={}, ruleId={}, currentActive={}",
                            binding.getId(), binding.getRuleId(), binding.getActive());
                    ruleBindingPort.deactivate(binding.getId(), "system");
                    logger.info("Disabled binding: bindingId={}", binding.getId());
                    return binding;
                }
                logger.info("Binding not found by id, falling back to disable-all-by-campaign: campaignId={}", campaignId);
            }

            // Query by objectId only — saga doesn't know the binding's object_type
            // (campaign may be bound as CAMPAIGN / DISCOUNT_COUPON / VOUCHER).
            List<RuleBinding> bindings = ruleBindingPort.findByObjectId(campaignId);
            if (bindings.isEmpty()) {
                logger.info("No binding found for campaignId={} - idempotent no-op", campaignId);
                return null;
            }

            RuleBinding firstDisabled = null;
            for (RuleBinding binding : bindings) {
                if (deactivateBindingSafely(binding) && firstDisabled == null) {
                    firstDisabled = binding;
                }
            }
            return firstDisabled;

        } catch (Exception e) {
            logger.error("Error executing disable: campaignId={}, validationRuleId={}", campaignId, validationRuleId, e);
            return null;
        }
    }

    private boolean deactivateBindingSafely(RuleBinding binding) {
        try {
            ruleBindingPort.deactivate(binding.getId(), "system");
            logger.info("Disabled binding: bindingId={}, ruleId={}", binding.getId(), binding.getRuleId());
            return true;
        } catch (Exception inner) {
            logger.error("Failed to disable binding bindingId={}, ruleId={}",
                    binding.getId(), binding.getRuleId(), inner);
            return false;
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
