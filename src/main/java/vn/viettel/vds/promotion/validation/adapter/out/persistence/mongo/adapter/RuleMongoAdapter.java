package vn.viettel.vds.promotion.validation.adapter.out.persistence.mongo.adapter;

import com.promix.platform.autoconfigure.mongo.condition.ConditionalOnPromixMongo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.mongo.repository.RuleRepository;
import vn.viettel.vds.promotion.validation.application.port.out.RulePersistencePort;
import vn.viettel.vds.promotion.validation.domain.entity.Rule;

import java.util.List;
import java.util.Optional;

@Component
@ConditionalOnPromixMongo
public class RuleMongoAdapter implements RulePersistencePort {

    private final RuleRepository repository;

    public RuleMongoAdapter(RuleRepository repository) {
        this.repository = repository;
    }

    @Override
    public Rule save(Rule rule) {
        return repository.save(rule);
    }

    @Override
    public Optional<Rule> findById(String id) {
        return repository.findById(id);
    }

    @Override
    public Optional<Rule> findByTenantIdAndCode(String tenantId, String code) {
        return repository.findByTenantIdAndCode(tenantId, code);
    }

    @Override
    public Page<Rule> findByTenantIdAndState(String tenantId, Rule.RuleState state, Pageable pageable) {
        return repository.findByTenantIdAndState(tenantId, state, pageable);
    }

    @Override
    public Page<Rule> findByTenantIdWithFilters(String tenantId, Rule.RuleState state,
                                                String codePattern, String namePattern, Pageable pageable) {
        return repository.findByTenantIdWithFilters(tenantId, state, codePattern, namePattern, pageable);
    }

    @Override
    public List<Rule> findByTenantIdOrderByUpdatedAtDesc(String tenantId) {
        return repository.findByTenantIdOrderByUpdatedAtDesc(tenantId);
    }

    @Override
    public boolean existsByTenantIdAndCode(String tenantId, String code) {
        return repository.existsByTenantIdAndCode(tenantId, code);
    }

    @Override
    public long countByTenantIdAndState(String tenantId, Rule.RuleState state) {
        return repository.countByTenantIdAndState(tenantId, state);
    }

    @Override
    public List<Rule> findByTenantIdAndStateNot(String tenantId, Rule.RuleState state) {
        return repository.findByTenantIdAndStateNot(tenantId, state);
    }

    @Override
    public List<Rule> findByState(Rule.RuleState state) {
        return repository.findByState(state);
    }

    @Override
    public List<Rule> findByType(String type) {
        return repository.findByType(type);
    }

    @Override
    public List<Rule> findByRuleSetId(String ruleSetId) {
        return repository.findByRuleSetId(ruleSetId);
    }

    @Override
    public List<Rule> findByCampaignId(String campaignId) {
        return repository.findByCampaignId(campaignId);
    }

    @Override
    public List<Rule> findByPriorityBetween(int minPriority, int maxPriority) {
        return repository.findByPriorityBetween(minPriority, maxPriority);
    }

    @Override
    public List<Rule> findByTargetSegmentsContaining(String segment) {
        return repository.findByTargetSegmentsContaining(segment);
    }

    @Override
    public Page<Rule> findAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    @Override
    public boolean existsById(String id) {
        return repository.existsById(id);
    }

    @Override
    public void delete(Rule rule) {
        repository.delete(rule);
    }

    @Override
    public void deleteById(String id) {
        repository.deleteById(id);
    }
}
