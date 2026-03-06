package vn.viettel.vds.promotion.validation.domain.exception;

import com.promix.platform.core.exception.BusinessRuleException;
import vn.viettel.vds.promotion.validation.domain.model.RuleStatus;

import java.util.Map;

/**
 * Exception thrown when a rule operation is invalid for its current state.
 * HTTP Status: 422 Unprocessable Entity
 */
public class InvalidRuleStateException extends BusinessRuleException {

    private static final String ERROR_CODE = "INVALID_RULE_STATE";

    public InvalidRuleStateException(String operation, RuleStatus currentStatus) {
        super(ERROR_CODE,
                String.format("Operation '%s' is not allowed for rule in status: %s", operation, currentStatus),
                Map.of("operation", operation, "currentStatus", currentStatus.name()));
    }

    public InvalidRuleStateException(String operation, RuleStatus currentStatus, RuleStatus requiredStatus) {
        super(ERROR_CODE,
                String.format("Operation '%s' requires rule to be in status %s, but current status is %s",
                        operation, requiredStatus, currentStatus),
                Map.of("operation", operation, "currentStatus", currentStatus.name(), "requiredStatus", requiredStatus.name()));
    }
}