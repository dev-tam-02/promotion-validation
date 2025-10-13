package vn.viettel.vds.promotion.validation.domain.exception;

/**
 * Exception thrown when validation operations fail
 */
public class ValidationException extends RuntimeException {
    
    public ValidationException(String message) {
        super(message);
    }
    
    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}