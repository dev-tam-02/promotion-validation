package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.adapter;

import com.promix.platform.data.jpa.autoconfigure.condition.ConditionalOnPromixJpa;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.RuleTemporalLinkEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.mapper.RuleTemporalLinkMapper;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.RuleTemporalLinkJpaRepository;
import vn.viettel.vds.promotion.validation.application.port.out.RuleTemporalLinkPersistencePort;
import vn.viettel.vds.promotion.validation.domain.model.RuleTemporalLink;

import java.util.List;
import java.util.Optional;

@Component
@ConditionalOnPromixJpa
public class RuleTemporalLinkJpaAdapter implements RuleTemporalLinkPersistencePort {

    private final RuleTemporalLinkJpaRepository repository;
    private final RuleTemporalLinkMapper mapper;

    public RuleTemporalLinkJpaAdapter(RuleTemporalLinkJpaRepository repository,
                                      RuleTemporalLinkMapper mapper) {
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
    public List<RuleTemporalLink> findByAssignmentId(String assignmentId) {
        return repository.findByAssignmentId(assignmentId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<RuleTemporalLink> findByPolicyId(String policyId) {
        return repository.findByTemporalPolicyId(policyId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<RuleTemporalLink> findByAssignmentIdAndPolicyId(String assignmentId, String policyId) {
        return repository.findByAssignmentIdAndTemporalPolicyId(assignmentId, policyId)
                .map(mapper::toDomain);
    }

    @Override
    public void deleteByAssignmentIdAndPolicyId(String assignmentId, String policyId) {
        repository.deleteByAssignmentIdAndTemporalPolicyId(assignmentId, policyId);
    }

    @Override
    public boolean existsByAssignmentIdAndPolicyId(String assignmentId, String policyId) {
        return repository.existsByAssignmentIdAndTemporalPolicyId(assignmentId, policyId);
    }

    @Override
    public long countByAssignmentId(String assignmentId) {
        return repository.countByAssignmentId(assignmentId);
    }

    @Override
    public void delete(RuleTemporalLink ruleTemporalLink) {
        repository.deleteById(ruleTemporalLink.getId());
    }

    @Override
    public void deleteById(String id) {
        repository.deleteById(id);
    }

    @Override
    public List<RuleTemporalLink> findByEntityTypeAndEntityId(String entityType, String entityId) {
        return repository.findTemporalPoliciesByEntityTypeAndEntityId(entityType, entityId).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
