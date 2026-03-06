package vn.viettel.vds.promotion.validation.application.service;

import com.promix.platform.core.exception.BusinessRuleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.viettel.vds.promotion.validation.domain.exception.InvalidCommandDataException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.viettel.vds.promotion.validation.application.port.out.RuleBindingPersistencePort;
import vn.viettel.vds.promotion.validation.application.port.out.ValidationRuleRepositoryPort;
import vn.viettel.vds.promotion.validation.domain.model.RuleBinding;
import vn.viettel.vds.promotion.validation.command.EnableValidationRuleCommand;
import vn.viettel.vds.promotion.validation.command.EnableValidationRuleCommand.EnableValidationRuleCommandPayload;

import java.time.Instant;
import java.util.List;

/**
 * Service to handle EnableValidationRuleCommand for enabling rule bindings.
 * <p>
 * Refactored to use unified RuleBinding model.
 */
@ConditionalOnProperty(prefix = "promix.messaging", name = "enabled", havingValue = "true")
@Service
@Transactional
public class EnableValidationRuleCommandHandler {

    private static final Logger logger = LoggerFactory.getLogger(EnableValidationRuleCommandHandler.class);

    private final RuleBindingPersistencePort ruleBindingPort;
    private final ValidationRuleRepositoryPort validationRulePort;
    private final SettingValidationRuleEventPublisher eventPublisher;
    private final IdempotencyService idempotencyService;
    private final RulePublishingService rulePublishingService;

    public EnableValidationRuleCommandHandler(
            RuleBindingPersistencePort ruleBindingPort,
            ValidationRuleRepositoryPort validationRulePort,
            SettingValidationRuleEventPublisher eventPublisher,
            IdempotencyService idempotencyService,
            RulePublishingService rulePublishingService) {
        this.ruleBindingPort = ruleBindingPort;
        this.validationRulePort = validationRulePort;
        this.eventPublisher = eventPublisher;
        this.idempotencyService = idempotencyService;
        this.rulePublishingService = rulePublishingService;
    }

    @SuppressWarnings("java:S2139")
    public boolean handleCommand(EnableValidationRuleCommand command) {
        String commandId = command.getId();

        try {
            logger.info("Processing EnableValidationRuleCommand: commandId={}", commandId);

            if (idempotencyService.isProcessed(commandId)) {
                logger.info("Enable command already processed: commandId={}", commandId);
                return true;
            }

            validateCommand(command);

            EnableValidationRuleCommandPayload payload = command.getPayload();
            if (payload == null) {
                logger.error("Enable command payload is null: commandId={}", commandId);
                publishEnableErrorEvent(commandId, null, "INVALID_PAYLOAD", "Command payload is missing");
                return false;
            }

            String campaignId = payload.getCampaignId();
            String validationRuleId = payload.getValidationRuleId();

            logger.info("Enable request: campaignId={}, validationRuleId={}", campaignId, validationRuleId);

            boolean success = executeEnable(campaignId, validationRuleId);

            if (!success) {
                String errorCode = "ENABLE_FAILED";
                String errorMessage = "Failed to enable validation rule binding";
                publishEnableErrorEvent(commandId, campaignId, errorCode, errorMessage);
                logger.error("Failed to process EnableValidationRuleCommand: commandId={}", commandId);
                throw new InvalidCommandDataException(errorCode, errorMessage);
            }

            idempotencyService.markAsProcessed(commandId, "Enable completed successfully");
            publishEnableSuccessEvent(commandId, campaignId, validationRuleId);

            logger.info("Successfully processed EnableValidationRuleCommand: commandId={}", commandId);
            return true;

        } catch (BusinessRuleException e) {
            logger.error("Validation failed: commandId={}, error={}", commandId, e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error: commandId={}", commandId, e);
            String campaignId = command.getPayload() != null ? command.getPayload().getCampaignId() : null;
            publishEnableErrorEvent(commandId, campaignId, "PROCESSING_ERROR", "Unexpected error: " + e.getMessage());
            return false;
        }
    }

    private void validateCommand(EnableValidationRuleCommand command) {
        if (command == null || command.getPayload() == null) {
            logger.error("Received null command or null payload");
            throw new InvalidCommandDataException("INVALID_COMMAND", "Command or payload is null");
        }

        EnableValidationRuleCommandPayload payload = command.getPayload();
        if (payload.getCampaignId() == null || payload.getCampaignId().isBlank()) {
            throw new InvalidCommandDataException("INVALID_CAMPAIGN_ID", "campaignId is required");
        }

        if (payload.getValidationRuleId() == null || payload.getValidationRuleId().isBlank()) {
            throw new InvalidCommandDataException("INVALID_VALIDATION_RULE_ID", "validationRuleId is required");
        }

        logger.debug("EnableValidationRuleCommand validation passed: commandId={}", command.getId());
    }

    private boolean executeEnable(String campaignId, String validationRuleId) {
        try {
            // Find binding by ID first
            RuleBinding binding = ruleBindingPort.findById(validationRuleId).orElse(null);

            // If not found by ID, search by object
            if (binding == null) {
                List<RuleBinding> bindings = ruleBindingPort.findByObject("campaign", campaignId);
                if (bindings.isEmpty()) {
                    logger.warn("No binding found for campaign: {}", campaignId);
                    return false;
                }
                binding = bindings.get(0);
            }

            logger.info("Found binding to enable: bindingId={}, ruleId={}, currentActive={}",
                    binding.getId(), binding.getRuleId(), binding.getActive());

            // Enable binding
            RuleBinding updated = binding.toBuilder()
                    .active(true)
                    .updatedAt(Instant.now())
                    .updatedBy("system")
                    .build();
            ruleBindingPort.save(updated);

            logger.info("Enabled binding: bindingId={}", binding.getId());

            // Deploy rule if binding has ruleId
            if (binding.getRuleId() != null && !binding.getRuleId().isBlank()) {
                deployRuleToEngine(updated);
            }

            return true;

        } catch (Exception e) {
            logger.error("Error executing enable: campaignId={}, validationRuleId={}", campaignId, validationRuleId, e);
            return false;
        }
    }

    private void deployRuleToEngine(RuleBinding binding) {
        try {
            String ruleId = binding.getRuleId();

            logger.info("Deploying rule: ruleId={}, bindingId={}", ruleId, binding.getId());

            if (!validationRulePort.existsById(ruleId)) {
                logger.warn("Rule not found for deployment: ruleId={}", ruleId);
                return;
            }

            var publishResult = rulePublishingService.publishRule(ruleId, binding.getId(), null);

            if (publishResult.isSuccess()) {
                RuleBinding updated = binding.toBuilder()
                        .bundleHash(publishResult.getBundleHash())
                        .build();
                ruleBindingPort.save(updated);
                logger.info("Rule deployed: ruleId={}, bundleHash={}", ruleId, publishResult.getBundleHash());
            } else {
                logger.error("Failed to deploy rule: ruleId={}, error={}", ruleId, publishResult.getErrorMessage());
            }

        } catch (Exception e) {
            logger.error("Error deploying rule: bindingId={}", binding.getId(), e);
        }
    }

    private void publishEnableSuccessEvent(String commandId, String campaignId, String validationRuleId) {
        try {
            eventPublisher.publishEnableSuccessEvent(commandId, campaignId, validationRuleId);
        } catch (Exception e) {
            logger.error("Failed to publish enable success event: commandId={}", commandId, e);
        }
    }

    private void publishEnableErrorEvent(String commandId, String campaignId, String errorCode, String errorMessage) {
        try {
            eventPublisher.publishEnableErrorEvent(commandId, campaignId, errorCode, errorMessage);
        } catch (Exception e) {
            logger.error("Failed to publish enable error event: commandId={}", commandId, e);
        }
    }

    public void handleDeadLetterCommand(EnableValidationRuleCommand command) {
        logger.error("Processing dead letter EnableValidationRuleCommand: commandId={}", command.getId());

        publishEnableErrorEvent(
                command.getId(),
                command.getPayload() != null ? command.getPayload().getCampaignId() : null,
                "DLQ_PROCESSING",
                "Command moved to dead letter queue"
        );
    }
}
