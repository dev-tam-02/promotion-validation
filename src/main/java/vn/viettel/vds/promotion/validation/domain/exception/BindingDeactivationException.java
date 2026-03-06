package vn.viettel.vds.promotion.validation.domain.exception;

import com.promix.platform.core.exception.InternalException;

/**
 * Exception thrown when binding deactivation fails.
 */
public class BindingDeactivationException extends InternalException {

    private static final String ERROR_CODE = "BINDING_DEACTIVATION_FAILED";

    public BindingDeactivationException(String bindingId) {
        super(ERROR_CODE, "Failed to deactivate binding: " + bindingId);
    }

    public BindingDeactivationException(String bindingId, Throwable cause) {
        super(ERROR_CODE, "Failed to deactivate binding: " + bindingId, cause);
    }
}
