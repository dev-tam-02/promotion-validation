package vn.viettel.vds.promotion.validation.application.port.out;

import vn.viettel.vds.promotion.validation.domain.model.RuleTemporalLink;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port for RuleTemporalLink persistence operations.
 * <p>
 * IMPORTANT: After migration 013, temporal links are assignment-specific, not rule-specific
 * Methods have been updated to use assignmentId instead of ruleId
 */
public interface RuleTemporalLinkPersistencePort {

    RuleTemporalLink save(RuleTemporalLink ruleTemporalLink);

    Optional<RuleTemporalLink> findById(String id);

    // UPDATED: Changed from findByRuleId to findByAssignmentId
    List<RuleTemporalLink> findByAssignmentId(String assignmentId);

    List<RuleTemporalLink> findByPolicyId(String policyId);

    // UPDATED: Changed from findByRuleIdAndPolicyId to findByAssignmentIdAndPolicyId
    Optional<RuleTemporalLink> findByAssignmentIdAndPolicyId(String assignmentId, String policyId);

    // UPDATED: Changed from deleteByRuleIdAndPolicyId to deleteByAssignmentIdAndPolicyId
    void deleteByAssignmentIdAndPolicyId(String assignmentId, String policyId);

    // UPDATED: Changed from existsByRuleIdAndPolicyId to existsByAssignmentIdAndPolicyId
    boolean existsByAssignmentIdAndPolicyId(String assignmentId, String policyId);

    // UPDATED: Changed from countByRuleId to countByAssignmentId
    long countByAssignmentId(String assignmentId);

    /**
     * Delete all temporal links by assignment ID.
     * Used for restore operations.
     */
    void deleteByAssignmentId(String assignmentId);

    void delete(RuleTemporalLink ruleTemporalLink);

    void deleteById(String id);

    /**
     * Find temporal links by entity type and entity ID.
     * This method retrieves all temporal policy links associated with a specific entity/object.
     *
     * @param entityType The type of the entity (e.g., "CAMPAIGN", "DISCOUNT")
     * @param entityId   The ID of the entity
     * @return List of RuleTemporalLink objects with temporal policies
     */
    List<RuleTemporalLink> findByEntityTypeAndEntityId(String entityType, String entityId);

    /**
     * Find entity IDs by entity type and time range.
     * Returns distinct entity IDs that have temporal policies overlapping with the specified time range.
     *
     * @param entityType The type of the entity (e.g., "CASHBACK", "DISCOUNT_COUPON")
     * @param startTs    Start timestamp of the query range (nullable)
     * @param endTs      End timestamp of the query range (nullable)
     * @return List of distinct entity IDs
     */
    List<String> findEntityIdsByEntityTypeAndTimeRange(String entityType, java.time.Instant startTs, java.time.Instant endTs);

    /**
     * Find temporal links by assignment ID with embedded TemporalPolicy data.
     * This method eagerly loads the associated temporal policy for each link,
     * avoiding N+1 query issues when full policy details are needed.
     *
     * @param assignmentId The assignment ID to search for
     * @return List of RuleTemporalLink objects with temporalPolicy field populated
     */
    List<RuleTemporalLink> findByAssignmentIdWithPolicy(String assignmentId);
}
