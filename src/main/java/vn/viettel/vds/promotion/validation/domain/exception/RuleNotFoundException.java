package vn.viettel.vds.promotion.validation.domain.exception;

import com.promix.platform.core.exception.ResourceNotFoundException;

/**
 * Exception thrown when a validation rule is not found.
 * HTTP Status: 404 Not Found
 */
public class RuleNotFoundException extends ResourceNotFoundException {

    private static final String ERROR_CODE = "VALIDATION_RULE_NOT_FOUND";

    public RuleNotFoundException(String ruleId) {
        super(ERROR_CODE, "ValidationRule", ruleId);
    }

    /**
     * Not-found with an explicit user-facing message. The 3-arg
     * {@code ResourceNotFoundException(code, resourceType, resourceId)} constructor
     * auto-builds an English "not found: &lt;uuid&gt;" message that
     * {@code BaseExceptionHandler} leaks verbatim to the CMS toast (it prefers the
     * exception's own non-blank message over the {@code error_messages.properties}
     * bundle). Callers that must surface an SRS-mandated Vietnamese toast pass the
     * copy here so it is what the client sees. (PROM-1229)
     */
    public RuleNotFoundException(String ruleId, String userMessage) {
        super(ERROR_CODE, "ValidationRule", ruleId, userMessage);
    }

    public RuleNotFoundException(String field, String value) {
        super(ERROR_CODE, "ValidationRule", value,
                String.format("Rule not found with %s: %s", field, value));
    }
}