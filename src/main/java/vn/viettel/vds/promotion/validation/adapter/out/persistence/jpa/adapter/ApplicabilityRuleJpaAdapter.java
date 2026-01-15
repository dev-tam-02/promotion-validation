package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.adapter;

import com.promix.platform.data.jpa.autoconfigure.condition.ConditionalOnPromixJpa;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.AssignmentApplicabilityRuleEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.mapper.ApplicabilityRuleMapper;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.AssignmentApplicabilityRuleJpaRepository;
import vn.viettel.vds.promotion.validation.application.port.out.ApplicabilityRulePersistencePort;
import vn.viettel.vds.promotion.validation.domain.model.ApplicabilityRule;

import java.util.List;
import java.util.Optional;

/**
 * JPA adapter implementing ApplicabilityRulePersistencePort.
 * Converts between ApplicabilityRule domain model and JPA entities.
 */
@Component
@ConditionalOnPromixJpa
public class ApplicabilityRuleJpaAdapter implements ApplicabilityRulePersistencePort {

    private final AssignmentApplicabilityRuleJpaRepository repository;
    private final ApplicabilityRuleMapper mapper;

    public ApplicabilityRuleJpaAdapter(AssignmentApplicabilityRuleJpaRepository repository,
                                       ApplicabilityRuleMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public ApplicabilityRule save(ApplicabilityRule applicabilityRule) {
        AssignmentApplicabilityRuleEntity entity = mapper.toEntity(applicabilityRule);
        AssignmentApplicabilityRuleEntity saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public List<ApplicabilityRule> saveAll(List<ApplicabilityRule> applicabilityRules) {
        List<AssignmentApplicabilityRuleEntity> entities = mapper.toEntityList(applicabilityRules);
        List<AssignmentApplicabilityRuleEntity> saved = repository.saveAll(entities);
        return mapper.toDomainList(saved);
    }

    @Override
    public Optional<ApplicabilityRule> findById(String id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<ApplicabilityRule> findByAssignmentId(String assignmentId) {
        List<AssignmentApplicabilityRuleEntity> entities = repository.findByAssignmentId(assignmentId);
        return mapper.toDomainList(entities);
    }

    @Override
    public List<ApplicabilityRule> findByAssignmentIdAndRuleType(String assignmentId, ApplicabilityRule.RuleType ruleType) {
        List<AssignmentApplicabilityRuleEntity> entities =
                repository.findByAssignmentIdAndRuleType(assignmentId, ruleType.name());
        return mapper.toDomainList(entities);
    }

    @Override
    public void deleteByAssignmentId(String assignmentId) {
        repository.deleteByAssignmentId(assignmentId);
    }

    @Override
    public void delete(ApplicabilityRule applicabilityRule) {
        repository.deleteById(applicabilityRule.getId());
    }

    @Override
    public void deleteById(String id) {
        repository.deleteById(id);
    }

    @Override
    public long countByAssignmentId(String assignmentId) {
        return repository.findByAssignmentId(assignmentId).size();
    }
}
