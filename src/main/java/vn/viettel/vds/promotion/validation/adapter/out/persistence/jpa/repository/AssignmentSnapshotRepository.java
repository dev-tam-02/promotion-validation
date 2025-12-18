package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.AssignmentSnapshotEntity;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for managing assignment snapshots.
 * <p>
 * Used for saga compensation - storing and retrieving assignment snapshots
 * to enable version-based rollback during saga failures.
 */
@Repository
public interface AssignmentSnapshotRepository extends JpaRepository<AssignmentSnapshotEntity, String> {

    /**
     * Find snapshot by assignment ID and version number.
     */
    Optional<AssignmentSnapshotEntity> findByAssignmentIdAndSnapshotVersion(String assignmentId, Long snapshotVersion);

    /**
     * Find the latest snapshot for an assignment.
     */
    Optional<AssignmentSnapshotEntity> findFirstByAssignmentIdOrderBySnapshotVersionDesc(String assignmentId);

    /**
     * Find all snapshots for an assignment, ordered by version descending.
     */
    List<AssignmentSnapshotEntity> findByAssignmentIdOrderBySnapshotVersionDesc(String assignmentId);

    /**
     * Find snapshot by saga ID.
     */
    List<AssignmentSnapshotEntity> findBySagaId(String sagaId);

    /**
     * Check if a snapshot exists for a specific assignment and version.
     */
    boolean existsByAssignmentIdAndSnapshotVersion(String assignmentId, Long snapshotVersion);

    /**
     * Get the latest version number for an assignment.
     */
    @Query("SELECT MAX(s.snapshotVersion) FROM AssignmentSnapshotEntity s WHERE s.assignmentId = :assignmentId")
    Optional<Long> findLatestVersionByAssignmentId(@Param("assignmentId") String assignmentId);

    /**
     * Delete expired snapshots.
     */
    @Modifying
    @Query("DELETE FROM AssignmentSnapshotEntity s WHERE s.expiresAt IS NOT NULL AND s.expiresAt < :cutoffTime")
    int deleteExpiredSnapshots(@Param("cutoffTime") Instant cutoffTime);

    /**
     * Delete all snapshots older than a certain time.
     */
    @Modifying
    @Query("DELETE FROM AssignmentSnapshotEntity s WHERE s.createdAt < :cutoffTime")
    int deleteOlderThan(@Param("cutoffTime") Instant cutoffTime);
}
