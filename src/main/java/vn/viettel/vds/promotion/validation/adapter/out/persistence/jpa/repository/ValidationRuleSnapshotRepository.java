package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.ValidationRuleSnapshotEntity;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for managing validation rule snapshots.
 * <p>
 * Used for saga compensation - storing and retrieving aggregate snapshots
 * to enable version-based rollback during saga failures.
 */
@Repository
public interface ValidationRuleSnapshotRepository extends JpaRepository<ValidationRuleSnapshotEntity, String> {

    /**
     * Find snapshot by rule ID and version number.
     * Used during revert operation to get the target version's state.
     *
     * @param validationRuleId the validation rule ID
     * @param version          the version number to restore
     * @return the snapshot if found
     */
    Optional<ValidationRuleSnapshotEntity> findByValidationRuleIdAndVersion(String validationRuleId, Long version);

    /**
     * Find the latest snapshot for a validation rule.
     * Useful for getting the most recent backup.
     *
     * @param validationRuleId the validation rule ID
     * @return the latest snapshot if found
     */
    Optional<ValidationRuleSnapshotEntity> findFirstByValidationRuleIdOrderByVersionDesc(String validationRuleId);

    /**
     * Find all snapshots for a validation rule, ordered by version descending.
     *
     * @param validationRuleId the validation rule ID
     * @return list of snapshots
     */
    List<ValidationRuleSnapshotEntity> findByValidationRuleIdOrderByVersionDesc(String validationRuleId);

    /**
     * Find snapshot by saga ID.
     * Used during compensation to find the snapshot created at saga start.
     *
     * @param sagaId the saga ID
     * @return list of snapshots created during the saga
     */
    List<ValidationRuleSnapshotEntity> findBySagaId(String sagaId);

    /**
     * Check if a snapshot exists for a specific rule and version.
     *
     * @param validationRuleId the validation rule ID
     * @param version          the version number
     * @return true if snapshot exists
     */
    boolean existsByValidationRuleIdAndVersion(String validationRuleId, Long version);

    /**
     * Delete expired snapshots.
     * For automatic cleanup of old snapshots.
     *
     * @param cutoffTime snapshots with expiresAt before this time will be deleted
     * @return number of deleted records
     */
    @Modifying
    @Query("DELETE FROM ValidationRuleSnapshotEntity s WHERE s.expiresAt IS NOT NULL AND s.expiresAt < :cutoffTime")
    int deleteExpiredSnapshots(@Param("cutoffTime") Instant cutoffTime);

    /**
     * Delete all snapshots older than a certain time.
     * For manual cleanup operations.
     *
     * @param cutoffTime snapshots created before this time will be deleted
     * @return number of deleted records
     */
    @Modifying
    @Query("DELETE FROM ValidationRuleSnapshotEntity s WHERE s.createdAt < :cutoffTime")
    int deleteOlderThan(@Param("cutoffTime") Instant cutoffTime);

    /**
     * Keep only the N most recent snapshots for each rule.
     * Useful for limiting storage while keeping recent backups.
     *
     * @param validationRuleId the validation rule ID
     * @param keepCount        number of snapshots to keep
     * @return number of deleted records
     */
    @Modifying
    @Query(value = """
            DELETE FROM validation_rule_snapshots
            WHERE validation_rule_id = :ruleId
            AND id NOT IN (
                SELECT id FROM (
                    SELECT id FROM validation_rule_snapshots
                    WHERE validation_rule_id = :ruleId
                    ORDER BY version DESC
                    LIMIT :keepCount
                ) AS keep_list
            )
            """, nativeQuery = true)
    int deleteOldSnapshots(@Param("ruleId") String validationRuleId, @Param("keepCount") int keepCount);
}
