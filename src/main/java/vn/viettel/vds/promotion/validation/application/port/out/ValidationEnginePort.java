package vn.viettel.vds.promotion.validation.application.port.out;

import vn.viettel.vds.promotion.validation.domain.model.ValidationRequest;
import vn.viettel.vds.promotion.validation.domain.model.ValidationResult;

import java.util.Map;

/**
 * Outbound port for external validation engine integration
 * Defines operations for interacting with validation engine service
 */
public interface ValidationEnginePort {

    /**
     * Execute validation using external engine
     *
     * @param request validation request
     * @return validation result from engine
     */
    ValidationResult executeValidation(ValidationRequest request);

    /**
     * Perform fast check using external engine
     *
     * @param campaignId campaign ID to validate
     * @param context    validation context
     * @return validation result
     */
    ValidationResult performFastCheck(String campaignId, Map<String, Object> context);

    /**
     * Compile validation rules in the engine
     *
     * @param rules rule expressions to compile
     * @return compilation result
     */
    Map<String, Object> compileRules(Map<String, String> rules);

    /**
     * Deploy rules to validation engine
     *
     * @param ruleSetId rule set ID
     * @param rules     rules to deploy
     * @return deployment status
     */
    boolean deployRules(String ruleSetId, Map<String, Object> rules);

    /**
     * Check if validation engine is available
     *
     * @return true if engine is healthy and available
     */
    boolean isAvailable();

    /**
     * Warm up the validation engine
     *
     * @param ruleSetId rule set to warm up
     * @return true if warm up successful
     */
    boolean warmUp(String ruleSetId);

    /**
     * Get supported operators from validation engine
     *
     * @return list of supported operators
     */
    Map<String, Object> getSupportedOperators();

    /**
     * Validate rule syntax
     *
     * @param rule rule expression to validate
     * @return validation result with any syntax errors
     */
    Map<String, Object> validateRuleSyntax(String rule);
}