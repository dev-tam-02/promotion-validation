package vn.viettel.vds.promotion.validation.domain.exception;

import com.promix.platform.core.exception.BadRequestException;

/**
 * Exception thrown when rule ID is invalid.
 * HTTP Status: 400 Bad Request
 */
public class InvalidRuleIdException extends BadRequestException {

    private static final String ERROR_CODE = "INVALID_RULE_ID";

    public InvalidRuleIdException(String message) {
        super(ERROR_CODE, message);
    }
}
