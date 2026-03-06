package vn.viettel.vds.promotion.validation.domain.exception;

import com.promix.platform.core.exception.ConflictException;

/**
 * Exception thrown when attempting to create a reason code that already exists.
 */
public class ReasonCodeAlreadyExistsException extends ConflictException {

    private static final String ERROR_CODE = "REASON_CODE_ALREADY_EXISTS";

    public ReasonCodeAlreadyExistsException(String reasonCodeId, String tenantId) {
        super(ERROR_CODE, String.format("Reason code '%s' already exists for tenant %s",
                reasonCodeId, tenantId));
    }

    public ReasonCodeAlreadyExistsException(String reasonCodeId) {
        super(ERROR_CODE, String.format("Reason code '%s' already exists", reasonCodeId));
    }
}
