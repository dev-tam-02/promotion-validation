package vn.viettel.vds.promotion.validation.domain.exception;

import com.promix.platform.core.exception.BusinessRuleException;

import java.util.Map;

/**
 * Exception thrown when a rule state transition is invalid.
 * HTTP Status: 422 Unprocessable Entity
 */
public class InvalidRuleStateTransitionException extends BusinessRuleException {

    private static final String ERROR_CODE = "INVALID_RULE_STATE_TRANSITION";

    public InvalidRuleStateTransitionException(String message) {
        super(ERROR_CODE, message);
    }

    public InvalidRuleStateTransitionException(String currentStatus, String targetStatus) {
        super(ERROR_CODE,
                String.format("Cannot transition from %s to %s", currentStatus, targetStatus),
                Map.of("currentStatus", currentStatus, "targetStatus", targetStatus));
    }

    public InvalidRuleStateTransitionException(String operation, String currentStatus, String requiredStatus) {
        super(ERROR_CODE,
                String.format("Operation '%s' requires rule to be in status %s, but current status is %s",
                        operation, requiredStatus, currentStatus),
                Map.of("operation", operation, "currentStatus", currentStatus, "requiredStatus", requiredStatus));
    }
}
