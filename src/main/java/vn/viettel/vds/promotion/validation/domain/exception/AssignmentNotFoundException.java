package vn.viettel.vds.promotion.validation.domain.exception;

/**
 * Exception được throw khi validation rule assignment không tồn tại.
 */
public class AssignmentNotFoundException extends RuntimeException {

    private final String validationRuleId;
    private final String objectId;

    public AssignmentNotFoundException(String validationRuleId, String objectId) {
        super(String.format("Validation rule assignment not found: validationRuleId=%s, objectId=%s",
                validationRuleId, objectId));
        this.validationRuleId = validationRuleId;
        this.objectId = objectId;
    }

    public String getValidationRuleId() {
        return validationRuleId;
    }

    public String getObjectId() {
        return objectId;
    }
}
