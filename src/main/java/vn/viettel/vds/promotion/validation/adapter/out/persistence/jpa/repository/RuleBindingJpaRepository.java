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

    // ========== Find by Object ==========

    /**
     * Find all bindings for a specific object
     */
    List<RuleBindingEntity> findByObjectTypeAndObjectId(String objectType, String objectId);

    /**
     * Find all active bindings for a specific object, ordered by priority
     */
    @Query("SELECT rb FROM RuleBindingEntity rb " +
            "WHERE rb.objectType = :objectType AND rb.objectId = :objectId AND rb.active = true " +
            "ORDER BY rb.priority DESC")
    List<RuleBindingEntity> findActiveByObject(
            @Param("objectType") String objectType,
            @Param("objectId") String objectId);

    /**
     * Find all bindings for an object type
     */
    List<RuleBindingEntity> findByObjectType(String objectType);

    /**
     * Find all bindings for multiple object IDs of the same type
     */
    @Query("SELECT rb FROM RuleBindingEntity rb " +
            "WHERE rb.objectType = :objectType AND rb.objectId IN :objectIds")
    List<RuleBindingEntity> findByObjectTypeAndObjectIdIn(
            @Param("objectType") String objectType,
            @Param("objectIds") List<String> objectIds);

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
            "WHERE rb.objectType = :objectType " +
            "AND rb.active = true " +
            "AND (rb.validFrom IS NULL OR rb.validFrom <= :timestamp) " +
            "AND (rb.validTo IS NULL OR rb.validTo >= :timestamp)")
    List<RuleBindingEntity> findEffectiveAtTime(
            @Param("objectType") String objectType,
            @Param("timestamp") Instant timestamp);

    /**
     * Find object IDs with bindings in a time range
     */
    @Query("SELECT DISTINCT rb.objectId FROM RuleBindingEntity rb " +
            "WHERE rb.objectType = :objectType " +
            "AND rb.active = true " +
            "AND ((:startTs IS NULL OR rb.validFrom IS NULL OR rb.validFrom <= :endTs) " +
            "     AND (:endTs IS NULL OR rb.validTo IS NULL OR rb.validTo >= :startTs))")
    List<String> findObjectIdsByTypeAndTimeRange(
            @Param("objectType") String objectType,
            @Param("startTs") Instant startTs,
            @Param("endTs") Instant endTs);

    // ========== Find with Pagination ==========

    /**
     * Find all bindings for an object type with pagination
     */
    Page<RuleBindingEntity> findByObjectType(String objectType, Pageable pageable);

    /**
     * Find all active bindings with pagination
     */
    Page<RuleBindingEntity> findByActive(Boolean active, Pageable pageable);

    /**
     * Search bindings by object type and optional filters
     */
    @Query("SELECT rb FROM RuleBindingEntity rb " +
            "WHERE (:objectType IS NULL OR rb.objectType = :objectType) " +
            "AND (:objectId IS NULL OR rb.objectId = :objectId) " +
            "AND (:ruleId IS NULL OR rb.ruleId = :ruleId) " +
            "AND (:active IS NULL OR rb.active = :active)")
    Page<RuleBindingEntity> searchBindings(
            @Param("objectType") String objectType,
            @Param("objectId") String objectId,
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
     * Check if a binding exists for an object and rule combination
     */
    boolean existsByObjectTypeAndObjectIdAndRuleId(String objectType, String objectId, String ruleId);

    /**
     * Check if any active binding exists for an object
     */
    boolean existsByObjectTypeAndObjectIdAndActive(String objectType, String objectId, Boolean active);

    // ========== Count Operations ==========

    /**
     * Count bindings for an object
     */
    long countByObjectTypeAndObjectId(String objectType, String objectId);

    /**
     * Count active bindings for a rule
     */
    long countByRuleIdAndActive(String ruleId, Boolean active);

    /**
     * Count all bindings (active and inactive) for a rule
     */
    long countByRuleId(String ruleId);

    /**
     * Count bindings by active status
     */
    long countByActive(Boolean active);

    // ========== Delete Operations ==========

    /**
     * Delete all bindings for an object
     */
    @Modifying
    @Query("DELETE FROM RuleBindingEntity rb WHERE rb.objectType = :objectType AND rb.objectId = :objectId")
    int deleteByObject(@Param("objectType") String objectType, @Param("objectId") String objectId);

    /**
     * Delete all bindings for an object, matching objectType case-insensitively.
     * Covers stored rows regardless of case convention (CAMPAIGN / campaign / Campaign).
     */
    @Modifying
    @Query("DELETE FROM RuleBindingEntity rb WHERE LOWER(rb.objectType) = LOWER(:objectType) AND rb.objectId = :objectId")
    int deleteByObjectIgnoreCase(@Param("objectType") String objectType, @Param("objectId") String objectId);

    /**
     * Find all bindings for an object, matching objectType case-insensitively.
     */
    @Query("SELECT rb FROM RuleBindingEntity rb WHERE LOWER(rb.objectType) = LOWER(:objectType) AND rb.objectId = :objectId")
    List<RuleBindingEntity> findByObjectIgnoreCase(@Param("objectType") String objectType, @Param("objectId") String objectId);

    /**
     * Delete binding by object and rule
     */
    @Modifying
    @Query("DELETE FROM RuleBindingEntity rb " +
            "WHERE rb.objectType = :objectType AND rb.objectId = :objectId AND rb.ruleId = :ruleId")
    int deleteByObjectAndRule(
            @Param("objectType") String objectType,
            @Param("objectId") String objectId,
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
     * Find a specific binding by object and rule
     */
    Optional<RuleBindingEntity> findByObjectTypeAndObjectIdAndRuleId(
            String objectType, String objectId, String ruleId);

    /**
     * Find the highest priority active binding for an object
     */
    @Query("SELECT rb FROM RuleBindingEntity rb " +
            "WHERE rb.objectType = :objectType AND rb.objectId = :objectId AND rb.active = true " +
            "ORDER BY rb.priority DESC LIMIT 1")
    Optional<RuleBindingEntity> findTopActiveByObject(
            @Param("objectType") String objectType,
            @Param("objectId") String objectId);
}
