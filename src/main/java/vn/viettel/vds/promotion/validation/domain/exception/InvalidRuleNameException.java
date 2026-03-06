package vn.viettel.vds.promotion.validation.domain.exception;

import com.promix.platform.core.exception.BadRequestException;

/**
 * Exception thrown when rule name is invalid.
 * HTTP Status: 400 Bad Request
 */
public class InvalidRuleNameException extends BadRequestException {

    private static final String ERROR_CODE = "INVALID_RULE_NAME";

    public InvalidRuleNameException(String message) {
        super(ERROR_CODE, message);
    }

    public InvalidRuleNameException(String ruleName, int minLength, int maxLength) {
        super(ERROR_CODE, String.format("Rule name '%s' must be between %d and %d characters",
                ruleName, minLength, maxLength));
    }
}
