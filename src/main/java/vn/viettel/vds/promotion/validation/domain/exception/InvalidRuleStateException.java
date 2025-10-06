package vn.viettel.vds.promotion.validation.domain.exception;

import vn.viettel.vds.promotion.validation.domain.model.RuleStatus;

/**
 * Exception thrown when a rule operation is invalid for its current state
 */
public class InvalidRuleStateException extends DomainException {

    public InvalidRuleStateException(String operation, RuleStatus currentStatus) {
        super(String.format("Operation '%s' is not allowed for rule in status: %s",
                        operation, currentStatus),
                "INVALID_RULE_STATE");
    }

    public InvalidRuleStateException(String operation, RuleStatus currentStatus, RuleStatus requiredStatus) {
        super(String.format("Operation '%s' requires rule to be in status %s, but current status is %s",
                        operation, requiredStatus, currentStatus),
                "INVALID_RULE_STATE");
    }
}