package vn.viettel.vds.promotion.validation.application.service;

import com.promix.platform.core.exception.BusinessRuleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.viettel.vds.promotion.validation.application.port.out.RuleBindingPersistencePort;
import vn.viettel.vds.promotion.validation.application.port.out.ValidationRuleRepositoryPort;
import vn.viettel.vds.promotion.validation.command.EnableValidationRuleCommand;
import vn.viettel.vds.promotion.validation.command.EnableValidationRuleCommand.EnableValidationRuleCommandPayload;
import vn.viettel.vds.promotion.validation.domain.exception.InvalidCommandDataException;
import vn.viettel.vds.promotion.validation.domain.model.RuleBinding;

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

            RuleBinding enabledBinding = executeEnable(campaignId, validationRuleId);

            if (enabledBinding == null) {
                // issue #4 family: a campaign created WITHOUT validation criteria (PROM-972 gate)
                // has no RuleBinding. "Enable validation" is then vacuously satisfied — there is
                // nothing to enable — so treat the absent-binding case as an idempotent success
                // (mirrors the DISABLE handler) instead of failing. Otherwise such campaigns could
                // never be enabled: the enable saga waits for ValidationRuleEnabledEvent forever and
                // times out to ERROR. A null return when a binding DOES exist means the activation
                // itself failed (e.g. DB error) and must still surface as ENABLE_FAILED so the saga
                // does not falsely report the campaign ACTIVE.
                if (!bindingExists(campaignId, validationRuleId)) {
                    logger.info("No binding to enable for campaignId={} - idempotent success", campaignId);
                    idempotencyService.markAsProcessed(commandId, "No binding to enable - idempotent success");
                    publishEnableSuccessEvent(commandId, campaignId, null);
                    return true;
                }

                String errorCode = "ENABLE_FAILED";
                String errorMessage = "Failed to enable validation rule binding";
                publishEnableErrorEvent(commandId, campaignId, errorCode, errorMessage);
                logger.error("Failed to process EnableValidationRuleCommand: commandId={}", commandId);
                throw new InvalidCommandDataException(errorCode, errorMessage);
            }

            idempotencyService.markAsProcessed(commandId, "Enable completed successfully");
            publishEnableSuccessEvent(commandId, campaignId, enabledBinding);

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

        logger.debug("EnableValidationRuleCommand validation passed: commandId={}", command.getId());
    }

    /**
     * Enable rule bindings for the given campaign. When {@code validationRuleId}
     * is provided, only that specific binding is enabled. Otherwise ALL bindings
     * registered under {@code object_id=campaignId} are reactivated — the enable
     * saga calls this without a ruleId because the campaign service does not
     * track which validation rule(s) a campaign owns. We look up by objectId
     * only (no object_type filter) because bindings are persisted with the
     * concrete promotion type (e.g. {@code DISCOUNT_COUPON}), not the generic
     * {@code "campaign"} literal (aligned with the DISABLE handler).
     * Returns the first enabled binding so the existing success event format can
     * still be published, or {@code null} when no binding was found.
     */
    /**
     * Distinguish "campaign has no validation binding at all" (idempotent enable
     * success — issue #4 family) from "a binding exists but its activation failed"
     * (a real error). Re-queries by ruleId then by campaign objectId, matching the
     * lookup order of {@link #executeEnable}.
     */
    private boolean bindingExists(String campaignId, String validationRuleId) {
        if (validationRuleId != null && !validationRuleId.isBlank()
                && ruleBindingPort.findById(validationRuleId).isPresent()) {
            return true;
        }
        return !ruleBindingPort.findByObjectId(campaignId).isEmpty();
    }

    private RuleBinding executeEnable(String campaignId, String validationRuleId) {
        try {
            if (validationRuleId != null && !validationRuleId.isBlank()) {
                RuleBinding binding = ruleBindingPort.findById(validationRuleId).orElse(null);
                if (binding != null) {
                    return enableSingleBinding(binding);
                }
                logger.info("Binding not found by id, falling back to enable-all-by-campaign: campaignId={}", campaignId);
            }

            // Query by objectId only — saga does not know the binding's object_type
            // (DISCOUNT_COUPON / CASHBACK / etc.), and the legacy "campaign" literal never matches.
            List<RuleBinding> bindings = ruleBindingPort.findByObjectId(campaignId);
            if (bindings.isEmpty()) {
                logger.warn("No binding found for campaign: {}", campaignId);
                return null;
            }

            RuleBinding firstEnabled = null;
            for (RuleBinding binding : bindings) {
                RuleBinding enabled = enableBindingSafely(binding);
                if (enabled != null && firstEnabled == null) {
                    firstEnabled = enabled;
                }
            }
            return firstEnabled;

        } catch (Exception e) {
            logger.error("Error executing enable: campaignId={}, validationRuleId={}", campaignId, validationRuleId, e);
            return null;
        }
    }

    private RuleBinding enableBindingSafely(RuleBinding binding) {
        try {
            return enableSingleBinding(binding);
        } catch (Exception inner) {
            logger.error("Failed to enable binding bindingId={}, ruleId={}",
                    binding.getId(), binding.getRuleId(), inner);
            return null;
        }
    }

    private RuleBinding enableSingleBinding(RuleBinding binding) {
        logger.info("Found binding to enable: bindingId={}, ruleId={}, currentActive={}",
                binding.getId(), binding.getRuleId(), binding.getActive());

        RuleBinding updated = binding.toBuilder()
                .active(true)
                .updatedAt(Instant.now())
                .updatedBy("system")
                .build();
        ruleBindingPort.save(updated);

        logger.info("Enabled binding: bindingId={}", binding.getId());

        RuleBinding redeployed = binding.getRuleId() != null && !binding.getRuleId().isBlank()
                ? deployRuleToEngine(updated)
                : deployRuleLessBinding(updated);
        if (redeployed != null) {
            updated = redeployed;
        }

        return updated;
    }

    /**
     * PROM-1437: bật lại một binding KHÔNG gắn rule nghiệp vụ vẫn phải biên dịch lại bundle.
     *
     * <p>Nhánh này trước đây bị bỏ qua hoàn toàn, nên binding chỉ mang khung thời gian giữ mãi
     * {@code bundleHash} sinh ra lúc tạo — mà lúc tạo binding chưa kịp lưu nên {@code timeLinks}
     * rơi mất và rule-engine trả về bundle {@code temporal_check_allow_24_7} (ALLOW mọi ngày).
     *
     * @return binding kèm bundleHash mới, hoặc {@code null} khi không có gì để biên dịch / publish lỗi
     */
    private RuleBinding deployRuleLessBinding(RuleBinding binding) {
        if (!binding.hasTemporalConstraints()) {
            logger.info("Skipping deployment - no ruleId and no temporal policy: bindingId={}", binding.getId());
            return null;
        }

        logger.info("Redeploying rule-less binding on enable: bindingId={}", binding.getId());

        var publishResult = rulePublishingService.publishAssignmentBundle(binding.getId(), binding, null, true);

        if (!publishResult.isSuccess()) {
            logger.error("Failed to redeploy rule-less binding: bindingId={}, error={}",
                    binding.getId(), publishResult.getErrorMessage());
            return null;
        }

        RuleBinding updated = binding.toBuilder()
                .bundleHash(publishResult.getBundleHash())
                .build();
        ruleBindingPort.save(updated);
        logger.info("Rule-less binding redeployed: bindingId={}, bundleHash={}",
                binding.getId(), publishResult.getBundleHash());
        return updated;
    }

    private RuleBinding deployRuleToEngine(RuleBinding binding) {
        try {
            String ruleId = binding.getRuleId();

            logger.info("Deploying rule: ruleId={}, bindingId={}", ruleId, binding.getId());

            if (!validationRulePort.existsById(ruleId)) {
                logger.warn("Rule not found for deployment: ruleId={}", ruleId);
                return null;
            }

            var publishResult = rulePublishingService.publishRule(ruleId, binding.getId(), null);

            if (publishResult.isSuccess()) {
                RuleBinding updated = binding.toBuilder()
                        .bundleHash(publishResult.getBundleHash())
                        .build();
                ruleBindingPort.save(updated);
                logger.info("Rule deployed: ruleId={}, bundleHash={}", ruleId, publishResult.getBundleHash());
                return updated;
            }
            logger.error("Failed to deploy rule: ruleId={}, error={}", ruleId, publishResult.getErrorMessage());
            return null;

        } catch (Exception e) {
            logger.error("Error deploying rule: bindingId={}", binding.getId(), e);
            return null;
        }
    }

    private void publishEnableSuccessEvent(String commandId, String campaignId, RuleBinding binding) {
        try {
            eventPublisher.publishEnableSuccessEvent(commandId, campaignId, binding);
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
        String commandId = command.getId();
        logger.error("Processing dead letter EnableValidationRuleCommand: commandId={}", commandId);

        // If already successfully processed, skip failure event to prevent duplicate SUCCESS+FAILURE
        if (idempotencyService.isProcessed(commandId)) {
            logger.info("Enable command already processed successfully, skipping DLQ failure event: commandId={}", commandId);
            return;
        }

        publishEnableErrorEvent(
                commandId,
                command.getPayload() != null ? command.getPayload().getCampaignId() : null,
                "DLQ_PROCESSING",
                "Command moved to dead letter queue"
        );
    }
}
