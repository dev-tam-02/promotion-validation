package vn.viettel.vds.promotion.validation.domain.valueobject;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value object representing a rule code
 * Must follow the pattern: RULE_[A-Z0-9_]+
 */
public class RuleCode {
    private static final Pattern VALID_PATTERN = Pattern.compile("^RULE_[A-Z0-9_]+$");
    private final String value;

    private RuleCode(String value) {
        Objects.requireNonNull(value, "RuleCode cannot be null");
        if (!VALID_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "RuleCode must follow pattern RULE_[A-Z0-9_]+ but was: " + value
            );
        }
        this.value = value;
    }

    public static RuleCode of(String value) {
        return new RuleCode(value);
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RuleCode ruleCode = (RuleCode) o;
        return Objects.equals(value, ruleCode.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}