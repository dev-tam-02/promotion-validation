package vn.viettel.vds.promotion.validation.domain.exception;

import com.promix.platform.core.exception.BusinessRuleException;

/**
 * Exception thrown when rule execution fails.
 * HTTP Status: 422 Unprocessable Entity
 */
public class RuleExecutionException extends BusinessRuleException {

    private static final String ERROR_CODE = "RULE_EXECUTION_FAILED";

    public RuleExecutionException(String message) {
        super(ERROR_CODE, message);
    }

    public RuleExecutionException(String message, Throwable cause) {
        super(ERROR_CODE, message, cause);
    }
}
