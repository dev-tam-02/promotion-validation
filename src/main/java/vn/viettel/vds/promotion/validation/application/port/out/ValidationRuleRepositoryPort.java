package vn.viettel.vds.promotion.validation.application.port.out;

import vn.viettel.vds.promotion.validation.domain.model.Rule;

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
    Optional<Rule> findById(String ruleId);

    /**
     * Find all active rules (rules in PUBLISHED state)
     * <p>
     * Active rules are those with state = PUBLISHED. The state field determines
     * whether a rule is active, along with the active boolean flag.
     * </p>
     *
     * @return list of active (published) validation rules sorted by priority
     */
    List<Rule> findActiveRules();

    /**
     * Find rules by type
     *
     * @param type the rule type
     * @return list of validation rules of specified type
     */
    List<Rule> findByType(Rule.RuleType type);

    /**
     * Find rules by rule set ID
     *
     * @param ruleSetId the rule set ID
     * @return list of validation rules in the rule set
     */
    List<Rule> findByRuleSetId(String ruleSetId);

    /**
     * Find rules applicable to a promotion
     *
     * @param promotionId the promotion ID
     * @return list of applicable validation rules
     */
    List<Rule> findByPromotionId(String promotionId);

    /**
     * Save or update a validation rule
     *
     * @param rule the validation rule
     * @return saved validation rule
     */
    Rule save(Rule rule);

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
    List<Rule> findByPriorityRange(int minPriority, int maxPriority);

    /**
     * Find rules applicable to customer segment
     *
     * @param segment the customer segment
     * @return list of validation rules for the segment
     */
    List<Rule> findByTargetSegment(String segment);
}