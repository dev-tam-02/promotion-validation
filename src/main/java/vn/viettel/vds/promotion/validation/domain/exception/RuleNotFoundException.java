package vn.viettel.vds.promotion.validation.domain.exception;

import com.promix.platform.core.exception.ResourceNotFoundException;

/**
 * Exception thrown when a validation rule is not found.
 * HTTP Status: 404 Not Found
 */
public class RuleNotFoundException extends ResourceNotFoundException {

    private static final String ERROR_CODE = "VALIDATION_RULE_NOT_FOUND";
    private static final String RESOURCE_TYPE = "ValidationRule";

    public RuleNotFoundException(String ruleId) {
        super(ERROR_CODE, RESOURCE_TYPE, ruleId);
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
        super(ERROR_CODE, RESOURCE_TYPE, ruleId, userMessage);
    }

    /**
     * Not-found described by an arbitrary lookup field/value pair (e.g. object
     * type + id) instead of a rule id. Exposed as a static factory so it does
     * not clash with the {@code (ruleId, userMessage)} constructor above, which
     * shares the erased {@code (String, String)} signature.
     */
    public static RuleNotFoundException byField(String field, String value) {
        return new RuleNotFoundException(value,
                String.format("Rule not found with %s: %s", field, value));
    }
}