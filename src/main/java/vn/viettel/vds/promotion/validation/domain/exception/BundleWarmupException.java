package vn.viettel.vds.promotion.validation.domain.exception;

import com.promix.platform.core.exception.BusinessRuleException;

/**
 * Exception thrown when bundle warmup fails.
 * HTTP Status: 422 Unprocessable Entity
 */
public class BundleWarmupException extends BusinessRuleException {

    private static final String ERROR_CODE = "BUNDLE_WARMUP_FAILED";

    public BundleWarmupException(String message) {
        super(ERROR_CODE, message);
    }

    public BundleWarmupException(String message, Throwable cause) {
        super(ERROR_CODE, message, cause);
    }
}
