package vn.viettel.vds.promotion.validation.domain.exception;

/**
 * Exception thrown when bundle warmup fails
 */
public class BundleWarmupException extends DomainException {

    private static final String ERROR_CODE = "BUNDLE_WARMUP_FAILED";

    public BundleWarmupException(String message) {
        super(message, ERROR_CODE);
    }

    public BundleWarmupException(String message, Throwable cause) {
        super(message, ERROR_CODE, cause);
    }
}
