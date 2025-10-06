package vn.viettel.vds.promotion.validation.application.port.out;

import vn.viettel.vds.promotion.validation.domain.model.RuleTemporalLink;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port for RuleTemporalLink persistence operations.
 */
public interface RuleTemporalLinkPersistencePort {

    RuleTemporalLink save(RuleTemporalLink ruleTemporalLink);

    Optional<RuleTemporalLink> findById(String id);

    List<RuleTemporalLink> findByRuleId(String ruleId);

    List<RuleTemporalLink> findByPolicyId(String policyId);

    Optional<RuleTemporalLink> findByRuleIdAndPolicyId(String ruleId, String policyId);

    void deleteByRuleIdAndPolicyId(String ruleId, String policyId);

    boolean existsByRuleIdAndPolicyId(String ruleId, String policyId);

    long countByRuleId(String ruleId);

    void delete(RuleTemporalLink ruleTemporalLink);

    void deleteById(String id);
}
