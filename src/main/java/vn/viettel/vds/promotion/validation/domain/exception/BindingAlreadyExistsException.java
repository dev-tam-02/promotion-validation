package vn.viettel.vds.promotion.validation.domain.exception;

import com.promix.platform.core.exception.ConflictException;

/**
 * Exception thrown when attempting to create a binding that already exists.
 */
public class BindingAlreadyExistsException extends ConflictException {

    private static final String ERROR_CODE = "BINDING_ALREADY_EXISTS";

    public BindingAlreadyExistsException(String objectType, String objectId, String ruleId) {
        super(ERROR_CODE, String.format("A binding already exists for object %s/%s and rule %s",
                objectType, objectId, ruleId));
    }

    public BindingAlreadyExistsException(String message) {
        super(ERROR_CODE, message);
    }
}
