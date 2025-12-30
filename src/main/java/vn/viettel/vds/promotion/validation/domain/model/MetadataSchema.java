package vn.viettel.vds.promotion.validation.domain.model;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;

/**
 * Domain model representing a metadata schema field definition.
 * Metadata schemas define dynamic fields for customer, order, redemption, and custom event metadata.
 */
@Value
@Builder(toBuilder = true)
public class MetadataSchema {
    String id;
    String tenantId;
    SchemaType schemaType;
    String fieldKey;
    String fieldName;
    FieldType fieldType;
    List<String> availableValues;
    boolean required;
    Integer displayOrder;
    boolean active;

    // Audit fields
    Instant createdAt;
    Instant updatedAt;

    /**
     * Type of metadata schema.
     */
    public enum SchemaType {
        CUSTOMER,
        ORDER,
        REDEMPTION,
        CUSTOM_EVENT
    }

    /**
     * Data type of the metadata field.
     */
    public enum FieldType {
        STRING,
        NUMBER,
        BOOLEAN,
        DATE
    }
}
