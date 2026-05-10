package vn.viettel.vds.promotion.validation.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request DTO for creating a metadata schema field.
 */
public record CreateMetadataSchemaRequest(
        @NotBlank(message = "Field key is required")
        @Size(max = 100, message = "Field key must not exceed 100 characters")
        @Pattern(regexp = "^[a-zA-Z_]\\w*$", message = "Field key must be a valid identifier")
        String fieldKey,

        @NotBlank(message = "Field name is required")
        @Size(max = 100, message = "Field name must not exceed 100 characters")
        String fieldName,

        @NotNull(message = "Field type is required")
        FieldType fieldType,

        List<String> availableValues,

        Boolean required
) {
    public enum FieldType {
        STRING, NUMBER, BOOLEAN, DATE
    }
}
