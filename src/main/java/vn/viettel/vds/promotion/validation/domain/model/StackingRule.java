package vn.viettel.vds.promotion.validation.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.function.Predicate;

/**
 * Domain model representing a stacking rule.
 * <p>
 * Defines a business rule that must be satisfied for discounts to be stacked.
 * </p>
 *
 * @author Validation Team
 * @since 1.0.0
 */
@Getter
@Builder
public class StackingRule {

    /**
     * Rule identifier
     */
    private final String ruleId;

    /**
     * Rule code for easy reference
     */
    private final String ruleCode;

    /**
     * Rule name
     */
    private final String name;

    /**
     * Rule description
     */
    private final String description;

    /**
     * Rule type
     */
    private final RuleType type;

    /**
     * Rule severity
     */
    private final RuleSeverity severity;

    /**
     * Whether rule is active
     */
    private final boolean active;

    /**
     * Target customer segments
     */
    private final List<String> targetSegments;

    /**
     * Error message when rule fails
     */
    private final String errorMessage;

    /**
     * Rule evaluation predicate
     */
    private final Predicate<StackingContext> evaluator;

    /**
     * Evaluates this rule against a stacking context.
     *
     * @param context stacking context
     * @return true if rule passes
     */
    public boolean evaluate(StackingContext context) {
        if (!active) {
            return true; // Inactive rules always pass
        }

        if (!appliesTo(context)) {
            return true; // Rule doesn't apply to this context
        }

        if (evaluator == null) {
            return true; // No evaluator means rule passes
        }

        return evaluator.test(context);
    }

    /**
     * Checks if this rule applies to the given context.
     *
     * @param context stacking context
     * @return true if rule applies
     */
    public boolean appliesTo(StackingContext context) {
        if (targetSegments == null || targetSegments.isEmpty()) {
            return true; // Rule applies to all segments
        }

        String customerSegment = context.getCustomerSegment();
        return customerSegment != null && targetSegments.contains(customerSegment);
    }

    /**
     * Gets the error message for rule failure.
     *
     * @return error message
     */
    public String getFailureMessage() {
        return errorMessage != null ? errorMessage : "Rule " + ruleCode + " failed";
    }

    /**
     * Rule type enumeration.
     */
    public enum RuleType {
        /**
         * Compatibility rule - checks if discounts can be combined
         */
        COMPATIBILITY,

        /**
         * Limit rule - checks against numerical limits
         */
        LIMIT,

        /**
         * Eligibility rule - checks customer eligibility
         */
        ELIGIBILITY,

        /**
         * Business rule - custom business logic
         */
        BUSINESS
    }

    /**
     * Rule severity enumeration.
     */
    public enum RuleSeverity {
        /**
         * Blocking - must pass for validation to succeed
         */
        BLOCKING,

        /**
         * Warning - failure generates warning but doesn't block
         */
        WARNING,

        /**
         * Info - informational only
         */
        INFO
    }
}
