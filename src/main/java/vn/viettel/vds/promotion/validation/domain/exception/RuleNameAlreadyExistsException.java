package vn.viettel.vds.promotion.validation.domain.exception;

import com.promix.platform.core.exception.ConflictException;

/**
 * Exception thrown when attempting to create (or rename) a rule with a name that
 * already exists. Name uniqueness is enforced system-wide, on the {@code name}
 * column alone, after trimming leading/trailing whitespace (SRS VRUL002_B03).
 * <p>
 * Error code {@code VALIDATION_RULE_DUPLICATE_NAME} is what the CMS create flow
 * catches to bounce the user back to Step 1 with an inline error on the name field.
 * HTTP Status: 409 Conflict.
 */
public class RuleNameAlreadyExistsException extends ConflictException {

    private static final String ERROR_CODE = "VALIDATION_RULE_DUPLICATE_NAME";

    public RuleNameAlreadyExistsException(String name) {
        super(ERROR_CODE, "ValidationRule", "name", name);
    }
}
