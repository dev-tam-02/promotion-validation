package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.RuleBindingEntity;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * JPA Repository for RuleBindingEntity.
 * Provides database operations for rule bindings.
 */
@Repository
public interface RuleBindingJpaRepository extends JpaRepository<RuleBindingEntity, String> {

    // ========== Find by Target ==========

    /**
     * Find all bindings for a specific target
     */
    List<RuleBindingEntity> findByTargetTypeAndTargetId(String targetType, String targetId);

    /**
     * Find all active bindings for a specific target, ordered by priority
     */
    @Query("SELECT rb FROM RuleBindingEntity rb " +
            "WHERE rb.targetType = :targetType AND rb.targetId = :targetId AND rb.active = true " +
            "ORDER BY rb.priority DESC")
    List<RuleBindingEntity> findActiveByTarget(
            @Param("targetType") String targetType,
            @Param("targetId") String targetId);

    /**
     * Find all bindings for a target type
     */
    List<RuleBindingEntity> findByTargetType(String targetType);

    /**
     * Find all bindings for multiple target IDs of the same type
     */
    @Query("SELECT rb FROM RuleBindingEntity rb " +
            "WHERE rb.targetType = :targetType AND rb.targetId IN :targetIds")
    List<RuleBindingEntity> findByTargetTypeAndTargetIdIn(
            @Param("targetType") String targetType,
            @Param("targetIds") List<String> targetIds);

    // ========== Find by Rule ==========

    /**
     * Find all bindings for a specific rule
     */
    List<RuleBindingEntity> findByRuleId(String ruleId);

    /**
     * Find all active bindings for a specific rule
     */
    List<RuleBindingEntity> findByRuleIdAndActive(String ruleId, Boolean active);

    // ========== Find by Time Range ==========

    /**
     * Find bindings effective at a specific time
     */
    @Query("SELECT rb FROM RuleBindingEntity rb " +
            "WHERE rb.targetType = :targetType " +
            "AND rb.active = true " +
            "AND (rb.validFrom IS NULL OR rb.validFrom <= :timestamp) " +
            "AND (rb.validTo IS NULL OR rb.validTo >= :timestamp)")
    List<RuleBindingEntity> findEffectiveAtTime(
            @Param("targetType") String targetType,
            @Param("timestamp") Instant timestamp);

    /**
     * Find target IDs with bindings in a time range
     */
    @Query("SELECT DISTINCT rb.targetId FROM RuleBindingEntity rb " +
            "WHERE rb.targetType = :targetType " +
            "AND rb.active = true " +
            "AND ((:startTs IS NULL OR rb.validFrom IS NULL OR rb.validFrom <= :endTs) " +
            "     AND (:endTs IS NULL OR rb.validTo IS NULL OR rb.validTo >= :startTs))")
    List<String> findTargetIdsByTypeAndTimeRange(
            @Param("targetType") String targetType,
            @Param("startTs") Instant startTs,
            @Param("endTs") Instant endTs);

    // ========== Find with Pagination ==========

    /**
     * Find all bindings for a target type with pagination
     */
    Page<RuleBindingEntity> findByTargetType(String targetType, Pageable pageable);

    /**
     * Find all active bindings with pagination
     */
    Page<RuleBindingEntity> findByActive(Boolean active, Pageable pageable);

    /**
     * Search bindings by target type and optional filters
     */
    @Query("SELECT rb FROM RuleBindingEntity rb " +
            "WHERE (:targetType IS NULL OR rb.targetType = :targetType) " +
            "AND (:targetId IS NULL OR rb.targetId = :targetId) " +
            "AND (:ruleId IS NULL OR rb.ruleId = :ruleId) " +
            "AND (:active IS NULL OR rb.active = :active)")
    Page<RuleBindingEntity> searchBindings(
            @Param("targetType") String targetType,
            @Param("targetId") String targetId,
            @Param("ruleId") String ruleId,
            @Param("active") Boolean active,
            Pageable pageable);

    // ========== Find by Active Status ==========

    /**
     * Find all bindings by active status
     */
    List<RuleBindingEntity> findByActive(Boolean active);

    // ========== Existence Checks ==========

    /**
     * Check if a binding exists for a target and rule combination
     */
    boolean existsByTargetTypeAndTargetIdAndRuleId(String targetType, String targetId, String ruleId);

    /**
     * Check if any active binding exists for a target
     */
    boolean existsByTargetTypeAndTargetIdAndActive(String targetType, String targetId, Boolean active);

    // ========== Count Operations ==========

    /**
     * Count bindings for a target
     */
    long countByTargetTypeAndTargetId(String targetType, String targetId);

    /**
     * Count active bindings for a rule
     */
    long countByRuleIdAndActive(String ruleId, Boolean active);

    /**
     * Count bindings by active status
     */
    long countByActive(Boolean active);

    // ========== Delete Operations ==========

    /**
     * Delete all bindings for a target
     */
    @Modifying
    @Query("DELETE FROM RuleBindingEntity rb WHERE rb.targetType = :targetType AND rb.targetId = :targetId")
    int deleteByTarget(@Param("targetType") String targetType, @Param("targetId") String targetId);

    /**
     * Delete binding by target and rule
     */
    @Modifying
    @Query("DELETE FROM RuleBindingEntity rb " +
            "WHERE rb.targetType = :targetType AND rb.targetId = :targetId AND rb.ruleId = :ruleId")
    int deleteByTargetAndRule(
            @Param("targetType") String targetType,
            @Param("targetId") String targetId,
            @Param("ruleId") String ruleId);

    /**
     * Soft delete: deactivate binding
     */
    @Modifying
    @Query("UPDATE RuleBindingEntity rb SET rb.active = false, rb.updatedAt = CURRENT_TIMESTAMP, rb.updatedBy = :updatedBy " +
            "WHERE rb.id = :id")
    int deactivate(@Param("id") String id, @Param("updatedBy") String updatedBy);

    // ========== Find Single ==========

    /**
     * Find a specific binding by target and rule
     */
    Optional<RuleBindingEntity> findByTargetTypeAndTargetIdAndRuleId(
            String targetType, String targetId, String ruleId);

    /**
     * Find the highest priority active binding for a target
     */
    @Query("SELECT rb FROM RuleBindingEntity rb " +
            "WHERE rb.targetType = :targetType AND rb.targetId = :targetId AND rb.active = true " +
            "ORDER BY rb.priority DESC LIMIT 1")
    Optional<RuleBindingEntity> findTopActiveByTarget(
            @Param("targetType") String targetType,
            @Param("targetId") String targetId);
}
