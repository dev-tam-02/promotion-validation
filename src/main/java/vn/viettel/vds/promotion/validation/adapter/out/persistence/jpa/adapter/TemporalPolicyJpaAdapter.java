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
import java.util.stream.Collectors;

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
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<TemporalPolicy> findByTenantIdAndName(String tenantId, String name) {
        // JPA repository doesn't have tenantId parameter, but entity has tenantId from BaseEntity
        // Need to find by name then filter by tenantId
        return repository.findByName(name)
                .filter(entity -> entity.equals(tenantId))
                .map(mapper::toDomain);
    }

    @Override
    public List<TemporalPolicy> findByTenantIdAndTz(String tenantId, String tz) {
        // Find by timezone then filter by tenantId
        return repository.findByTz(tz).stream()
                .filter(entity -> entity.equals(tenantId))
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Page<TemporalPolicy> findByTenantIdAndNamePattern(String tenantId, String namePattern, Pageable pageable) {
        // JPA doesn't have this query - implement with filters
        List<TemporalPolicyEntity> all = repository.findAll();
        List<TemporalPolicyEntity> filtered = all.stream()
                .filter(e -> e.equals(tenantId))
                .filter(e -> namePattern == null || e.getName().toLowerCase().contains(namePattern.toLowerCase()))
                .collect(Collectors.toList());
        return convertToPage(filtered, pageable);
    }

    @Override
    public Page<TemporalPolicy> findWithFilters(String tenantId, String tz, String namePattern, Pageable pageable) {
        // Implement filtering manually
        List<TemporalPolicyEntity> all = repository.findAll();
        List<TemporalPolicyEntity> filtered = all.stream()
                .filter(e -> e.equals(tenantId))
                .filter(e -> tz == null || (e.getTz() != null && e.getTz().equals(tz)))
                .filter(e -> namePattern == null || e.getName().toLowerCase().contains(namePattern.toLowerCase()))
                .collect(Collectors.toList());
        return convertToPage(filtered, pageable);
    }

    @Override
    public List<TemporalPolicy> findByTenantIdOrderByNameAsc(String tenantId) {
        // Find all then filter by tenantId and sort by name
        return repository.findAll().stream()
                .filter(e -> e.equals(tenantId))
                .sorted((e1, e2) -> e1.getName().compareTo(e2.getName()))
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByTenantIdAndName(String tenantId, String name) {
        return repository.findByName(name)
                .map(entity -> entity.equals(tenantId))
                .orElse(false);
    }

    @Override
    public long countByTenantId(String tenantId) {
        return repository.findAll().stream()
                .filter(e -> e.equals(tenantId))
                .count();
    }

    @Override
    public Page<TemporalPolicy> findByTenantIdAndTz(String tenantId, String tz, Pageable pageable) {
        List<TemporalPolicyEntity> filtered = repository.findByTz(tz).stream()
                .filter(entity -> entity.equals(tenantId))
                .collect(Collectors.toList());
        return convertToPage(filtered, pageable);
    }

    @Override
    public List<TemporalPolicy> findActivePoliciesAt(Instant checkTime) {
        return repository.findActivePoliciesAt(checkTime).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<TemporalPolicy> findByRruleIsNotNull() {
        return repository.findByRruleIsNotNull().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
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
        int end = Math.min((start + pageable.getPageSize()), entities.size());
        List<TemporalPolicy> pageContent = entities.subList(start, end)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
        return new PageImpl<>(pageContent, pageable, entities.size());
    }
}
