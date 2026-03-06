package vn.viettel.vds.promotion.validation.domain.exception;

import com.promix.platform.core.exception.ResourceNotFoundException;

/**
 * Exception thrown when a validation rule is not found.
 * HTTP Status: 404 Not Found
 */
public class RuleNotFoundException extends ResourceNotFoundException {

    private static final String ERROR_CODE = "VALIDATION_RULE_NOT_FOUND";

    public RuleNotFoundException(String ruleId) {
        super(ERROR_CODE, "ValidationRule", ruleId);
    }

    public RuleNotFoundException(String field, String value) {
        super(ERROR_CODE, "ValidationRule", value,
                String.format("Rule not found with %s: %s", field, value));
    }
}