package vn.viettel.vds.promotion.validation.adapter.out.persistence.mongo.adapter;

import com.promix.platform.autoconfigure.mongo.condition.ConditionalOnPromixMongo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.mongo.repository.AuditLogRepository;
import vn.viettel.vds.promotion.validation.application.port.out.AuditLogPersistencePort;
import vn.viettel.vds.promotion.validation.domain.entity.AuditLog;

import java.time.Instant;
import java.util.List;

/**
 * MongoDB adapter implementation for AuditLog persistence.
 */
@Component
@ConditionalOnPromixMongo
public class AuditLogMongoAdapter implements AuditLogPersistencePort {

    private final AuditLogRepository repository;

    public AuditLogMongoAdapter(AuditLogRepository repository) {
        this.repository = repository;
    }

    @Override
    public AuditLog save(AuditLog auditLog) {
        return repository.save(auditLog);
    }

    @Override
    public Page<AuditLog> findByTenantIdAndAction(String tenantId, AuditLog.AuditAction action, Pageable pageable) {
        return repository.findByTenantIdAndActionOrderByAtDesc(tenantId, action, pageable);
    }

    @Override
    public Page<AuditLog> findByTenantIdAndTimeBetween(String tenantId, Instant from, Instant to, Pageable pageable) {
        return repository.findByTenantIdAndTimeBetween(tenantId, from, to, pageable);
    }

    @Override
    public Page<AuditLog> findByTenantIdAndActor(String tenantId, String actor, Pageable pageable) {
        return repository.findByTenantIdAndActorOrderByAtDesc(tenantId, actor, pageable);
    }

    @Override
    public Page<AuditLog> findWithFilters(String tenantId, AuditLog.AuditAction action, String actorPattern,
                                         Instant from, Instant to, Pageable pageable) {
        return repository.findWithFilters(tenantId, action, actorPattern, from, to, pageable);
    }

    @Override
    public List<AuditLog> findByTenantIdAndTarget(String tenantId, String targetType, String targetId) {
        return repository.findByTenantIdAndTargetTypeAndTargetIdOrderByAtDesc(tenantId, targetType, targetId);
    }

    @Override
    public Page<AuditLog> findByTenantId(String tenantId, Pageable pageable) {
        return repository.findByTenantIdOrderByAtDesc(tenantId, pageable);
    }

    @Override
    public List<AuditLog> findLogsOlderThan(Instant cutoffTime) {
        return repository.findLogsOlderThan(cutoffTime);
    }

    @Override
    public long countByTenantIdAndAction(String tenantId, AuditLog.AuditAction action) {
        return repository.countByTenantIdAndAction(tenantId, action);
    }

    @Override
    public long countByTenantIdAndTimeBetween(String tenantId, Instant from, Instant to) {
        return repository.countByTenantIdAndTimeBetween(tenantId, from, to);
    }

    @Override
    public void deleteLogsOlderThan(Instant cutoffTime) {
        repository.deleteLogsOlderThan(cutoffTime);
    }
}
