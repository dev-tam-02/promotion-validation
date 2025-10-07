package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.adapter;

import com.promix.platform.data.jpa.autoconfigure.condition.ConditionalOnPromixJpa;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.ValidationRuleEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.ValidationRuleJpaRepository;
import vn.viettel.vds.promotion.validation.application.port.out.ValidationRuleEntityPersistencePort;
import vn.viettel.vds.promotion.validation.domain.model.ValidationRule;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * JPA adapter implementation for ValidationRuleEntityPersistencePort
 * Handles conversion between ValidationRule mongo entity and ValidationRuleEntity JPA
 */
@Component
@ConditionalOnPromixJpa
public class ValidationRuleEntityJpaAdapter implements ValidationRuleEntityPersistencePort {

    private final ValidationRuleJpaRepository repository;

    public ValidationRuleEntityJpaAdapter(ValidationRuleJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public ValidationRule save(ValidationRule validationRule) {
        ValidationRuleEntity entity = toJpaEntity(validationRule);
        ValidationRuleEntity saved = repository.save(entity);
        return toMongoEntity(saved);
    }

    @Override
    public Optional<ValidationRule> findById(String id) {
        return repository.findById(id)
                .map(this::toMongoEntity);
    }

    @Override
    public Optional<ValidationRule> findByCode(String code) {
        return repository.findByCode(code)
                .map(this::toMongoEntity);
    }

    @Override
    public List<ValidationRule> findByState(String state) {
        return repository.findByState(state).stream()
                .map(this::toMongoEntity)
                .collect(Collectors.toList());
    }

    @Override
    public Page<ValidationRule> findByState(String state, Pageable pageable) {
        List<ValidationRuleEntity> entities = repository.findByState(state);
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), entities.size());

        List<ValidationRule> pageContent = entities.subList(start, end).stream()
                .map(this::toMongoEntity)
                .collect(Collectors.toList());

        return new PageImpl<>(pageContent, pageable, entities.size());
    }

    @Override
    public List<ValidationRule> findByStateAndVersionGreaterThan(String state, Integer version) {
        return repository.findByState(state).stream()
                .filter(e -> e.getRuleVersion() > version)
                .map(this::toMongoEntity)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByCode(String code) {
        return repository.existsByCode(code);
    }

    @Override
    public Optional<ValidationRule> findTopByCodeOrderByVersionDesc(String code) {
        return repository.findLatestVersionByCode(code)
                .map(this::toMongoEntity);
    }

    @Override
    public List<ValidationRule> findByStateOrderByVersionDesc(String state) {
        return repository.findByStateOrderByVersionDesc(state).stream()
                .map(this::toMongoEntity)
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
     * Convert ValidationRule mongo entity to JPA entity
     */
    private ValidationRuleEntity toJpaEntity(ValidationRule mongoEntity) {
        ValidationRuleEntity jpaEntity = new ValidationRuleEntity();
        jpaEntity.setId(mongoEntity.getId());
        jpaEntity.setCode(mongoEntity.getCode());
        jpaEntity.setName(mongoEntity.getName());
        jpaEntity.setState(mongoEntity.getState());
        jpaEntity.setRuleVersion(mongoEntity.getVersion() != null ? mongoEntity.getVersion().longValue() : 1L);
        jpaEntity.setLogic(mongoEntity.getLogic());
        // Note: dsl is optional and not present in ValidationRule domain model
        jpaEntity.setDsl(null);
        jpaEntity.setPublishedAt(mongoEntity.getPublishedAt());
        jpaEntity.setPublishedBy(mongoEntity.getPublishedBy());
        jpaEntity.setCreatedAt(mongoEntity.getCreatedAt());
        jpaEntity.setCreatedBy(mongoEntity.getCreatedBy());
        jpaEntity.setUpdatedAt(mongoEntity.getUpdatedAt());
        // Note: updatedBy is not present in ValidationRule domain model
        return jpaEntity;
    }

    /**
     * Convert JPA entity to ValidationRule mongo entity
     */
    private ValidationRule toMongoEntity(ValidationRuleEntity jpaEntity) {
        ValidationRule mongoEntity = new ValidationRule();
        mongoEntity.setId(jpaEntity.getId());
        mongoEntity.setCode(jpaEntity.getCode());
        mongoEntity.setName(jpaEntity.getName());
        mongoEntity.setState(jpaEntity.getState());
        mongoEntity.setVersion(jpaEntity.getRuleVersion() != null ? jpaEntity.getRuleVersion().intValue() : 1);
        mongoEntity.setLogic(jpaEntity.getLogic());
        // Note: dsl is optional and not mapped to ValidationRule domain model
        mongoEntity.setPublishedAt(jpaEntity.getPublishedAt());
        mongoEntity.setPublishedBy(jpaEntity.getPublishedBy());
        mongoEntity.setCreatedAt(jpaEntity.getCreatedAt());
        mongoEntity.setCreatedBy(jpaEntity.getCreatedBy());
        mongoEntity.setUpdatedAt(jpaEntity.getUpdatedAt());
        // Note: updatedBy is not present in ValidationRule domain model
        return mongoEntity;
    }
}
