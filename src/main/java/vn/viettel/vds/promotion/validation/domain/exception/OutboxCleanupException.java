package vn.viettel.vds.promotion.validation.domain.exception;

public class OutboxCleanupException extends RuntimeException {

    public OutboxCleanupException(String message, Throwable cause) {
        super(message, cause);
    }
}
