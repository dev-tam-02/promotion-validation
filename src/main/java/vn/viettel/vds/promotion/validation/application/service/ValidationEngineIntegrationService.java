package vn.viettel.vds.promotion.validation.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import vn.viettel.vds.promotion.validation.adapter.out.integration.ValidationEngineDeploymentService;
import vn.viettel.vds.promotion.validation.application.port.out.RuleBindingPersistencePort;
import vn.viettel.vds.promotion.validation.application.port.out.ValidationRuleRepositoryPort;
import vn.viettel.vds.promotion.validation.domain.model.Rule;
import vn.viettel.vds.promotion.validation.domain.model.RuleBinding;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service for managing integration and synchronization with validation-engine.
 * <p>
 * Refactored to use unified RuleBinding model.
 */
@Service
public class ValidationEngineIntegrationService {

    private static final Logger logger = LoggerFactory.getLogger(ValidationEngineIntegrationService.class);

    private static final String STATUS_CONNECTED = "Connected";
    private static final String STATUS_DISCONNECTED = "Disconnected";
    private static final String STATUS_ERROR_PREFIX = "Error: ";

    private final ValidationEngineDeploymentService validationEngineClient;
    private final ValidationRuleRepositoryPort validationRulePort;
    private final RuleBindingPersistencePort ruleBindingPort;

    public ValidationEngineIntegrationService(
            ValidationEngineDeploymentService validationEngineClient,
            ValidationRuleRepositoryPort validationRulePort,
            RuleBindingPersistencePort ruleBindingPort) {
        this.validationEngineClient = validationEngineClient;
        this.validationRulePort = validationRulePort;
        this.ruleBindingPort = ruleBindingPort;
    }

    /**
     * Synchronize all active rules with validation-engine
     */
    @Async("validationEngineExecutor")
    public void synchronizeAllRules() {
        logger.info("Starting full rule synchronization with validation-engine");

        List<RuleBinding> activeBindings = ruleBindingPort.findByActive(true);

        long successCount = activeBindings.stream()
                .filter(this::synchronizeBinding)
                .count();

        logger.info("Rule synchronization completed: success={}, failures={}, total={}",
                successCount, activeBindings.size() - successCount, activeBindings.size());

        // Note: reloadRules() is deprecated and does nothing.
        // Bundle management is now automatic in validation-engine.
    }

    private boolean synchronizeBinding(RuleBinding binding) {
        try {
            if (binding.getRuleId() == null) {
                logger.warn("Binding has no ruleId: bindingId={}", binding.getId());
                return false;
            }

            var validationRuleOpt = validationRulePort.findById(binding.getRuleId());
            if (validationRuleOpt.isEmpty()) {
                logger.warn("Validation rule not found for active binding: ruleId={}, bindingId={}",
                        binding.getRuleId(), binding.getId());
                return false;
            }

            Rule rule = validationRuleOpt.get();

            // Note: deployRule() is deprecated and does nothing.
            // Actual rule deployment is handled by RulePublishingService.publishRule()
            logger.debug("Rule binding found for synchronization: ruleId={}, bindingId={}",
                    rule.getId(), binding.getId());
            return true;

        } catch (Exception e) {
            logger.error("Error synchronizing rule for binding: bindingId={}",
                    binding.getId(), e);
            return false;
        }
    }

    /**
     * Deploy a specific rule binding to validation-engine
     */
    @Async("validationEngineExecutor")
    public CompletableFuture<Boolean> deployRuleBinding(String bindingId) {
        logger.info("Deploying specific rule binding: bindingId={}", bindingId);

        try {
            // Get the binding via port
            var bindingOpt = ruleBindingPort.findById(bindingId);
            if (bindingOpt.isEmpty()) {
                logger.warn("Binding not found: bindingId={}", bindingId);
                return CompletableFuture.completedFuture(false);
            }

            RuleBinding binding = bindingOpt.get();

            // Only deploy if active
            if (binding.getActive() == null || !binding.getActive()) {
                logger.info("Skipping deployment - binding is not active: bindingId={}", bindingId);
                return CompletableFuture.completedFuture(false);
            }

            // Get the validation rule via port
            if (binding.getRuleId() == null) {
                logger.warn("Binding has no ruleId: bindingId={}", bindingId);
                return CompletableFuture.completedFuture(false);
            }

            var validationRuleOpt = validationRulePort.findById(binding.getRuleId());
            if (validationRuleOpt.isEmpty()) {
                logger.warn("Validation rule not found for binding: ruleId={}, bindingId={}",
                        binding.getRuleId(), bindingId);
                return CompletableFuture.completedFuture(false);
            }

            Rule rule = validationRuleOpt.get();

            // Note: deployRule() is deprecated and does nothing.
            // Actual rule deployment is handled by RulePublishingService.publishRule()
            logger.info("Rule binding ready for deployment: ruleId={}, bindingId={}",
                    rule.getId(), bindingId);

            return CompletableFuture.completedFuture(true);

        } catch (Exception e) {
            logger.error("Error deploying rule binding: bindingId={}", bindingId, e);
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * Remove a rule from validation-engine
     */
    @Async("validationEngineExecutor")
    public CompletableFuture<Boolean> removeRule(String ruleId) {
        logger.info("Removing rule from validation-engine: ruleId={}", ruleId);

        try {
            // Note: removeRule() is deprecated and does nothing.
            // Bundle management is now automatic in validation-engine.
            logger.info("Rule removal requested (automatic management): ruleId={}", ruleId);
            return CompletableFuture.completedFuture(true);

        } catch (Exception e) {
            logger.error("Error removing rule from validation-engine: ruleId={}", ruleId, e);
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * Check validation-engine health and connectivity
     */
    public boolean isValidationEngineHealthy() {
        try {
            return validationEngineClient.isHealthy();
        } catch (Exception e) {
            logger.error("Error checking validation-engine health", e);
            return false;
        }
    }

    /**
     * Get synchronization status
     */
    public SynchronizationStatus getSynchronizationStatus() {
        try {
            // Count active bindings
            long activeBindings = ruleBindingPort.countByActive(true);

            // Check validation-engine health
            boolean engineHealthy = isValidationEngineHealthy();

            return new SynchronizationStatus(
                    activeBindings,
                    engineHealthy,
                    engineHealthy ? STATUS_CONNECTED : STATUS_DISCONNECTED
            );

        } catch (Exception e) {
            logger.error("Error getting synchronization status", e);
            return new SynchronizationStatus(0, false, STATUS_ERROR_PREFIX + e.getMessage());
        }
    }
}
