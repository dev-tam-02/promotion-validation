package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.AssignmentDeletedEntity;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for deleted assignment records.
 * Used for audit trail and recovery of deleted assignments.
 */
@Repository
public interface AssignmentDeletedJpaRepository extends JpaRepository<AssignmentDeletedEntity, String> {

    /**
     * Find deleted assignments by original assignment ID.
     */
    List<AssignmentDeletedEntity> findByOriginalAssignmentId(String originalAssignmentId);

    /**
     * Find deleted assignments by rule ID.
     */
    List<AssignmentDeletedEntity> findByRuleId(String ruleId);

    /**
     * Find deleted assignments by entity type and entity ID.
     */
    @Query("SELECT a FROM AssignmentDeletedEntity a WHERE a.entityType = :entityType AND a.entityId = :entityId")
    List<AssignmentDeletedEntity> findByEntityTypeAndEntityId(
            @Param("entityType") String entityType,
            @Param("entityId") String entityId);

    /**
     * Find deleted assignments by rule ID and entity ID.
     */
    @Query("SELECT a FROM AssignmentDeletedEntity a WHERE a.ruleId = :ruleId AND a.entityId = :entityId")
    List<AssignmentDeletedEntity> findByRuleIdAndEntityId(
            @Param("ruleId") String ruleId,
            @Param("entityId") String entityId);

    /**
     * Find deleted assignments within a time range.
     */
    @Query("SELECT a FROM AssignmentDeletedEntity a WHERE a.deletedAt BETWEEN :startTime AND :endTime ORDER BY a.deletedAt DESC")
    List<AssignmentDeletedEntity> findByDeletedAtBetween(
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime);

    /**
     * Find deleted assignments by user who deleted.
     */
    List<AssignmentDeletedEntity> findByDeletedBy(String deletedBy);

    /**
     * Find the latest deleted assignment for a specific rule and entity combination.
     */
    @Query("SELECT a FROM AssignmentDeletedEntity a WHERE a.ruleId = :ruleId AND a.entityId = :entityId ORDER BY a.deletedAt DESC")
    List<AssignmentDeletedEntity> findLatestByRuleIdAndEntityId(
            @Param("ruleId") String ruleId,
            @Param("entityId") String entityId);

    /**
     * Check if a deletion record exists for a specific original assignment.
     */
    boolean existsByOriginalAssignmentId(String originalAssignmentId);
}
