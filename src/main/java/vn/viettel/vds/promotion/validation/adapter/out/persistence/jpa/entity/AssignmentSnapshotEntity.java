package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Entity for storing assignment snapshots for saga compensation.
 * <p>
 * This entity stores a complete JSON snapshot of the assignment aggregate
 * (assignment + applicability rules + temporal links) before an update operation.
 * <p>
 * When a saga needs to compensate/revert, it can restore the aggregate
 * from this snapshot using the targetVersion.
 */
@Entity
@Table(name = "assignment_snapshots", indexes = {
        @Index(name = "idx_assignment_snapshot_version", columnList = "assignment_id, snapshot_version", unique = true),
        @Index(name = "idx_assignment_snapshot_saga_id", columnList = "saga_id"),
        @Index(name = "idx_assignment_snapshot_created_at", columnList = "created_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentSnapshotEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "assignment_id", nullable = false, length = 36)
    private String assignmentId;

    @Column(name = "snapshot_version", nullable = false)
    private Long snapshotVersion;

    @Column(name = "snapshot_data", columnDefinition = "LONGTEXT", nullable = false)
    private String snapshotData;

    @Column(name = "saga_id", length = 100)
    private String sagaId;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    @Column(name = "snapshot_reason", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private SnapshotReason snapshotReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    /**
     * Reason for creating the snapshot
     */
    public enum SnapshotReason {
        BEFORE_UPDATE,
        BEFORE_DELETE,
        BEFORE_DISABLE,
        MANUAL
    }
}
