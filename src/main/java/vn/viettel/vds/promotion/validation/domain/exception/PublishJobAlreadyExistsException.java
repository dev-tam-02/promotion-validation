package vn.viettel.vds.promotion.validation.domain.exception;

import com.promix.platform.core.exception.ConflictException;

/**
 * Exception thrown when a publish job already exists for a rule version.
 */
public class PublishJobAlreadyExistsException extends ConflictException {

    private static final String ERROR_CODE = "PUBLISH_JOB_ALREADY_EXISTS";

    public PublishJobAlreadyExistsException(String ruleId, Integer version) {
        super(ERROR_CODE, String.format("Publish job already exists for rule %s version %d",
                ruleId, version));
    }

    public PublishJobAlreadyExistsException(String message) {
        super(ERROR_CODE, message);
    }
}
