package vn.viettel.vds.promotion.validation.application.port.out;

import vn.viettel.vds.promotion.validation.domain.model.RuleTemporalLink;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port for RuleTemporalLink persistence operations.
 *
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

    void delete(RuleTemporalLink ruleTemporalLink);

    void deleteById(String id);
}
