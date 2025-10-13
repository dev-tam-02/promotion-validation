package vn.viettel.vds.promotion.validation.domain.exception;

/**
 * Exception thrown when timeframe processing fails
 */
public class TimeframeProcessingException extends RuntimeException {
    
    public TimeframeProcessingException(String message) {
        super(message);
    }
    
    public TimeframeProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}