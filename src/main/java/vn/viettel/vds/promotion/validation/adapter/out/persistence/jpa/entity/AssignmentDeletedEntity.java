package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * JPA entity for deleted assignments mapped to validation_rules_assignment_deleted table.
 * <p>
 * This table stores soft-deleted assignment records for audit purposes.
 * According to SRS PRM_KBNV_API_VALD008:
 * - When an assignment is deleted, insert a record here
 * - Store deleted_at (timestamp), deleted_by (user who deleted)
 */
@Getter
@Setter
@Entity
@Table(name = "validation_rules_assignment_deleted", indexes = {
        @Index(name = "idx_assignment_deleted_rule", columnList = "rule_id"),
        @Index(name = "idx_assignment_deleted_entity", columnList = "entity_type, entity_id"),
        @Index(name = "idx_assignment_deleted_at", columnList = "deleted_at")
})
public class AssignmentDeletedEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    /**
     * Original assignment ID from assignments table.
     */
    @Column(name = "original_assignment_id", length = 36)
    private String originalAssignmentId;

    /**
     * Entity type (e.g., CAMPAIGN, VOUCHER).
     */
    @Column(name = "entity_type", length = 100)
    private String entityType;

    /**
     * Entity identifier (object_id from SRS).
     */
    @Column(name = "entity_id", length = 100)
    private String entityId;

    /**
     * Rule identifier (validation_rule_id from SRS).
     */
    @Column(name = "rule_id", length = 100)
    private String ruleId;

    /**
     * Assignment priority.
     */
    @Column(name = "priority")
    private Integer priority;

    /**
     * Was this assignment active before deletion.
     */
    @Column(name = "was_active")
    private Boolean wasActive;

    /**
     * Temporal bundle hash at time of deletion.
     */
    @Column(name = "temporal_bundle_hash", length = 255)
    private String temporalBundleHash;

    /**
     * Was included_all flag set.
     */
    @Column(name = "included_all")
    private Boolean includedAll;

    /**
     * Original creation timestamp.
     */
    @Column(name = "original_created_at")
    private Instant originalCreatedAt;

    /**
     * Original last update timestamp.
     */
    @Column(name = "original_updated_at")
    private Instant originalUpdatedAt;

    /**
     * Timestamp when this assignment was deleted.
     * Format: DD/MM/YYYY HH:MM:SS as per SRS.
     */
    @Column(name = "deleted_at", nullable = false)
    private Instant deletedAt;

    /**
     * User who deleted this assignment.
     */
    @Column(name = "deleted_by", length = 100)
    private String deletedBy;

    /**
     * Version number (incremented from original on delete).
     */
    @Column(name = "version")
    private Integer version;

    /**
     * Create deleted entity from original assignment.
     */
    public static AssignmentDeletedEntity fromAssignment(AssignmentEntity assignment, String deletedBy, int newVersion) {
        AssignmentDeletedEntity deleted = new AssignmentDeletedEntity();
        deleted.setOriginalAssignmentId(assignment.getId());
        deleted.setEntityType(assignment.getEntityType());
        deleted.setEntityId(assignment.getEntityId());
        deleted.setRuleId(assignment.getRuleId());
        deleted.setPriority(assignment.getPriority());
        deleted.setWasActive(assignment.getActive());
        deleted.setTemporalBundleHash(assignment.getTemporalBundleHash());
        deleted.setIncludedAll(assignment.getIncludedAll());
        deleted.setOriginalCreatedAt(assignment.getCreatedAt());
        deleted.setOriginalUpdatedAt(assignment.getUpdatedAt());
        deleted.setDeletedAt(Instant.now());
        deleted.setDeletedBy(deletedBy);
        deleted.setVersion(newVersion);
        return deleted;
    }

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = java.util.UUID.randomUUID().toString();
        }
        if (deletedAt == null) {
            deletedAt = Instant.now();
        }
    }
}
