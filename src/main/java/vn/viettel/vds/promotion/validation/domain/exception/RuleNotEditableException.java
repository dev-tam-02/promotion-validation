package vn.viettel.vds.promotion.validation.domain.exception;

import com.promix.platform.core.exception.ConflictException;

/**
 * Exception thrown when attempting to edit a rule that is currently assigned to a
 * campaign (has an active {@code rule_bindings} row). Per SRS VRUL003, an assigned
 * rule is locked from editing.
 * <p>
 * Error code {@code VALIDATION_RULE_NOT_EDITABLE}, HTTP 409 Conflict.
 */
public class RuleNotEditableException extends ConflictException {

    private static final String ERROR_CODE = "VALIDATION_RULE_NOT_EDITABLE";

    public RuleNotEditableException(String ruleId) {
        super(ERROR_CODE,
                "Rule is assigned to a campaign and cannot be edited. ruleId=" + ruleId,
                "ValidationRule", "id", ruleId);
    }
}
