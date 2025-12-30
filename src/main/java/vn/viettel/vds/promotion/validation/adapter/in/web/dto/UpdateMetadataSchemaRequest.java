package vn.viettel.vds.promotion.validation.adapter.in.web.dto;

import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request DTO for updating a metadata schema field.
 * All fields are optional - only provided fields will be updated.
 */
public record UpdateMetadataSchemaRequest(
        @Size(max = 100, message = "Field name must not exceed 100 characters")
        String fieldName,

        FieldType fieldType,

        List<String> availableValues,

        Boolean required,

        Integer displayOrder
) {
    public enum FieldType {
        STRING, NUMBER, BOOLEAN, DATE
    }
}
