package vn.viettel.vds.promotion.validation.domain.valueobject;

import vn.viettel.vds.promotion.validation.domain.exception.InvalidRuleIdException;

import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing a unique identifier for a Rule
 */
public class RuleId {
    private final String value;

    private RuleId(String value) {
        Objects.requireNonNull(value, "RuleId cannot be null");
        if (value.trim().isEmpty()) {
            throw new InvalidRuleIdException("RuleId cannot be empty");
        }
        this.value = value;
    }

    public static RuleId of(String value) {
        return new RuleId(value);
    }

    public static RuleId generate() {
        return new RuleId(UUID.randomUUID().toString());
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RuleId ruleId = (RuleId) o;
        return Objects.equals(value, ruleId.value);
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