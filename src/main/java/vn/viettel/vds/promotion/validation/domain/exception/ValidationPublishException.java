package vn.viettel.vds.promotion.validation.domain.exception;

/**
 * Exception thrown when publishing validation results fails.
 *
 * @author Validation Team
 * @since 1.0.0
 */
public class ValidationPublishException extends RuntimeException {

    /**
     * Constructs a new validation publish exception with the specified detail message.
     *
     * @param message the detail message
     */
    public ValidationPublishException(String message) {
        super(message);
    }

    /**
     * Constructs a new validation publish exception with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause
     */
    public ValidationPublishException(String message, Throwable cause) {
        super(message, cause);
    }
}
