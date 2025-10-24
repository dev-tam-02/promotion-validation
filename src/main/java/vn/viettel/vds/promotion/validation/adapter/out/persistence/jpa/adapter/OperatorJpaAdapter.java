package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.adapter;

import com.promix.platform.data.jpa.autoconfigure.condition.ConditionalOnPromixJpa;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.OperatorEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.mapper.OperatorEntityMapper;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.OperatorJpaRepository;
import vn.viettel.vds.promotion.validation.application.port.out.OperatorPersistencePort;
import vn.viettel.vds.promotion.validation.domain.model.Operator;

import java.util.List;
import java.util.Optional;

/**
 * JPA adapter implementation for Operator persistence.
 * Active when promix.jpa.enabled=true
 */
@Component
@ConditionalOnPromixJpa
public class OperatorJpaAdapter implements OperatorPersistencePort {

    private final OperatorJpaRepository repository;
    private final OperatorEntityMapper mapper;

    public OperatorJpaAdapter(OperatorJpaRepository repository, OperatorEntityMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Operator save(Operator operator) {
        OperatorEntity entity = mapper.toEntity(operator);
        OperatorEntity saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Operator> findById(String id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Operator> findByTenantIdAndNameAndVersion(String tenantId, String name, Integer version) {
        return repository.findByNameAndOperatorVersion(name, version)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Operator> findLatestVersion(String tenantId, String name) {
        return repository.findLatestVersionByName(name)
                .map(mapper::toDomain);
    }

    @Override
    public List<Operator> findByTenantIdAndContext(String tenantId, String context) {
        return repository.findByOrderByNameAscOperatorVersionDesc()
                .stream()
                .filter(e -> e.getContext() != null && e.getContext().equals(context))
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Page<Operator> findByTenantIdAndContextAndStatus(String tenantId, String context,
                                                            Operator.OperatorStatus status, Pageable pageable) {
        List<OperatorEntity> entities = repository.findByContextAndStatus(
                context,
                OperatorEntity.OperatorStatus.valueOf(status.name())
        );

        // Manual pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), entities.size());
        List<Operator> pageContent = entities.subList(start, end)
                .stream()
                .map(mapper::toDomain)
                .toList();

        return new PageImpl<>(pageContent, pageable, entities.size());
    }

    @Override
    public List<Operator> findByTenantIdAndStatus(String tenantId, Operator.OperatorStatus status) {
        return repository.findByStatus(
                OperatorEntity.OperatorStatus.valueOf(status.name())
        ).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Operator> findAllVersions(String tenantId, String name) {
        return repository.findByNameOrderByOperatorVersionDesc(name)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Page<Operator> findWithFilters(String tenantId, String contextPattern,
                                          Operator.OperatorStatus status, Pageable pageable) {
        // Simplified implementation - can be enhanced with Specifications
        List<OperatorEntity> all = repository.findByOrderByNameAscOperatorVersionDesc();
        List<OperatorEntity> filtered = all.stream()
                .filter(e -> status == null || e.getStatus().name().equals(status.name()))
                .filter(e -> contextPattern == null ||
                        (e.getContext() != null && e.getContext().contains(contextPattern)))
                .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filtered.size());
        List<Operator> pageContent = filtered.subList(start, end)
                .stream()
                .map(mapper::toDomain)
                .toList();

        return new PageImpl<>(pageContent, pageable, filtered.size());
    }

    @Override
    public boolean existsByTenantIdAndNameAndVersion(String tenantId, String name, Integer version) {
        return repository.existsByNameAndOperatorVersion(name, version);
    }

    @Override
    public List<Operator> findGlobalOperatorsByStatus(Operator.OperatorStatus status) {
        return repository.findByStatus(
                OperatorEntity.OperatorStatus.valueOf(status.name())
        ).stream().map(mapper::toDomain).toList();
    }

    @Override
    public long countByTenantIdAndStatus(String tenantId, Operator.OperatorStatus status) {
        return repository.findByStatus(
                OperatorEntity.OperatorStatus.valueOf(status.name())
        ).size();
    }

    @Override
    public void delete(Operator operator) {
        repository.deleteById(operator.getId());
    }

    @Override
    public void deleteById(String id) {
        repository.deleteById(id);
    }

    @Override
    public List<Operator> findAllByTenantId(String tenantId) {
        return repository.findByOrderByNameAscOperatorVersionDesc()
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}
