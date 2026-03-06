package vn.viettel.vds.promotion.validation.domain.exception;

import com.promix.platform.core.exception.InternalException;

/**
 * Exception thrown when command processing fails.
 * HTTP Status: 500 Internal Server Error
 */
public class CommandProcessingException extends InternalException {

    private static final String ERROR_CODE = "COMMAND_PROCESSING_FAILED";

    public CommandProcessingException(String message) {
        super(ERROR_CODE, message);
    }

    public CommandProcessingException(String message, Throwable cause) {
        super(ERROR_CODE, message, cause);
    }
}
