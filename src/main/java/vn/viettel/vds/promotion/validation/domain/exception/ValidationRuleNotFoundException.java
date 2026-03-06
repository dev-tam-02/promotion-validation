package vn.viettel.vds.promotion.validation.domain.exception;

import com.promix.platform.core.exception.ResourceNotFoundException;

/**
 * Exception thrown when validation rule is not found.
 * HTTP Status: 404 Not Found
 */
public class ValidationRuleNotFoundException extends ResourceNotFoundException {

    private static final String ERROR_CODE = "VALIDATION_RULE_NOT_FOUND";

    private final String validationRuleId;

    public ValidationRuleNotFoundException(String validationRuleId) {
        super(ERROR_CODE, "ValidationRule", validationRuleId);
        this.validationRuleId = validationRuleId;
    }

    public String getValidationRuleId() {
        return validationRuleId;
    }
}
