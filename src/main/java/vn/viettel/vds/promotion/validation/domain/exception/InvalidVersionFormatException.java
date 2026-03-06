package vn.viettel.vds.promotion.validation.domain.exception;

import com.promix.platform.core.exception.BadRequestException;

/**
 * Exception thrown when version format is invalid.
 * HTTP Status: 400 Bad Request
 */
public class InvalidVersionFormatException extends BadRequestException {

    private static final String ERROR_CODE = "INVALID_VERSION_FORMAT";

    public InvalidVersionFormatException(String message) {
        super(ERROR_CODE, message);
    }

    public InvalidVersionFormatException(String versionString, Throwable cause) {
        super(ERROR_CODE, "Invalid version format: " + versionString, cause);
    }
}
