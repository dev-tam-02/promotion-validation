package vn.viettel.vds.promotion.validation.adapter.in.web.dto;

import vn.viettel.vds.promotion.validation.domain.model.MetadataSchema;

import java.time.Instant;
import java.util.List;

/**
 * Response DTO for metadata schema.
 */
public record MetadataSchemaResponse(
        String id,
        String tenantId,
        String schemaType,
        String fieldKey,
        String fieldName,
        String fieldType,
        List<String> availableValues,
        boolean required,
        Integer displayOrder,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
    /**
     * Create response from domain model.
     */
    public static MetadataSchemaResponse from(MetadataSchema schema) {
        return new MetadataSchemaResponse(
                schema.getId(),
                schema.getTenantId(),
                schema.getSchemaType() != null ? schema.getSchemaType().name().toLowerCase() : null,
                schema.getFieldKey(),
                schema.getFieldName(),
                schema.getFieldType() != null ? schema.getFieldType().name().toLowerCase() : null,
                schema.getAvailableValues(),
                schema.isRequired(),
                schema.getDisplayOrder(),
                schema.isActive(),
                schema.getCreatedAt(),
                schema.getUpdatedAt()
        );
    }
}
