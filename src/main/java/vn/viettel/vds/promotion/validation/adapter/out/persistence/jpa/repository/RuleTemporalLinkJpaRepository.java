package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.RuleTemporalLinkEntity;

import java.util.List;
import java.util.Optional;

/**
 * Repository for RuleTemporalLinkEntity
 *
 * IMPORTANT: After migration 013, rule_temporal_links now links to assignments, not validation_rules
 * All queries have been updated to use assignment.id instead of validationRule.id
 */
@Repository
public interface RuleTemporalLinkJpaRepository extends JpaRepository<RuleTemporalLinkEntity, String> {

    // UPDATED: Changed from validationRule.id to assignment.id
    @Query("SELECT rtl FROM RuleTemporalLinkEntity rtl WHERE rtl.assignment.id = :assignmentId")
    List<RuleTemporalLinkEntity> findByAssignmentId(@Param("assignmentId") String assignmentId);

    @Query("SELECT rtl FROM RuleTemporalLinkEntity rtl WHERE rtl.temporalPolicy.id = :policyId")
    List<RuleTemporalLinkEntity> findByTemporalPolicyId(@Param("policyId") String policyId);

    // UPDATED: Changed from validationRule.id to assignment.id
    @Query("SELECT rtl FROM RuleTemporalLinkEntity rtl WHERE rtl.assignment.id = :assignmentId AND rtl.temporalPolicy.id = :policyId")
    Optional<RuleTemporalLinkEntity> findByAssignmentIdAndTemporalPolicyId(@Param("assignmentId") String assignmentId, @Param("policyId") String policyId);

    // UPDATED: Changed from validationRule.id to assignment.id
    @Query("DELETE FROM RuleTemporalLinkEntity rtl WHERE rtl.assignment.id = :assignmentId AND rtl.temporalPolicy.id = :policyId")
    void deleteByAssignmentIdAndTemporalPolicyId(@Param("assignmentId") String assignmentId, @Param("policyId") String policyId);

    // UPDATED: Changed from validationRule.id to assignment.id
    @Query("SELECT CASE WHEN COUNT(rtl) > 0 THEN true ELSE false END FROM RuleTemporalLinkEntity rtl WHERE rtl.assignment.id = :assignmentId AND rtl.temporalPolicy.id = :policyId")
    boolean existsByAssignmentIdAndTemporalPolicyId(@Param("assignmentId") String assignmentId, @Param("policyId") String policyId);

    // UPDATED: Changed from validationRule.id to assignment.id
    @Query("SELECT COUNT(rtl) FROM RuleTemporalLinkEntity rtl WHERE rtl.assignment.id = :assignmentId")
    long countByAssignmentId(@Param("assignmentId") String assignmentId);

    /**
     * Find temporal policies by entity type and entity ID through assignments.
     * This query joins: rule_temporal_links -> assignments -> temporal_policies
     * to get all temporal policies associated with a specific object (entity).
     *
     * @param entityType The type of the entity (e.g., "CAMPAIGN", "DISCOUNT")
     * @param entityId The ID of the entity
     * @return List of RuleTemporalLinkEntity objects with temporal policies loaded
     */
    @Query("SELECT rtl FROM RuleTemporalLinkEntity rtl " +
            "JOIN FETCH rtl.temporalPolicy " +
            "WHERE rtl.assignment.entityType = :entityType " +
            "AND rtl.assignment.entityId = :entityId")
    List<RuleTemporalLinkEntity> findTemporalPoliciesByEntityTypeAndEntityId(
            @Param("entityType") String entityType,
            @Param("entityId") String entityId
    );

    /**
     * Find distinct entity IDs by entity type and time range.
     * This query filters entities (campaigns/discounts) by their effectiveTimeFrom (tp.startTs).
     *
     * Filter logic:
     * - If startTs provided: only return entities where tp.startTs >= startTs
     * - If endTs provided: only return entities where tp.startTs <= endTs
     * - If both provided: return entities where startTs <= tp.startTs <= endTs
     *
     * @param entityType The type of the entity (e.g., "CASHBACK", "DISCOUNT_COUPON")
     * @param startTs Start timestamp of the query range (nullable) - filters by effectiveTimeFrom >= startTs
     * @param endTs End timestamp of the query range (nullable) - filters by effectiveTimeFrom <= endTs
     * @return List of distinct entity IDs that match the criteria
     */
    @Query("SELECT DISTINCT rtl.assignment.entityId FROM RuleTemporalLinkEntity rtl " +
            "JOIN rtl.temporalPolicy tp " +
            "WHERE rtl.assignment.entityType = :entityType " +
            "AND rtl.assignment.active = true " +
            "AND (" +
            "    (:startTs IS NULL AND :endTs IS NULL) " +
            "    OR (:startTs IS NULL AND :endTs IS NOT NULL AND tp.startTs <= :endTs) " +
            "    OR (:startTs IS NOT NULL AND :endTs IS NULL AND tp.startTs >= :startTs) " +
            "    OR (:startTs IS NOT NULL AND :endTs IS NOT NULL AND tp.startTs >= :startTs AND tp.startTs <= :endTs)" +
            ")")
    List<String> findEntityIdsByEntityTypeAndTimeRange(
            @Param("entityType") String entityType,
            @Param("startTs") java.time.Instant startTs,
            @Param("endTs") java.time.Instant endTs
    );
}
