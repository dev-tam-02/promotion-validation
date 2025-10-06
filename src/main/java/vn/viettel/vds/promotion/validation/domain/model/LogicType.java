package vn.viettel.vds.promotion.validation.domain.model;

/**
 * Enumeration representing the logic type for rule evaluation
 */
public enum LogicType {
    AND("All conditions must be true"),
    OR("At least one condition must be true"),
    NOT("Negates the condition"),
    XOR("Exactly one condition must be true"),
    CUSTOM("Custom logic implementation");

    private final String description;

    LogicType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}