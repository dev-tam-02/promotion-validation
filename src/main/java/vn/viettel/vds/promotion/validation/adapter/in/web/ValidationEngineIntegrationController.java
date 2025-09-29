package vn.viettel.vds.promotion.validation.adapter.in.web;

import com.promix.platform.web.annotation.ResponseWrapper;
import org.springframework.web.bind.annotation.*;
import vn.viettel.vds.promotion.validation.application.service.ValidationEngineIntegrationService;

import java.util.concurrent.CompletableFuture;

/**
 * REST controller for managing validation-engine integration
 */
@RestController
@RequestMapping("/api/validation-engine")
@ResponseWrapper
public class ValidationEngineIntegrationController {

    private final ValidationEngineIntegrationService integrationService;

    public ValidationEngineIntegrationController(ValidationEngineIntegrationService integrationService) {
        this.integrationService = integrationService;
    }

    /**
     * Get synchronization status
     */
    @GetMapping("/status")
    public ValidationEngineIntegrationService.SynchronizationStatus getStatus() {
        return integrationService.getSynchronizationStatus();
    }

    /**
     * Trigger full synchronization of all rules
     */
    @PostMapping("/sync")
    public String synchronizeAllRules() {
        integrationService.synchronizeAllRules();
        return "Rule synchronization started";
    }

    /**
     * Deploy a specific rule assignment
     */
    @PostMapping("/deploy/{assignmentId}")
    public CompletableFuture<Boolean> deployRuleAssignment(@PathVariable String assignmentId) {
        return integrationService.deployRuleAssignment(assignmentId);
    }

    /**
     * Remove a rule from validation-engine
     */
    @DeleteMapping("/rules/{ruleId}")
    public CompletableFuture<Boolean> removeRule(@PathVariable String ruleId) {
        return integrationService.removeRule(ruleId);
    }

    /**
     * Check validation-engine health
     */
    @GetMapping("/health")
    public Boolean checkHealth() {
        return integrationService.isValidationEngineHealthy();
    }
}