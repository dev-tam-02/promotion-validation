package vn.viettel.vds.promotion.validation.domain.exception;

import com.promix.platform.core.exception.BadRequestException;

/**
 * Exception thrown when rule validation fails during publishing.
 */
public class RuleValidationFailedException extends BadRequestException {

    private static final String ERROR_CODE = "RULE_VALIDATION_FAILED";

    public RuleValidationFailedException(String message) {
        super(ERROR_CODE, message);
    }

    public RuleValidationFailedException(String ruleId, String errors) {
        super(ERROR_CODE, String.format("Rule %s validation failed: %s", ruleId, errors));
    }
}
