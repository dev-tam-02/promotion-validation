package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.adapter;

import com.promix.platform.data.jpa.autoconfigure.condition.ConditionalOnPromixJpa;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.ReasonCodeEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.mapper.ReasonCodeEntityMapper;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.ReasonCodeJpaRepository;
import vn.viettel.vds.promotion.validation.application.port.out.ReasonCodePersistencePort;
import vn.viettel.vds.promotion.validation.domain.model.ReasonCode;

import java.util.List;
import java.util.Optional;

@Component
@ConditionalOnPromixJpa
public class ReasonCodeJpaAdapter implements ReasonCodePersistencePort {

    private final ReasonCodeJpaRepository repository;
    private final ReasonCodeEntityMapper mapper;

    public ReasonCodeJpaAdapter(ReasonCodeJpaRepository repository,
                                ReasonCodeEntityMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public ReasonCode save(ReasonCode reasonCode) {
        ReasonCodeEntity entity = mapper.toEntity(reasonCode);
        ReasonCodeEntity saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<ReasonCode> findByIdAndTenant(String tenantId, String id) {
        // Note: ReasonCode entities don't have separate tenantId field
        // This implementation ignores tenantId parameter
        return repository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public List<ReasonCode> findByTenantAndCategory(String tenantId, String category) {
        return repository.findByCategory(category).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Page<ReasonCode> findByTenant(String tenantId, Pageable pageable) {
        List<ReasonCodeEntity> all = repository.findAll();
        return convertToPage(all, pageable);
    }

    @Override
    public Page<ReasonCode> findWithFilters(String tenantId, String categoryPattern,
                                            ReasonCode.Severity severity, Pageable pageable) {
        // Implement filtering manually
        List<ReasonCodeEntity> all = repository.findAll();
        List<ReasonCodeEntity> filtered = all.stream()
                .filter(e -> categoryPattern == null ||
                        (e.getCategory() != null && e.getCategory().toLowerCase().contains(categoryPattern.toLowerCase())))
                .filter(e -> severity == null ||
                        (e.getSeverity() != null && e.getSeverity().name().equals(severity.name())))
                .toList();
        return convertToPage(filtered, pageable);
    }

    @Override
    public List<ReasonCode> findByTenantIdIsNull() {
        return repository.findAll().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<ReasonCode> findByTenantId(String tenantId) {
        return repository.findAll().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<ReasonCode> findByTenantAndSeverity(String tenantId, ReasonCode.Severity severity) {
        ReasonCodeEntity.Severity entitySeverity = mapper.mapSeverity(severity);
        return repository.findBySeverity(entitySeverity).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByIdAndTenant(String tenantId, String id) {
        return repository.existsById(id);
    }

    @Override
    public long countByTenant(String tenantId) {
        return repository.count();
    }

    @Override
    public void delete(ReasonCode reasonCode) {
        repository.deleteById(reasonCode.getId());
    }

    @Override
    public void deleteById(String id) {
        repository.deleteById(id);
    }

    private Page<ReasonCode> convertToPage(List<ReasonCodeEntity> entities, Pageable pageable) {
        int start = (int) pageable.getOffset();
        // Handle case when start is beyond the list size (return empty page)
        if (start >= entities.size()) {
            return new PageImpl<>(List.of(), pageable, entities.size());
        }
        int end = Math.min((start + pageable.getPageSize()), entities.size());
        List<ReasonCode> pageContent = entities.subList(start, end)
                .stream()
                .map(mapper::toDomain)
                .toList();
        return new PageImpl<>(pageContent, pageable, entities.size());
    }
}
