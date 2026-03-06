package vn.viettel.vds.promotion.validation.domain.exception;

import com.promix.platform.core.exception.InternalException;

/**
 * Exception thrown when outbox cleanup operations fail.
 * HTTP Status: 500 Internal Server Error
 */
public class OutboxCleanupException extends InternalException {

    private static final String ERROR_CODE = "OUTBOX_CLEANUP_FAILED";

    public OutboxCleanupException(String message) {
        super(ERROR_CODE, message);
    }

    public OutboxCleanupException(String message, Throwable cause) {
        super(ERROR_CODE, message, cause);
    }
}
