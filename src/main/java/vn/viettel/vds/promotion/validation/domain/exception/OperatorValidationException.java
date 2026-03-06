package vn.viettel.vds.promotion.validation.domain.exception;

import com.promix.platform.core.exception.BadRequestException;

/**
 * Exception thrown when operator validation fails.
 */
public class OperatorValidationException extends BadRequestException {

    private static final String ERROR_CODE = "OPERATOR_VALIDATION_ERROR";

    public OperatorValidationException(String message) {
        super(ERROR_CODE, message);
    }

    public OperatorValidationException(String operatorName, Integer version, String reason) {
        super(ERROR_CODE, String.format("Operator '%s' version %d is not supported: %s",
                operatorName, version, reason));
    }
}
