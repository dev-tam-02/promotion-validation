package vn.viettel.vds.promotion.validation.application.port.out;

import vn.viettel.vds.promotion.validation.domain.model.AssignmentSnapshot;

import java.time.Instant;
import java.util.Optional;

/**
 * Outbound port for AssignmentSnapshot persistence operations.
 * <p>
 * Handles snapshot storage for saga compensation patterns.
 */
public interface AssignmentSnapshotPersistencePort {

    AssignmentSnapshot save(AssignmentSnapshot snapshot);

    Optional<AssignmentSnapshot> findById(String id);

    Optional<AssignmentSnapshot> findByAssignmentIdAndSnapshotVersion(String assignmentId, Long snapshotVersion);

    Optional<Long> findLatestVersionByAssignmentId(String assignmentId);

    boolean existsByAssignmentIdAndSnapshotVersion(String assignmentId, Long snapshotVersion);

    int deleteExpiredSnapshots(Instant expirationTime);

    void delete(AssignmentSnapshot snapshot);

    void deleteById(String id);
}
