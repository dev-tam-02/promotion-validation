package vn.viettel.vds.promotion.validation.domain.exception;

import com.promix.platform.core.exception.BadRequestException;

import java.util.Map;

/**
 * Exception thrown when operator is invalid or null.
 * HTTP Status: 400 Bad Request
 */
public class InvalidOperatorException extends BadRequestException {

    private static final String ERROR_CODE = "INVALID_OPERATOR";

    public InvalidOperatorException(String message) {
        super(ERROR_CODE, message);
    }

    public InvalidOperatorException(String operator, String reason) {
        super(ERROR_CODE, String.format("Invalid operator '%s': %s", operator, reason),
                Map.of("operator", operator, "reason", reason));
    }
}
