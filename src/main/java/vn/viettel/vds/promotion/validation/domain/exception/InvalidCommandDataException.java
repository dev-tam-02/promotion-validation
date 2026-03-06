package vn.viettel.vds.promotion.validation.domain.exception;

import com.promix.platform.core.exception.BadRequestException;

import java.util.Map;

/**
 * Exception thrown when command data is invalid.
 * HTTP Status: 400 Bad Request
 */
public class InvalidCommandDataException extends BadRequestException {

    private static final String ERROR_CODE = "INVALID_COMMAND_DATA";

    public InvalidCommandDataException(String message) {
        super(ERROR_CODE, message);
    }

    public InvalidCommandDataException(String field, String message) {
        super(ERROR_CODE, message, Map.of("field", field));
    }

    public InvalidCommandDataException(String commandType, String field, String message) {
        super(ERROR_CODE, message, Map.of("commandType", commandType, "field", field));
    }
}
