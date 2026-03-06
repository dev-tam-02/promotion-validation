package vn.viettel.vds.promotion.validation.domain.exception;

import com.promix.platform.core.exception.BadRequestException;

/**
 * Exception thrown when rule code is invalid.
 * HTTP Status: 400 Bad Request
 */
public class InvalidRuleCodeException extends BadRequestException {

    private static final String ERROR_CODE = "INVALID_RULE_CODE";

    public InvalidRuleCodeException(String message) {
        super(ERROR_CODE, message);
    }

    public InvalidRuleCodeException(String ruleCode, int minLength, int maxLength) {
        super(ERROR_CODE, String.format("Rule code '%s' must be between %d and %d characters",
                ruleCode, minLength, maxLength));
    }
}
