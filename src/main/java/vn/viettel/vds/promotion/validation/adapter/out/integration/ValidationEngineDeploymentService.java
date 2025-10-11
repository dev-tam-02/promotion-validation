package vn.viettel.vds.promotion.validation.adapter.out.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.ValidationRuleEntity;

/**
 * Service for deploying validation rules to the validation engine
 * Updated to use the new compile-based approach instead of direct deployment
 */
@Service
public class ValidationEngineDeploymentService {

    private static final Logger logger = LoggerFactory.getLogger(ValidationEngineDeploymentService.class);

    private final ValidationEngineClient validationEngineClient;

    public ValidationEngineDeploymentService(ValidationEngineClient validationEngineClient) {
        this.validationEngineClient = validationEngineClient;
    }

    /**
     * This method is deprecated - use RulePublishingService instead for rule compilation
     */
    @Deprecated
    public boolean deployRule(ValidationRuleEntity rule) {
        logger.warn("deployRule is deprecated - use RulePublishingService.publishRule instead");
        return true;
    }

    /**
     * This method is deprecated - bundle management is now handled automatically by compile/warmup
     */
    @Deprecated
    public boolean removeRule(String ruleId) {
        logger.warn("removeRule is deprecated - bundle management is now automatic");
        return true;
    }

    /**
     * This method is deprecated - bundle management is now handled automatically by compile/warmup
     */
    @Deprecated
    public boolean reloadRules() {
        logger.warn("reloadRules is deprecated - bundle management is now automatic");
        return true;
    }

    /**
     * Check if validation-engine is healthy
     */
    public boolean isHealthy() {
        try {
            ResponseEntity<String> response = validationEngineClient.getHealth();
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            logger.warn("Validation-engine health check failed", e);
            return false;
        }
    }

    /**
     * Generate DRL (Drools Rule Language) content from ValidationRuleEntity
     * This is a simplified version - in production you'd want more sophisticated mapping
     */
    private String generateDrlFromRule(ValidationRuleEntity rule) {
        // TODO: Implement proper DRL generation based on rule structure
        // For now, return a basic template

        return String.format("""
                        package vn.viettel.vds.promotion.validation.rules;
                        
                        import vn.viettel.vds.promotion.validation.engine.domain.model.Customer;
                        import vn.viettel.vds.promotion.validation.engine.domain.model.Order;
                        import vn.viettel.vds.promotion.validation.engine.domain.model.ValidationResult;
                        
                        rule "%s_v%s"
                            when
                                $customer : Customer()
                                $order : Order()
                            then
                                // TODO: Implement rule logic based on %s
                                ValidationResult result = new ValidationResult();
                                result.setRuleId("%s");
                                result.setValid(true);
                                result.setMessage("Rule %s executed successfully");
                                insert(result);
                        end
                        """,
                rule.getName().replaceAll("\\s+", "_"),
                rule.getRuleVersion(),
                "validation logic",
                rule.getId(),
                rule.getName()
        );
    }
}