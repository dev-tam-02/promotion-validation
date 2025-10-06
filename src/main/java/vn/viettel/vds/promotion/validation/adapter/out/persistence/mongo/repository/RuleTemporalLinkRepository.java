package vn.viettel.vds.promotion.validation.adapter.out.persistence.mongo.repository;

import com.promix.platform.autoconfigure.mongo.condition.ConditionalOnPromixMongo;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import vn.viettel.vds.promotion.validation.domain.entity.RuleTemporalLink;

import java.util.List;
import java.util.Optional;

@ConditionalOnPromixMongo
@Repository
public interface RuleTemporalLinkRepository extends MongoRepository<RuleTemporalLink, String> {

    /**
     * Find temporal links by rule ID
     */
    List<RuleTemporalLink> findByRuleId(String ruleId);

    /**
     * Find temporal links by policy ID
     */
    List<RuleTemporalLink> findByPolicyId(String policyId);

    /**
     * Find specific link by rule and policy
     */
    Optional<RuleTemporalLink> findByRuleIdAndPolicyId(String ruleId, String policyId);

    /**
     * Delete temporal link by rule and policy
     */
    void deleteByRuleIdAndPolicyId(String ruleId, String policyId);

    /**
     * Check if temporal link exists
     */
    boolean existsByRuleIdAndPolicyId(String ruleId, String policyId);

    /**
     * Count temporal links for a rule
     */
    long countByRuleId(String ruleId);
}