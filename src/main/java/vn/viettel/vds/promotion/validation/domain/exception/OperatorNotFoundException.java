package vn.viettel.vds.promotion.validation.domain.exception;

import com.promix.platform.core.exception.ResourceNotFoundException;

/**
 * Exception thrown when an operator is not found.
 */
public class OperatorNotFoundException extends ResourceNotFoundException {

    private static final String ERROR_CODE = "OPERATOR_NOT_FOUND";

    public OperatorNotFoundException(String operatorName, Integer version) {
        super(ERROR_CODE, "Operator", operatorName + "@" + version);
    }

    public OperatorNotFoundException(String operatorId) {
        super(ERROR_CODE, "Operator", operatorId);
    }
}
