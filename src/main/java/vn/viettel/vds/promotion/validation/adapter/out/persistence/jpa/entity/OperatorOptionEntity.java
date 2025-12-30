package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * JPA entity for operator options mapped to operator_options table.
 * Options define individual operators within a category.
 */
@Getter
@Setter
@Entity
@Table(name = "operator_options", indexes = {
        @Index(name = "idx_operator_options_category_id", columnList = "category_id"),
        @Index(name = "idx_operator_options_display_order", columnList = "display_order"),
        @Index(name = "idx_operator_options_operator_name", columnList = "operator_name"),
        @Index(name = "idx_operator_options_active", columnList = "is_active")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_operator_options_category_code", columnNames = {"category_id", "code"})
})
public class OperatorOptionEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private OperatorCategoryEntity category;

    @Column(name = "code", length = 50, nullable = false)
    private String code;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "operator_name", length = 100)
    private String operatorName;

    @Column(name = "operator_version")
    private Integer operatorVersion = 1;

    @Column(name = "comparison_type", length = 20)
    private String comparisonType;

    @Column(name = "available_comparators", columnDefinition = "TEXT")
    private String availableComparators;

    @Column(name = "default_comparator", length = 20)
    private String defaultComparator;

    @Column(name = "value_type", length = 20)
    private String valueType;

    @Column(name = "value_source", length = 20)
    private String valueSource;

    @Column(name = "value_options", columnDefinition = "TEXT")
    private String valueOptions;

    @Column(name = "min_value", precision = 19, scale = 4)
    private BigDecimal minValue;

    @Column(name = "max_value", precision = 19, scale = 4)
    private BigDecimal maxValue;

    @Column(name = "pattern", length = 255)
    private String pattern;

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
        if (displayOrder == null) {
            displayOrder = 0;
        }
        if (operatorVersion == null) {
            operatorVersion = 1;
        }
        if (active == null) {
            active = true;
        }
    }
}
