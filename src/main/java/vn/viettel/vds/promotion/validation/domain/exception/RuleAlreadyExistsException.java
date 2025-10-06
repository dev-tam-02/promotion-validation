package vn.viettel.vds.promotion.validation.domain.exception;

/**
 * Exception thrown when attempting to create a rule that already exists
 */
public class RuleAlreadyExistsException extends DomainException {

    public RuleAlreadyExistsException(String code) {
        super("Rule already exists with code: " + code, "RULE_ALREADY_EXISTS");
    }

    public RuleAlreadyExistsException(String code, String tenantId) {
        super(String.format("Rule already exists with code: %s for tenant: %s", code, tenantId),
                "RULE_ALREADY_EXISTS");
    }
}