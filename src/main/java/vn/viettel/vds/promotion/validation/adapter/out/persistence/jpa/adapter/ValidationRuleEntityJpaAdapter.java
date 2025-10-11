package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.adapter;

import com.promix.platform.data.jpa.autoconfigure.condition.ConditionalOnPromixJpa;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.ValidationRuleEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.ValidationRuleJpaRepository;
import vn.viettel.vds.promotion.validation.application.port.out.ValidationRuleEntityPersistencePort;
import vn.viettel.vds.promotion.validation.domain.model.Rule;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * JPA adapter implementation for ValidationRuleEntityPersistencePort
 * Handles conversion between Rule domain model and ValidationRuleEntity JPA
 */
@Component
@ConditionalOnPromixJpa
public class ValidationRuleEntityJpaAdapter implements ValidationRuleEntityPersistencePort {

    private final ValidationRuleJpaRepository repository;

    public ValidationRuleEntityJpaAdapter(ValidationRuleJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Rule save(Rule rule) {
        ValidationRuleEntity entity = toJpaEntity(rule);
        ValidationRuleEntity saved = repository.save(entity);
        return toDomainModel(saved);
    }

    @Override
    public Optional<Rule> findById(String id) {
        return repository.findById(id)
                .map(this::toDomainModel);
    }

    @Override
    public Optional<Rule> findByCode(String code) {
        return repository.findByCode(code)
                .map(this::toDomainModel);
    }

    @Override
    public List<Rule> findByState(String state) {
        return repository.findByState(state).stream()
                .map(this::toDomainModel)
                .collect(Collectors.toList());
    }

    @Override
    public Page<Rule> findByState(String state, Pageable pageable) {
        List<ValidationRuleEntity> entities = repository.findByState(state);
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), entities.size());

        List<Rule> pageContent = entities.subList(start, end).stream()
                .map(this::toDomainModel)
                .collect(Collectors.toList());

        return new PageImpl<>(pageContent, pageable, entities.size());
    }

    @Override
    public List<Rule> findByStateAndVersionGreaterThan(String state, Integer version) {
        return repository.findByState(state).stream()
                .filter(e -> e.getRuleVersion() > version)
                .map(this::toDomainModel)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByCode(String code) {
        return repository.existsByCode(code);
    }

    @Override
    public Optional<Rule> findTopByCodeOrderByVersionDesc(String code) {
        return repository.findLatestVersionByCode(code)
                .map(this::toDomainModel);
    }

    @Override
    public List<Rule> findByStateOrderByVersionDesc(String state) {
        return repository.findByStateOrderByVersionDesc(state).stream()
                .map(this::toDomainModel)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(String id) {
        repository.deleteById(id);
    }

    @Override
    public boolean existsById(String id) {
        return repository.existsById(id);
    }

    /**
     * Convert Rule domain model to JPA entity
     */
    private ValidationRuleEntity toJpaEntity(Rule domainModel) {
        ValidationRuleEntity jpaEntity = new ValidationRuleEntity();
        jpaEntity.setId(domainModel.getId());
        jpaEntity.setCode(domainModel.getCode());
        jpaEntity.setName(domainModel.getName());
        jpaEntity.setState(domainModel.getState() != null ? domainModel.getState().name() : "DRAFT");
        jpaEntity.setRuleVersion(domainModel.getRuleVersion() != null ? domainModel.getRuleVersion() : 1L);
        jpaEntity.setLogic(domainModel.getLogic() != null ? domainModel.getLogic().name() : null);
        jpaEntity.setDsl(domainModel.getDsl());
        jpaEntity.setPublishedAt(domainModel.getPublishedAt());
        jpaEntity.setPublishedBy(domainModel.getPublishedBy());
        jpaEntity.setCreatedAt(domainModel.getCreatedAt());
        jpaEntity.setCreatedBy(domainModel.getCreatedBy());
        jpaEntity.setUpdatedAt(domainModel.getUpdatedAt());
        return jpaEntity;
    }

    /**
     * Convert JPA entity to Rule domain model
     */
    private Rule toDomainModel(ValidationRuleEntity jpaEntity) {
        return Rule.builder()
                .id(jpaEntity.getId())
                .code(jpaEntity.getCode())
                .name(jpaEntity.getName())
                .state(jpaEntity.getState() != null ? Rule.RuleState.valueOf(jpaEntity.getState()) : Rule.RuleState.DRAFT)
                .ruleVersion(jpaEntity.getRuleVersion())
                .logic(jpaEntity.getLogic() != null ? Rule.LogicType.valueOf(jpaEntity.getLogic()) : null)
                .dsl(jpaEntity.getDsl())
                .publishedAt(jpaEntity.getPublishedAt())
                .publishedBy(jpaEntity.getPublishedBy())
                .createdAt(jpaEntity.getCreatedAt())
                .createdBy(jpaEntity.getCreatedBy())
                .updatedAt(jpaEntity.getUpdatedAt())
                .build();
    }
}
