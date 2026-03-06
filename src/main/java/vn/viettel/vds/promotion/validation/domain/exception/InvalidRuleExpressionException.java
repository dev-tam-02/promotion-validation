package vn.viettel.vds.promotion.validation.domain.exception;

import com.promix.platform.core.exception.BadRequestException;

/**
 * Exception thrown when rule expression is invalid.
 * HTTP Status: 400 Bad Request
 */
public class InvalidRuleExpressionException extends BadRequestException {

    private static final String ERROR_CODE = "INVALID_RULE_EXPRESSION";

    public InvalidRuleExpressionException(String message) {
        super(ERROR_CODE, message);
    }

    public InvalidRuleExpressionException(String message, Throwable cause) {
        super(ERROR_CODE, message, cause);
    }
}
