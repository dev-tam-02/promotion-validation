package vn.viettel.vds.promotion.validation.domain.exception;

import com.promix.platform.core.exception.BadRequestException;

/**
 * Exception thrown when a JSON schema is invalid.
 */
public class InvalidJsonSchemaException extends BadRequestException {

    private static final String ERROR_CODE = "INVALID_JSON_SCHEMA";

    public InvalidJsonSchemaException(String message) {
        super(ERROR_CODE, message);
    }

    public InvalidJsonSchemaException(String message, Throwable cause) {
        super(ERROR_CODE, "Invalid JSON schema: " + message, cause);
    }
}
