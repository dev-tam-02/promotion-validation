package vn.viettel.vds.promotion.validation.domain.exception;

import com.promix.platform.core.exception.BusinessRuleException;

import java.util.Map;

/**
 * Exception thrown when attempting to modify a rule in a non-editable state.
 * HTTP Status: 422 Unprocessable Entity
 */
public class RuleStateNotEditableException extends BusinessRuleException {

    private static final String ERROR_CODE = "RULE_STATE_NOT_EDITABLE";

    public RuleStateNotEditableException(String currentStatus) {
        super(ERROR_CODE,
                String.format("Rule cannot be modified in status: %s", currentStatus),
                Map.of("currentStatus", currentStatus));
    }

    public RuleStateNotEditableException(String ruleId, String currentStatus) {
        super(ERROR_CODE,
                String.format("Rule '%s' cannot be modified in status: %s", ruleId, currentStatus),
                Map.of("ruleId", ruleId, "currentStatus", currentStatus));
    }
}
