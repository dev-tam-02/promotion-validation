package vn.viettel.vds.promotion.validation.domain.exception;

/**
 * Exception thrown when rule compilation fails
 */
public class RuleCompilationException extends DomainException {

    private static final String ERROR_CODE = "RULE_COMPILATION_FAILED";

    public RuleCompilationException(String message) {
        super(message, ERROR_CODE);
    }

    public RuleCompilationException(String message, Throwable cause) {
        super(message, ERROR_CODE, cause);
    }
}
