package vn.viettel.vds.promotion.validation.adapter.out.persistence.mongo.adapter;

import com.promix.platform.autoconfigure.mongo.condition.ConditionalOnPromixMongo;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.mongo.repository.RuleTemporalLinkRepository;
import vn.viettel.vds.promotion.validation.application.port.out.RuleTemporalLinkPersistencePort;
import vn.viettel.vds.promotion.validation.domain.entity.RuleTemporalLink;

import java.util.List;
import java.util.Optional;

@Component
@ConditionalOnPromixMongo
public class RuleTemporalLinkMongoAdapter implements RuleTemporalLinkPersistencePort {

    private final RuleTemporalLinkRepository repository;

    public RuleTemporalLinkMongoAdapter(RuleTemporalLinkRepository repository) {
        this.repository = repository;
    }

    @Override
    public RuleTemporalLink save(RuleTemporalLink ruleTemporalLink) {
        return repository.save(ruleTemporalLink);
    }

    @Override
    public Optional<RuleTemporalLink> findById(String id) {
        return repository.findById(id);
    }

    @Override
    public List<RuleTemporalLink> findByRuleId(String ruleId) {
        return repository.findByRuleId(ruleId);
    }

    @Override
    public List<RuleTemporalLink> findByPolicyId(String policyId) {
        return repository.findByPolicyId(policyId);
    }

    @Override
    public Optional<RuleTemporalLink> findByRuleIdAndPolicyId(String ruleId, String policyId) {
        return repository.findByRuleIdAndPolicyId(ruleId, policyId);
    }

    @Override
    public void deleteByRuleIdAndPolicyId(String ruleId, String policyId) {
        repository.deleteByRuleIdAndPolicyId(ruleId, policyId);
    }

    @Override
    public boolean existsByRuleIdAndPolicyId(String ruleId, String policyId) {
        return repository.existsByRuleIdAndPolicyId(ruleId, policyId);
    }

    @Override
    public long countByRuleId(String ruleId) {
        return repository.countByRuleId(ruleId);
    }

    @Override
    public void delete(RuleTemporalLink ruleTemporalLink) {
        repository.delete(ruleTemporalLink);
    }

    @Override
    public void deleteById(String id) {
        repository.deleteById(id);
    }
}
