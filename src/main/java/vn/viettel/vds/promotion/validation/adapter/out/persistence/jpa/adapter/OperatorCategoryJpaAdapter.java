package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.adapter;

import com.promix.platform.data.jpa.autoconfigure.condition.ConditionalOnPromixJpa;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.OperatorCategoryEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.mapper.OperatorCategoryMapper;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.OperatorCategoryJpaRepository;
import vn.viettel.vds.promotion.validation.application.port.out.OperatorCategoryPersistencePort;
import vn.viettel.vds.promotion.validation.domain.model.OperatorCategory;

import java.util.List;
import java.util.Optional;

/**
 * JPA adapter implementation for OperatorCategory persistence.
 */
@Component
@ConditionalOnPromixJpa
public class OperatorCategoryJpaAdapter implements OperatorCategoryPersistencePort {

    private final OperatorCategoryJpaRepository repository;
    private final OperatorCategoryMapper mapper;

    public OperatorCategoryJpaAdapter(OperatorCategoryJpaRepository repository, OperatorCategoryMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<OperatorCategory> findAllActiveWithOptions() {
        return repository.findAllWithOptions()
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<OperatorCategory> findAll() {
        return repository.findAllByOrderByDisplayOrderAsc()
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<OperatorCategory> findById(String id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<OperatorCategory> findByCode(String code) {
        return repository.findByCode(code).map(mapper::toDomain);
    }

    @Override
    public Optional<OperatorCategory> findByCodeWithOptions(String code) {
        return repository.findByCode(code)
                .map(entity -> {
                    // Force load options
                    entity.getOptions().forEach(opt -> { /* trigger lazy load */ });
                    return mapper.toDomain(entity);
                });
    }

    @Override
    public List<OperatorCategory> findMetadataCategories() {
        return repository.findByMetadataCategoryTrueAndActiveTrueOrderByDisplayOrderAsc()
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<OperatorCategory> findNonMetadataCategories() {
        return repository.findByMetadataCategoryFalseAndActiveTrueOrderByDisplayOrderAsc()
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public OperatorCategory save(OperatorCategory category) {
        OperatorCategoryEntity entity = mapper.toEntity(category);
        OperatorCategoryEntity saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public boolean existsByCode(String code) {
        return repository.existsByCode(code);
    }

    @Override
    public void deleteById(String id) {
        repository.deleteById(id);
    }
}
