package vn.viettel.vds.promotion.validation.domain.model;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;

/**
 * Domain model representing an operator category for UI configuration.
 * Categories group related operators together (e.g., Audience, Products, Prices & Quantities).
 */
@Value
@Builder(toBuilder = true)
public class OperatorCategory {
    String id;
    String code;
    String name;
    Integer displayOrder;
    String icon;
    String description;

    // I18n fields
    String nameVi;
    String descriptionVi;

    boolean metadataCategory;
    String metadataSchemaType;
    String metadataSchemaId;
    boolean active;

    // Nested options (loaded when needed)
    List<OperatorOption> options;

    // Audit fields
    Instant createdAt;
    Instant updatedAt;

    /**
     * Available metadata schema types for metadata categories.
     */
    public enum MetadataSchemaType {
        CUSTOMER,
        ORDER,
        REDEMPTION,
        CUSTOM_EVENT
    }
}
