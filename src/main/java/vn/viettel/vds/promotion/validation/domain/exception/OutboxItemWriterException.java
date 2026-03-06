package vn.viettel.vds.promotion.validation.domain.exception;

import com.promix.platform.core.exception.InternalException;

/**
 * Exception thrown when outbox item writing fails.
 * HTTP Status: 500 Internal Server Error
 */
public class OutboxItemWriterException extends InternalException {

    private static final String ERROR_CODE = "OUTBOX_ITEM_WRITE_FAILED";

    public OutboxItemWriterException(String message) {
        super(ERROR_CODE, message);
    }

    public OutboxItemWriterException(String message, Throwable cause) {
        super(ERROR_CODE, message, cause);
    }
}
