package vn.viettel.vds.promotion.validation.application.port.in;

import vn.viettel.vds.promotion.validation.application.port.in.command.CreateRuleCommand;
import vn.viettel.vds.promotion.validation.application.port.in.command.DeployRulesCommand;
import vn.viettel.vds.promotion.validation.application.port.in.command.UpdateRuleCommand;
import vn.viettel.vds.promotion.validation.domain.model.Rule;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Inbound port for managing validation rules
 * Defines the contract for rule management operations
 */
public interface ManageValidationRulesUseCase {

    /**
     * Create a new validation rule
     */
    Rule createRule(CreateRuleCommand command);

    /**
     * Update an existing validation rule
     */
    Rule updateRule(String ruleId, UpdateRuleCommand command);

    /**
     * Delete a validation rule
     */
    void deleteRule(String ruleId);

    /**
     * Get a validation rule by ID
     */
    Optional<Rule> getRule(String ruleId);

    /**
     * Get all validation rules
     */
    List<Rule> getAllRules();

    /**
     * Get active validation rules
     */
    List<Rule> getActiveRules();

    /**
     * Activate a rule
     */
    Rule activateRule(String ruleId);

    /**
     * Deactivate a rule
     */
    Rule deactivateRule(String ruleId);

    /**
     * Deploy rules to validation engine
     */
    boolean deployRules(DeployRulesCommand command);

    /**
     * Validate rule syntax
     */
    boolean validateRuleSyntax(String ruleExpression);

    /**
     * Clone a rule
     */
    Rule cloneRule(String ruleId, String newRuleCode);

    /**
     * Get rules by promotion
     */
    List<Rule> getRulesByPromotion(String promotionId);

    /**
     * Get rules by type
     */
    List<Rule> getRulesByType(String ruleType);

    /**
     * Test a rule against sample data
     */
    boolean testRule(String ruleId, Map<String, Object> testData);

    /**
     * Bulk create rules
     */
    List<Rule> bulkCreateRules(List<CreateRuleCommand> commands);

    /**
     * Bulk update rules
     */
    List<Rule> bulkUpdateRules(Map<String, UpdateRuleCommand> updates);

    /**
     * Export rules as configuration
     */
    Map<String, Object> exportRules(List<String> ruleIds);

    /**
     * Import rules from configuration
     */
    List<Rule> importRules(Map<String, Object> configuration);
}