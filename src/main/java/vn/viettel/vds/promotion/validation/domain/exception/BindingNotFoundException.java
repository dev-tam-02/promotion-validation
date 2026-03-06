package vn.viettel.vds.promotion.validation.domain.exception;

import com.promix.platform.core.exception.ResourceNotFoundException;

/**
 * Exception thrown when a binding is not found.
 */
public class BindingNotFoundException extends ResourceNotFoundException {

    private static final String ERROR_CODE = "BINDING_NOT_FOUND";

    public BindingNotFoundException(String bindingId) {
        super(ERROR_CODE, "Binding", bindingId);
    }

    public BindingNotFoundException(String objectType, String objectId, String ruleId) {
        super(ERROR_CODE, "Binding", objectType + "/" + objectId + "/" + ruleId,
                String.format("Binding not found for object %s/%s and rule %s",
                        objectType, objectId, ruleId));
    }

    public BindingNotFoundException(String objectType, String objectId) {
        super(ERROR_CODE, "Binding", objectType + "/" + objectId,
                String.format("Binding not found for object %s/%s", objectType, objectId));
    }
}
