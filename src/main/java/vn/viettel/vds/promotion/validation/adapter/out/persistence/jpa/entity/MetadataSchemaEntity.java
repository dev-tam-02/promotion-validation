package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * JPA entity for metadata schemas mapped to metadata_schemas table.
 * Defines dynamic fields for customer, order, redemption, and custom event metadata.
 */
@Getter
@Setter
@Entity
@Table(name = "metadata_schemas", indexes = {
        @Index(name = "idx_metadata_schemas_tenant_type", columnList = "tenant_id, schema_type"),
        @Index(name = "idx_metadata_schemas_display_order", columnList = "display_order"),
        @Index(name = "idx_metadata_schemas_active", columnList = "is_active")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_metadata_schemas_tenant_type_key", columnNames = {"tenant_id", "schema_type", "field_key"})
})
public class MetadataSchemaEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "tenant_id", length = 50, nullable = false)
    private String tenantId;

    @Column(name = "schema_type", length = 50, nullable = false)
    private String schemaType;

    @Column(name = "field_key", length = 100, nullable = false)
    private String fieldKey;

    @Column(name = "field_name", length = 100, nullable = false)
    private String fieldName;

    @Column(name = "field_type", length = 20, nullable = false)
    private String fieldType;

    @Column(name = "available_values", columnDefinition = "TEXT")
    private String availableValues;

    @Column(name = "is_required", nullable = false)
    private Boolean required = false;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = java.util.UUID.randomUUID().toString();
        }
        if (required == null) {
            required = false;
        }
        if (displayOrder == null) {
            displayOrder = 0;
        }
        if (active == null) {
            active = true;
        }
    }
}
