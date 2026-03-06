package vn.viettel.vds.promotion.validation.domain.exception;

import com.promix.platform.core.exception.ConflictException;

/**
 * Exception thrown when schema field already exists.
 * HTTP Status: 409 Conflict
 */
public class SchemaFieldAlreadyExistsException extends ConflictException {

    private static final String ERROR_CODE = "SCHEMA_FIELD_ALREADY_EXISTS";

    public SchemaFieldAlreadyExistsException(String schemaType, String fieldKey) {
        super(ERROR_CODE, "SchemaField", "fieldKey", schemaType + "." + fieldKey);
    }
}
