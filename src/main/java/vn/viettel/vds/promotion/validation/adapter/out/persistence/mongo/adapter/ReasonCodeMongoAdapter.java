package vn.viettel.vds.promotion.validation.adapter.out.persistence.mongo.adapter;

import com.promix.platform.autoconfigure.mongo.condition.ConditionalOnPromixMongo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.mongo.repository.ReasonCodeRepository;
import vn.viettel.vds.promotion.validation.application.port.out.ReasonCodePersistencePort;
import vn.viettel.vds.promotion.validation.domain.entity.ReasonCode;

import java.util.List;
import java.util.Optional;

@Component
@ConditionalOnPromixMongo
public class ReasonCodeMongoAdapter implements ReasonCodePersistencePort {

    private final ReasonCodeRepository repository;

    public ReasonCodeMongoAdapter(ReasonCodeRepository repository) {
        this.repository = repository;
    }

    @Override
    public ReasonCode save(ReasonCode reasonCode) {
        return repository.save(reasonCode);
    }

    @Override
    public Optional<ReasonCode> findByIdAndTenant(String tenantId, String id) {
        return repository.findByIdAndTenant(tenantId, id);
    }

    @Override
    public List<ReasonCode> findByTenantAndCategory(String tenantId, String category) {
        return repository.findByTenantAndCategory(tenantId, category);
    }

    @Override
    public Page<ReasonCode> findByTenant(String tenantId, Pageable pageable) {
        return repository.findByTenant(tenantId, pageable);
    }

    @Override
    public Page<ReasonCode> findWithFilters(String tenantId, String categoryPattern,
                                           ReasonCode.Severity severity, Pageable pageable) {
        return repository.findWithFilters(tenantId, categoryPattern, severity, pageable);
    }

    @Override
    public List<ReasonCode> findByTenantIdIsNull() {
        return repository.findByTenantIdIsNull();
    }

    @Override
    public List<ReasonCode> findByTenantId(String tenantId) {
        return repository.findByTenantId(tenantId);
    }

    @Override
    public List<ReasonCode> findByTenantAndSeverity(String tenantId, ReasonCode.Severity severity) {
        return repository.findByTenantAndSeverity(tenantId, severity);
    }

    @Override
    public boolean existsByIdAndTenant(String tenantId, String id) {
        return repository.existsByIdAndTenant(tenantId, id);
    }

    @Override
    public long countByTenant(String tenantId) {
        return repository.countByTenant(tenantId);
    }

    @Override
    public void delete(ReasonCode reasonCode) {
        repository.delete(reasonCode);
    }

    @Override
    public void deleteById(String id) {
        repository.deleteById(id);
    }
}
