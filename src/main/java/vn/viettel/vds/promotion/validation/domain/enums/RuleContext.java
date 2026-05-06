package vn.viettel.vds.promotion.validation.domain.enums;

/**
 * Enum defining the trigger-event contexts in which a validation rule applies.
 *
 * <p>Used by {@code context} field on {@code CreateRuleRequest} / {@code UpdateRuleRequest}
 * and validated at the HTTP adapter layer via {@code @ValidEnum}.</p>
 *
 * <p>Values must stay in sync with the FE {@code ValidationRuleContext} enum.</p>
 */
public enum RuleContext {

    COMMON,
    CUSTOMER_CREATED,
    ORDER_CREATED,
    PAYMENT_COMPLETED,
    PROMOTION_APPLIED;

    /**
     * Case-insensitive lookup — returns true if the given string matches any enum name.
     */
    public static boolean isValid(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        for (RuleContext c : values()) {
            if (c.name().equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }
}
