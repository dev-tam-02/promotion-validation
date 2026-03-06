package vn.viettel.vds.promotion.validation.domain.exception;

import com.promix.platform.core.exception.InternalException;

/**
 * Exception thrown when there is an error calculating operator fingerprint.
 */
public class OperatorFingerprintException extends InternalException {

    private static final String ERROR_CODE = "OPERATOR_FINGERPRINT_ERROR";

    public OperatorFingerprintException(String message) {
        super(ERROR_CODE, message);
    }

    public OperatorFingerprintException(String message, Throwable cause) {
        super(ERROR_CODE, message, cause);
    }
}
