package vn.viettel.vds.promotion.validation.domain.exception;

import com.promix.platform.core.exception.BusinessRuleException;

import java.util.Map;

/**
 * Exception thrown when a rule binding cannot be redeployed: it is inactive, or it
 * carries neither a rule, a temporal policy nor an applicability scope — there is
 * nothing to compile into a bundle.
 * <p>
 * HTTP Status: 422 Unprocessable Entity
 */
public class BindingNotRedeployableException extends BusinessRuleException {

    private static final String ERROR_CODE = "BINDING_NOT_REDEPLOYABLE";

    public BindingNotRedeployableException(String bindingId, String reason) {
        super(ERROR_CODE,
                String.format("Binding %s cannot be redeployed: %s", bindingId, reason),
                Map.of("bindingId", bindingId, "reason", reason));
    }
}
