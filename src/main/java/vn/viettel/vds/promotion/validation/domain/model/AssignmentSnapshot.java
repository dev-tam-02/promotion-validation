package vn.viettel.vds.promotion.validation.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Domain model for storing assignment snapshots for saga compensation.
 * <p>
 * This model stores a complete JSON snapshot of the assignment aggregate
 * (assignment + applicability rules + temporal links) before an update operation.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentSnapshot {
    private String id;
    private String assignmentId;
    private Long snapshotVersion;
    private String snapshotData;
    private String sagaId;
    private String correlationId;
    private SnapshotReason snapshotReason;
    private Instant createdAt;
    private String createdBy;
    private Instant expiresAt;

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
