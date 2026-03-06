package vn.viettel.vds.promotion.validation.domain.exception;

import com.promix.platform.core.exception.ExternalServiceException;

/**
 * Exception thrown when rule publishing operations fail.
 * HTTP Status: 502 Bad Gateway
 */
public class RulePublishingException extends ExternalServiceException {

    private static final String ERROR_CODE = "RULE_PUBLISHING_FAILED";
    private static final String SERVICE_NAME = "rule-publisher";

    public RulePublishingException(String message) {
        super(ERROR_CODE, message);
    }

    public RulePublishingException(String message, Throwable cause) {
        super(ERROR_CODE, SERVICE_NAME, message, cause);
    }
}
