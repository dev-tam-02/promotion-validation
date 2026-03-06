package vn.viettel.vds.promotion.validation.domain.exception;

import com.promix.platform.core.exception.ConflictException;

/**
 * Exception thrown when attempting to create an operator that already exists.
 */
public class OperatorAlreadyExistsException extends ConflictException {

    private static final String ERROR_CODE = "OPERATOR_ALREADY_EXISTS";

    public OperatorAlreadyExistsException(String operatorName, Integer version) {
        super(ERROR_CODE, String.format("Operator %s@%d already exists", operatorName, version));
    }

    public OperatorAlreadyExistsException(String operatorName) {
        super(ERROR_CODE, String.format("Operator '%s' already exists", operatorName));
    }
}
