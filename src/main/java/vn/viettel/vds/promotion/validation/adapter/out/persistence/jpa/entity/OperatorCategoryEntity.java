package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA entity for operator categories mapped to operator_categories table.
 * Categories group operators together for UI display (e.g., Audience, Products).
 */
@Getter
@Setter
@Entity
@Table(name = "operator_categories", indexes = {
        @Index(name = "idx_operator_categories_display_order", columnList = "display_order"),
        @Index(name = "idx_operator_categories_active", columnList = "is_active")
})
public class OperatorCategoryEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "code", length = 50, nullable = false, unique = true)
    private String code;

    // name / description (all locales) are overlaid from the translations table in
    // OperatorCategoryJpaAdapter (changelog 083) — the inline operator_categories
    // i18n columns were dropped.

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Column(name = "icon", length = 50)
    private String icon;

    @Column(name = "is_metadata_category", nullable = false)
    private Boolean metadataCategory = false;

    @Column(name = "metadata_schema_type", length = 50)
    private String metadataSchemaType;

    @Column(name = "metadata_schema_id", length = 64)
    private String metadataSchemaId;

    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OperatorOptionEntity> options = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = java.util.UUID.randomUUID().toString();
        }
        if (displayOrder == null) {
            displayOrder = 0;
        }
        if (metadataCategory == null) {
            metadataCategory = false;
        }
        if (active == null) {
            active = true;
        }
    }
}
