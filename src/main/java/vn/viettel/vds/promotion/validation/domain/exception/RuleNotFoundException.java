package vn.viettel.vds.promotion.validation.domain.exception;

/**
 * Exception thrown when a rule is not found
 */
public class RuleNotFoundException extends DomainException {

    public RuleNotFoundException(String ruleId) {
        super("Rule not found with ID: " + ruleId, "RULE_NOT_FOUND");
    }

    public RuleNotFoundException(String field, String value) {
        super(String.format("Rule not found with %s: %s", field, value), "RULE_NOT_FOUND");
    }
}