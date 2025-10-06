package vn.viettel.vds.promotion.validation.adapter.out.persistence.mongo.adapter;

import com.promix.platform.autoconfigure.mongo.condition.ConditionalOnPromixMongo;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.mongo.entity.ValidationRule;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.mongo.repository.ValidationRuleMongoRepository;
import vn.viettel.vds.promotion.validation.application.port.out.ValidationRuleEntityPersistencePort;

import java.util.List;
import java.util.Optional;

/**
 * MongoDB adapter implementation for ValidationRuleEntityPersistencePort
 */
@Component
@ConditionalOnPromixMongo
public class ValidationRuleEntityMongoAdapter implements ValidationRuleEntityPersistencePort {

    private final ValidationRuleMongoRepository repository;

    public ValidationRuleEntityMongoAdapter(ValidationRuleMongoRepository repository) {
        this.repository = repository;
    }

    @Override
    public ValidationRule save(ValidationRule validationRule) {
        return repository.save(validationRule);
    }

    @Override
    public Optional<ValidationRule> findById(String id) {
        return repository.findById(id);
    }

    @Override
    public Optional<ValidationRule> findByCode(String code) {
        return repository.findByCode(code);
    }

    @Override
    public List<ValidationRule> findByState(String state) {
        return repository.findByState(state);
    }

    @Override
    public Page<ValidationRule> findByState(String state, Pageable pageable) {
        return repository.findByState(state, pageable);
    }

    @Override
    public List<ValidationRule> findByStateAndVersionGreaterThan(String state, Integer version) {
        return repository.findByStateAndVersionGreaterThan(state, version);
    }

    @Override
    public boolean existsByCode(String code) {
        return repository.existsByCode(code);
    }

    @Override
    public Optional<ValidationRule> findTopByCodeOrderByVersionDesc(String code) {
        return repository.findTopByCodeOrderByVersionDesc(code);
    }

    @Override
    public List<ValidationRule> findByStateOrderByVersionDesc(String state) {
        return repository.findByStateOrderByVersionDesc(state);
    }

    @Override
    public void deleteById(String id) {
        repository.deleteById(id);
    }

    @Override
    public boolean existsById(String id) {
        return repository.existsById(id);
    }
}
