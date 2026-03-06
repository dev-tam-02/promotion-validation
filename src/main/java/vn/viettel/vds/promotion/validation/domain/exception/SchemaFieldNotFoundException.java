package vn.viettel.vds.promotion.validation.domain.exception;

import com.promix.platform.core.exception.ResourceNotFoundException;

/**
 * Exception thrown when schema field is not found.
 * HTTP Status: 404 Not Found
 */
public class SchemaFieldNotFoundException extends ResourceNotFoundException {

    private static final String ERROR_CODE = "SCHEMA_FIELD_NOT_FOUND";

    public SchemaFieldNotFoundException(String schemaType, String fieldKey) {
        super(ERROR_CODE, "SchemaField", schemaType + "." + fieldKey);
    }
}
