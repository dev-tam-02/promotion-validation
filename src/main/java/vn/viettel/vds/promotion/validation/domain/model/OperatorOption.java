package vn.viettel.vds.promotion.validation.domain.model;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Domain model representing an operator option within a category.
 * Options define how operators appear and behave in the UI rule builder.
 */
@Value
@Builder(toBuilder = true)
public class OperatorOption {
    String id;
    String categoryId;
    String code;
    String name;
    Integer displayOrder;
    String description;
    String operatorName;
    Integer operatorVersion;

    // UI Configuration
    ComparisonType comparisonType;
    List<String> availableComparators;
    String defaultComparator;
    ValueType valueType;
    ValueSource valueSource;
    List<ValueOption> valueOptions;

    // Validation
    BigDecimal minValue;
    BigDecimal maxValue;
    String pattern;

    boolean active;

    // Audit fields
    Instant createdAt;
    Instant updatedAt;

    /**
     * Type of comparison the option supports.
     */
    public enum ComparisonType {
        SINGLE,   // Single value comparison (equals, not_equals)
        RANGE,    // Range comparison (gte, lte, between)
        LIST,     // List comparison (in, not_in)
        BOOLEAN   // Boolean comparison (true/false)
    }

    /**
     * Type of value the option accepts.
     */
    public enum ValueType {
        NUMBER,
        STRING,
        LIST,
        BOOLEAN,
        DATE
    }

    /**
     * Source of values for the option.
     */
    public enum ValueSource {
        INPUT,        // Free text input
        SELECT,       // Single select dropdown
        MULTI_SELECT, // Multi-select dropdown
        SCHEMA        // Dynamic from metadata schema
    }

    /**
     * Predefined value option for select/multi-select.
     */
    @Value
    @Builder
    public static class ValueOption {
        String value;
        String label;
    }
}
