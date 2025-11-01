package vn.viettel.vds.promotion.validation.domain.exception;

/**
 * Exception được throw khi validation rule không tồn tại.
 */
public class ValidationRuleNotFoundException extends RuntimeException {

    private final String validationRuleId;

    public ValidationRuleNotFoundException(String validationRuleId) {
        super(String.format("Validation rule not found with id: %s", validationRuleId));
        this.validationRuleId = validationRuleId;
    }

    public String getValidationRuleId() {
        return validationRuleId;
    }
}
