package vn.viettel.vds.promotion.validation.domain.exception;

import com.promix.platform.core.exception.BadRequestException;

import java.util.Map;

/**
 * Exception thrown when schema type is invalid.
 * HTTP Status: 400 Bad Request
 */
public class InvalidSchemaTypeException extends BadRequestException {

    private static final String ERROR_CODE = "INVALID_SCHEMA_TYPE";

    public InvalidSchemaTypeException(String schemaType) {
        super(ERROR_CODE, "Invalid schema type: " + schemaType, Map.of("schemaType", schemaType));
    }
}
