package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.adapter;

import com.promix.platform.data.jpa.autoconfigure.condition.ConditionalOnPromixJpa;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.TemporalPolicyEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.TemporalPolicyWindowEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.mapper.TemporalPolicyEntityMapper;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.TemporalPolicyJpaRepository;
import vn.viettel.vds.promotion.validation.application.port.out.TemporalPolicyPersistencePort;
import vn.viettel.vds.promotion.validation.domain.model.TemporalPolicy;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
@ConditionalOnPromixJpa
public class TemporalPolicyJpaAdapter implements TemporalPolicyPersistencePort {

    private final TemporalPolicyJpaRepository repository;
    private final TemporalPolicyEntityMapper mapper;

    public TemporalPolicyJpaAdapter(TemporalPolicyJpaRepository repository,
                                    TemporalPolicyEntityMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public TemporalPolicy save(TemporalPolicy temporalPolicy) {
        TemporalPolicyEntity entity = mapper.toEntity(temporalPolicy);

        // Set bidirectional relationship for timeOfDayWindows
        if (entity.getTimeOfDayWindows() != null) {
            for (TemporalPolicyWindowEntity window : entity.getTimeOfDayWindows()) {
                window.setTemporalPolicy(entity);
            }
        }

        TemporalPolicyEntity saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<TemporalPolicy> findById(String id) {
        return repository.findByIdWithTimeWindows(id).map(mapper::toDomain);
    }

    @Override
    public Optional<TemporalPolicy> findByTenantIdAndName(String tenantId, String name) {
        // TemporalPolicyEntity doesn't support multi-tenancy (no tenantId field)
        // Ignoring tenantId parameter for now - consider adding tenantId field to entity if needed
        return repository.findByName(name)
                .map(mapper::toDomain);
    }

    @Override
    public List<TemporalPolicy> findByTenantIdAndTz(String tenantId, String tz) {
        // TemporalPolicyEntity doesn't support multi-tenancy (no tenantId field)
        // Ignoring tenantId parameter
        return repository.findByTz(tz).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Page<TemporalPolicy> findByTenantIdAndNamePattern(String tenantId, String namePattern, Pageable pageable) {
        // JPA doesn't have this query - implement with filters
        // Note: tenantId filtering not supported (entity has no tenantId field)
        List<TemporalPolicyEntity> all = repository.findAll();
        List<TemporalPolicyEntity> filtered = all.stream()
                .filter(e -> namePattern == null || e.getName().toLowerCase().contains(namePattern.toLowerCase()))
                .toList();
        return convertToPage(filtered, pageable);
    }

    @Override
    public Page<TemporalPolicy> findWithFilters(String tenantId, String tz, String namePattern, Pageable pageable) {
        // Implement filtering manually
        // Note: tenantId filtering not supported (entity has no tenantId field)
        List<TemporalPolicyEntity> all = repository.findAll();
        List<TemporalPolicyEntity> filtered = all.stream()
                .filter(e -> tz == null || (e.getTz() != null && e.getTz().equals(tz)))
                .filter(e -> namePattern == null || e.getName().toLowerCase().contains(namePattern.toLowerCase()))
                .toList();
        return convertToPage(filtered, pageable);
    }

    @Override
    public List<TemporalPolicy> findByTenantIdOrderByNameAsc(String tenantId) {
        // Note: tenantId filtering not supported (entity has no tenantId field)
        return repository.findAll().stream()
                .sorted((e1, e2) -> e1.getName().compareTo(e2.getName()))
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByTenantIdAndName(String tenantId, String name) {
        // Note: tenantId filtering not supported (entity has no tenantId field)
        return repository.findByName(name).isPresent();
    }

    @Override
    public long countByTenantId(String tenantId) {
        // Note: tenantId filtering not supported (entity has no tenantId field)
        return repository.count();
    }

    @Override
    public Page<TemporalPolicy> findByTenantIdAndTz(String tenantId, String tz, Pageable pageable) {
        // Note: tenantId filtering not supported (entity has no tenantId field)
        List<TemporalPolicyEntity> filtered = repository.findByTz(tz);
        return convertToPage(filtered, pageable);
    }

    @Override
    public List<TemporalPolicy> findActivePoliciesAt(Instant checkTime) {
        return repository.findActivePoliciesAt(checkTime).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<TemporalPolicy> findByRruleIsNotNull() {
        return repository.findByRruleIsNotNull().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public void delete(TemporalPolicy temporalPolicy) {
        repository.deleteById(temporalPolicy.getId());
    }

    @Override
    public void deleteById(String id) {
        repository.deleteById(id);
    }

    private Page<TemporalPolicy> convertToPage(List<TemporalPolicyEntity> entities, Pageable pageable) {
        int start = (int) pageable.getOffset();
        // Handle case when start is beyond the list size (return empty page)
        if (start >= entities.size()) {
            return new PageImpl<>(List.of(), pageable, entities.size());
        }
        int end = Math.min((start + pageable.getPageSize()), entities.size());
        List<TemporalPolicy> pageContent = entities.subList(start, end)
                .stream()
                .map(mapper::toDomain)
                .toList();
        return new PageImpl<>(pageContent, pageable, entities.size());
    }
}
