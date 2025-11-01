package vn.viettel.vds.promotion.validation.domain.exception;

/**
 * Exception thrown when rule execution fails
 */
public class RuleExecutionException extends DomainException {

    private static final String ERROR_CODE = "RULE_EXECUTION_FAILED";

    public RuleExecutionException(String message) {
        super(message, ERROR_CODE);
    }

    public RuleExecutionException(String message, Throwable cause) {
        super(message, ERROR_CODE, cause);
    }
}
