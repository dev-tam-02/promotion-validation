package vn.viettel.vds.promotion.validation.domain.valueobject;

import java.util.Objects;

/**
 * Value object representing a rule name
 */
public class RuleName {
    private static final int MIN_LENGTH = 3;
    private static final int MAX_LENGTH = 100;
    private final String value;

    private RuleName(String value) {
        Objects.requireNonNull(value, "RuleName cannot be null");
        String trimmed = value.trim();

        if (trimmed.length() < MIN_LENGTH || trimmed.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    String.format("RuleName must be between %d and %d characters", MIN_LENGTH, MAX_LENGTH)
            );
        }
        this.value = trimmed;
    }

    public static RuleName of(String value) {
        return new RuleName(value);
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RuleName ruleName = (RuleName) o;
        return Objects.equals(value, ruleName.value);
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