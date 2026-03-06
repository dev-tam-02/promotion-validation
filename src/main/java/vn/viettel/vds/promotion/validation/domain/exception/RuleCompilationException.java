package vn.viettel.vds.promotion.validation.domain.exception;

import com.promix.platform.core.exception.InternalException;

/**
 * Exception thrown when rule compilation fails.
 * HTTP Status: 500 Internal Server Error
 */
public class RuleCompilationException extends InternalException {

    private static final String ERROR_CODE = "RULE_COMPILATION_FAILED";

    public RuleCompilationException(String message) {
        super(ERROR_CODE, message);
    }

    public RuleCompilationException(String message, Throwable cause) {
        super(ERROR_CODE, message, cause);
    }
}
