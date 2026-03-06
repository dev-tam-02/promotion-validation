package vn.viettel.vds.promotion.validation.domain.exception;

import com.promix.platform.core.exception.ConflictException;

/**
 * Exception thrown when attempting to create a rule that already exists.
 * HTTP Status: 409 Conflict
 */
public class RuleAlreadyExistsException extends ConflictException {

    private static final String ERROR_CODE = "RULE_ALREADY_EXISTS";

    public RuleAlreadyExistsException(String code) {
        super(ERROR_CODE, "ValidationRule", "code", code);
    }

    public RuleAlreadyExistsException(String code, String tenantId) {
        super(ERROR_CODE,
                String.format("Rule already exists with code: %s for tenant: %s", code, tenantId),
                "ValidationRule", "code", code);
    }
}