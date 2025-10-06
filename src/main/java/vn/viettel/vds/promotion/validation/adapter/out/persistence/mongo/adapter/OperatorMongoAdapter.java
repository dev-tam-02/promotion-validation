package vn.viettel.vds.promotion.validation.adapter.out.persistence.mongo.adapter;

import com.promix.platform.autoconfigure.mongo.condition.ConditionalOnPromixMongo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.mongo.repository.OperatorRepository;
import vn.viettel.vds.promotion.validation.application.port.out.OperatorPersistencePort;
import vn.viettel.vds.promotion.validation.domain.entity.Operator;

import java.util.List;
import java.util.Optional;

/**
 * MongoDB adapter implementation for Operator persistence.
 * Active when promix.mongo.enabled=true
 */
@Component
@ConditionalOnPromixMongo
public class OperatorMongoAdapter implements OperatorPersistencePort {

    private final OperatorRepository repository;

    public OperatorMongoAdapter(OperatorRepository repository) {
        this.repository = repository;
    }

    @Override
    public Operator save(Operator operator) {
        return repository.save(operator);
    }

    @Override
    public Optional<Operator> findById(String id) {
        return repository.findById(id);
    }

    @Override
    public Optional<Operator> findByTenantIdAndNameAndVersion(String tenantId, String name, Integer version) {
        return repository.findByTenantIdAndNameAndVersion(tenantId, name, version);
    }

    @Override
    public Optional<Operator> findLatestVersion(String tenantId, String name) {
        return repository.findFirstByTenantIdAndNameOrderByVersionDesc(tenantId, name);
    }

    @Override
    public List<Operator> findByTenantIdAndContext(String tenantId, String context) {
        return repository.findByTenantIdAndContext(tenantId, context);
    }

    @Override
    public Page<Operator> findByTenantIdAndContextAndStatus(String tenantId, String context,
                                                             Operator.OperatorStatus status, Pageable pageable) {
        return repository.findByTenantIdAndContextAndStatus(tenantId, context, status, pageable);
    }

    @Override
    public List<Operator> findByTenantIdAndStatus(String tenantId, Operator.OperatorStatus status) {
        return repository.findByTenantIdAndStatus(tenantId, status);
    }

    @Override
    public List<Operator> findAllVersions(String tenantId, String name) {
        return repository.findByTenantIdAndNameOrderByVersionDesc(tenantId, name);
    }

    @Override
    public Page<Operator> findWithFilters(String tenantId, String contextPattern,
                                          Operator.OperatorStatus status, Pageable pageable) {
        return repository.findWithFilters(tenantId, contextPattern, status, pageable);
    }

    @Override
    public boolean existsByTenantIdAndNameAndVersion(String tenantId, String name, Integer version) {
        return repository.existsByTenantIdAndNameAndVersion(tenantId, name, version);
    }

    @Override
    public List<Operator> findGlobalOperatorsByStatus(Operator.OperatorStatus status) {
        return repository.findByTenantIdIsNullAndStatus(status);
    }

    @Override
    public long countByTenantIdAndStatus(String tenantId, Operator.OperatorStatus status) {
        return repository.countByTenantIdAndStatus(tenantId, status);
    }

    @Override
    public void delete(Operator operator) {
        repository.delete(operator);
    }

    @Override
    public void deleteById(String id) {
        repository.deleteById(id);
    }

    @Override
    public List<Operator> findAllByTenantId(String tenantId) {
        // MongoDB repository doesn't have this method, find all by status instead
        return repository.findByTenantIdAndStatus(tenantId, Operator.OperatorStatus.ACTIVE);
    }
}
