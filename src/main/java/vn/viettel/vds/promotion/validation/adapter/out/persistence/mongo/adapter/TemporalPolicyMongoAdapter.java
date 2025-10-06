package vn.viettel.vds.promotion.validation.adapter.out.persistence.mongo.adapter;

import com.promix.platform.autoconfigure.mongo.condition.ConditionalOnPromixMongo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.mongo.repository.TemporalPolicyMongoRepository;
import vn.viettel.vds.promotion.validation.application.port.out.TemporalPolicyPersistencePort;
import vn.viettel.vds.promotion.validation.domain.entity.TemporalPolicy;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@ConditionalOnPromixMongo
public class TemporalPolicyMongoAdapter implements TemporalPolicyPersistencePort {

    private final TemporalPolicyMongoRepository repository;

    public TemporalPolicyMongoAdapter(TemporalPolicyMongoRepository repository) {
        this.repository = repository;
    }

    @Override
    public TemporalPolicy save(TemporalPolicy temporalPolicy) {
        return repository.save(temporalPolicy);
    }

    @Override
    public Optional<TemporalPolicy> findById(String id) {
        return repository.findById(id);
    }

    @Override
    public Optional<TemporalPolicy> findByTenantIdAndName(String tenantId, String name) {
        return repository.findByTenantIdAndName(tenantId, name);
    }

    @Override
    public List<TemporalPolicy> findByTenantIdAndTz(String tenantId, String tz) {
        return repository.findByTenantIdAndTz(tenantId, tz);
    }

    @Override
    public Page<TemporalPolicy> findByTenantIdAndNamePattern(String tenantId, String namePattern, Pageable pageable) {
        return repository.findByTenantIdAndNamePattern(tenantId, namePattern, pageable);
    }

    @Override
    public Page<TemporalPolicy> findWithFilters(String tenantId, String tz, String namePattern, Pageable pageable) {
        return repository.findWithFilters(tenantId, tz, namePattern, pageable);
    }

    @Override
    public List<TemporalPolicy> findByTenantIdOrderByNameAsc(String tenantId) {
        return repository.findByTenantIdOrderByNameAsc(tenantId);
    }

    @Override
    public boolean existsByTenantIdAndName(String tenantId, String name) {
        return repository.existsByTenantIdAndName(tenantId, name);
    }

    @Override
    public long countByTenantId(String tenantId) {
        return repository.countByTenantId(tenantId);
    }

    @Override
    public Page<TemporalPolicy> findByTenantIdAndTz(String tenantId, String tz, Pageable pageable) {
        return repository.findByTenantIdAndTz(tenantId, tz, pageable);
    }

    @Override
    public List<TemporalPolicy> findActivePoliciesAt(Instant checkTime) {
        // MongoDB doesn't have this query - implement with filters
        return repository.findAll().stream()
                .filter(tp -> (tp.getStartTs() == null || !tp.getStartTs().isAfter(checkTime)) &&
                        (tp.getEndTs() == null || !tp.getEndTs().isBefore(checkTime)))
                .collect(Collectors.toList());
    }

    @Override
    public List<TemporalPolicy> findByRruleIsNotNull() {
        // MongoDB doesn't have this query - implement with filters
        return repository.findAll().stream()
                .filter(tp -> tp.getRrule() != null)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(TemporalPolicy temporalPolicy) {
        repository.delete(temporalPolicy);
    }

    @Override
    public void deleteById(String id) {
        repository.deleteById(id);
    }
}
