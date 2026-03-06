package vn.viettel.vds.promotion.validation.domain.exception;

import com.promix.platform.core.exception.BusinessRuleException;

import java.util.Map;

/**
 * Exception thrown when a publish job state transition is invalid.
 */
public class InvalidPublishJobStateException extends BusinessRuleException {

    private static final String ERROR_CODE = "INVALID_PUBLISH_JOB_STATE";

    public InvalidPublishJobStateException(String operation, String currentState) {
        super(ERROR_CODE,
                String.format("Cannot %s job in status: %s", operation, currentState),
                Map.of("operation", operation, "currentState", currentState));
    }

    public InvalidPublishJobStateException(String message) {
        super(ERROR_CODE, message);
    }
}
