package vn.viettel.vds.promotion.validation.domain.exception;

import com.promix.platform.core.exception.ResourceNotFoundException;

/**
 * Exception thrown when a reason code is not found.
 */
public class ReasonCodeNotFoundException extends ResourceNotFoundException {

    private static final String ERROR_CODE = "REASON_CODE_NOT_FOUND";

    public ReasonCodeNotFoundException(String reasonCodeId, String tenantId) {
        super(ERROR_CODE, "ReasonCode", reasonCodeId,
                String.format("Reason code '%s' not found for tenant %s", reasonCodeId, tenantId));
    }

    public ReasonCodeNotFoundException(String reasonCodeId) {
        super(ERROR_CODE, "ReasonCode", reasonCodeId);
    }
}
