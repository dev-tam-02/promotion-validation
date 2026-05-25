package vn.viettel.vds.promotion.validation.domain.enums;

/**
 * Enum defining the contexts in which a validation rule applies.
 *
 * <p>Used by {@code context} field on {@code CreateRuleRequest} / {@code UpdateRuleRequest}
 * and validated at the HTTP adapter layer via {@code @ValidEnum}.</p>
 *
 * <p>Values must stay in sync with {@link RuleContextType} (dropdown enum
 * returned by {@code GET /v1/rules/contexts}) and the FE
 * {@code ValidationRuleContext} enum.</p>
 */
public enum RuleContext {

    GENERAL_USAGE;

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
