package vn.viettel.vds.promotion.validation.application.port.out;

import vn.viettel.vds.promotion.validation.domain.model.ValidationRule;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port for validation rule persistence
 * Defines repository operations for validation rules
 */
public interface ValidationRuleRepositoryPort {

    /**
     * Find validation rule by ID
     *
     * @param ruleId the rule ID
     * @return optional containing the rule if found
     */
    Optional<ValidationRule> findById(String ruleId);

    /**
     * Find all active rules
     *
     * @return list of active validation rules
     */
    List<ValidationRule> findActiveRules();

    /**
     * Find rules by type
     *
     * @param type the rule type
     * @return list of validation rules of specified type
     */
    List<ValidationRule> findByType(ValidationRule.RuleType type);

    /**
     * Find rules by rule set ID
     *
     * @param ruleSetId the rule set ID
     * @return list of validation rules in the rule set
     */
    List<ValidationRule> findByRuleSetId(String ruleSetId);

    /**
     * Find rules applicable to a promotion
     *
     * @param promotionId the promotion ID
     * @return list of applicable validation rules
     */
    List<ValidationRule> findByPromotionId(String promotionId);

    /**
     * Save or update a validation rule
     *
     * @param rule the validation rule
     * @return saved validation rule
     */
    ValidationRule save(ValidationRule rule);

    /**
     * Delete a validation rule
     *
     * @param ruleId the rule ID
     */
    void deleteById(String ruleId);

    /**
     * Check if a rule exists
     *
     * @param ruleId the rule ID
     * @return true if rule exists
     */
    boolean existsById(String ruleId);

    /**
     * Find rules by priority range
     *
     * @param minPriority minimum priority
     * @param maxPriority maximum priority
     * @return list of validation rules within priority range
     */
    List<ValidationRule> findByPriorityRange(int minPriority, int maxPriority);

    /**
     * Find rules applicable to customer segment
     *
     * @param segment the customer segment
     * @return list of validation rules for the segment
     */
    List<ValidationRule> findByTargetSegment(String segment);
}