package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * JPA entity for assignments mapped to assignments table.
 * <p>
 * IMPORTANT: This entity does NOT extend BaseEntity because the schema does NOT have
 * created_by, updated_by, or version columns that BaseEntity provides.
 * <p>
 * Schema columns (from 001-create-validation-rule-engine-schema.yaml:1215-1257):
 * - id: varchar(36) - Primary key
 * - entity_type: varchar(100) - Entity type
 * - entity_id: varchar(100) - Entity identifier
 * - rule_id: varchar(100) - Rule identifier
 * - priority: int - Assignment priority
 * - active: boolean - Whether assignment is active
 * - created_at: timestamp - Creation timestamp
 * - updated_at: timestamp - Last update timestamp
 * <p>
 * NOTE: Schema does NOT have tenant_id, subject_type, subject_key, assignment_version,
 * valid_from, valid_to, traffic_percent, or sticky_key_strategy columns
 */
@Getter
@Setter
@Entity
@Table(name = "assignments", indexes = {
        @Index(name = "idx_assignments_entity", columnList = "entity_type, entity_id")
})
public class AssignmentEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "entity_type", length = 100)
    private String entityType;

    @Column(name = "entity_id", length = 100)
    private String entityId;

    @Column(name = "rule_id", length = 100)
    private String ruleId;

    @Column(name = "priority")
    private Integer priority;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // One-to-many relationship with temporal links (ADDED after migration 013)
    // Temporal policies are assignment-specific, not rule-specific
    @OneToMany(mappedBy = "assignment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private java.util.List<RuleTemporalLinkEntity> temporalLinks = new java.util.ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = java.util.UUID.randomUUID().toString();
        }
        if (active == null) {
            active = true;
        }
    }
}
