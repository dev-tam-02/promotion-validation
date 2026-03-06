package vn.viettel.vds.promotion.validation.domain.exception;

import com.promix.platform.core.exception.BusinessRuleException;

import java.util.Map;

/**
 * Exception thrown when rule verification fails.
 * HTTP Status: 422 Unprocessable Entity
 */
public class RuleVerificationException extends BusinessRuleException {

    private static final String ERROR_CODE = "RULE_VERIFICATION_FAILED";

    public RuleVerificationException(String message) {
        super(ERROR_CODE, message);
    }

    public RuleVerificationException(String ruleId, String message) {
        super(ERROR_CODE, message, Map.of("ruleId", ruleId));
    }

    public RuleVerificationException(String message, Throwable cause) {
        super(ERROR_CODE, message, cause);
    }
}
