package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.adapter;

import com.promix.platform.data.jpa.autoconfigure.condition.ConditionalOnPromixJpa;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.RuleTemporalLinkEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.mapper.RuleTemporalLinkEntityMapper;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.RuleTemporalLinkJpaRepository;
import vn.viettel.vds.promotion.validation.application.port.out.RuleTemporalLinkPersistencePort;
import vn.viettel.vds.promotion.validation.domain.model.RuleTemporalLink;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@ConditionalOnPromixJpa
public class RuleTemporalLinkJpaAdapter implements RuleTemporalLinkPersistencePort {

    private final RuleTemporalLinkJpaRepository repository;
    private final RuleTemporalLinkEntityMapper mapper;

    public RuleTemporalLinkJpaAdapter(RuleTemporalLinkJpaRepository repository,
                                      RuleTemporalLinkEntityMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public RuleTemporalLink save(RuleTemporalLink ruleTemporalLink) {
        RuleTemporalLinkEntity entity = mapper.toEntity(ruleTemporalLink);
        RuleTemporalLinkEntity saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<RuleTemporalLink> findById(String id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<RuleTemporalLink> findByRuleId(String ruleId) {
        return repository.findByValidationRule_Id(ruleId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<RuleTemporalLink> findByPolicyId(String policyId) {
        return repository.findByTemporalPolicy_Id(policyId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<RuleTemporalLink> findByRuleIdAndPolicyId(String ruleId, String policyId) {
        return repository.findByValidationRule_IdAndTemporalPolicy_Id(ruleId, policyId)
                .map(mapper::toDomain);
    }

    @Override
    public void deleteByRuleIdAndPolicyId(String ruleId, String policyId) {
        repository.deleteByValidationRule_IdAndTemporalPolicy_Id(ruleId, policyId);
    }

    @Override
    public boolean existsByRuleIdAndPolicyId(String ruleId, String policyId) {
        return repository.existsByValidationRule_IdAndTemporalPolicy_Id(ruleId, policyId);
    }

    @Override
    public long countByRuleId(String ruleId) {
        return repository.countByValidationRule_Id(ruleId);
    }

    @Override
    public void delete(RuleTemporalLink ruleTemporalLink) {
        repository.deleteById(ruleTemporalLink.getId());
    }

    @Override
    public void deleteById(String id) {
        repository.deleteById(id);
    }
}
