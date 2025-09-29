package vn.viettel.vds.promotion.validation.adapter.out.persistence.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import vn.viettel.vds.promotion.validation.domain.entity.RuleTemporalLink;

import java.util.List;
import java.util.Optional;

@Repository
public interface RuleTemporalLinkRepository extends MongoRepository<RuleTemporalLink, String> {

    /**
     * Find temporal links by tenant and rule ID
     */
    List<RuleTemporalLink> findByTenantIdAndRuleId(String tenantId, String ruleId);

    /**
     * Find temporal links by tenant and policy ID
     */
    List<RuleTemporalLink> findByTenantIdAndPolicyId(String tenantId, String policyId);

    /**
     * Find specific link by tenant, rule and policy
     */
    Optional<RuleTemporalLink> findByTenantIdAndRuleIdAndPolicyId(String tenantId, String ruleId, String policyId);

    /**
     * Delete temporal link by tenant, rule and policy
     */
    void deleteByTenantIdAndRuleIdAndPolicyId(String tenantId, String ruleId, String policyId);

    /**
     * Check if temporal link exists
     */
    boolean existsByTenantIdAndRuleIdAndPolicyId(String tenantId, String ruleId, String policyId);

    /**
     * Count temporal links for a rule
     */
    long countByTenantIdAndRuleId(String tenantId, String ruleId);
}