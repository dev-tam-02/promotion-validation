package vn.viettel.vds.promotion.validation.domain.exception;

import com.promix.platform.core.exception.BusinessRuleException;

/**
 * Exception thrown when timeframe processing fails.
 * HTTP Status: 422 Unprocessable Entity
 */
public class TimeframeProcessingException extends BusinessRuleException {

    private static final String ERROR_CODE = "TIMEFRAME_PROCESSING_FAILED";

    public TimeframeProcessingException(String message) {
        super(ERROR_CODE, message);
    }

    public TimeframeProcessingException(String message, Throwable cause) {
        super(ERROR_CODE, message, cause);
    }
}