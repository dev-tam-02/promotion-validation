package vn.viettel.vds.promotion.validation.domain.exception;

import com.promix.platform.core.exception.ExternalServiceException;

/**
 * Exception thrown when publishing validation results fails.
 * HTTP Status: 502 Bad Gateway
 */
public class ValidationPublishException extends ExternalServiceException {

    private static final String ERROR_CODE = "VALIDATION_PUBLISH_FAILED";
    private static final String SERVICE_NAME = "validation-publisher";

    public ValidationPublishException(String message) {
        super(ERROR_CODE, message);
    }

    public ValidationPublishException(String message, Throwable cause) {
        super(ERROR_CODE, SERVICE_NAME, message, cause);
    }
}
