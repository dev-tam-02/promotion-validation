package vn.viettel.vds.promotion.validation.domain.exception;

public class OutboxItemWriterException extends RuntimeException {

    public OutboxItemWriterException(String message, Throwable cause) {
        super(message, cause);
    }
}
