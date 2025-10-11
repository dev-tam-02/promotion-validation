package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.adapter;

import com.promix.platform.data.jpa.autoconfigure.condition.ConditionalOnPromixJpa;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.AuditLogEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.mapper.AuditLogEntityMapper;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.AuditLogJpaRepository;
import vn.viettel.vds.promotion.validation.application.port.out.AuditLogPersistencePort;
import vn.viettel.vds.promotion.validation.domain.model.AuditLog;

import java.time.Instant;
import java.util.List;
/**
 * JPA adapter implementation for AuditLog persistence.
 */
@Component
@ConditionalOnPromixJpa
public class AuditLogJpaAdapter implements AuditLogPersistencePort {

    private final AuditLogJpaRepository repository;
    private final AuditLogEntityMapper mapper;

    public AuditLogJpaAdapter(AuditLogJpaRepository repository, AuditLogEntityMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public AuditLog save(AuditLog auditLog) {
        AuditLogEntity entity = mapper.toEntity(auditLog);
        AuditLogEntity saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Page<AuditLog> findByTenantIdAndAction(String tenantId, AuditLog.AuditAction action, Pageable pageable) {
        // Note: tenantId removed from schema, using action field only
        List<AuditLogEntity> entities = repository.findByActionOrderByTimestampDesc(action.name());
        return convertToPage(entities, pageable);
    }

    @Override
    public Page<AuditLog> findByTenantIdAndTimeBetween(String tenantId, Instant from, Instant to, Pageable pageable) {
        // Note: tenantId removed from schema
        List<AuditLogEntity> entities = repository.findByDateRange(from, to);
        return convertToPage(entities, pageable);
    }

    @Override
    public Page<AuditLog> findByTenantIdAndActor(String tenantId, String actor, Pageable pageable) {
        // Note: tenantId removed, using actorId field
        List<AuditLogEntity> entities = repository.findByActorIdOrderByTimestampDesc(actor);
        return convertToPage(entities, pageable);
    }

    @Override
    public Page<AuditLog> findWithFilters(String tenantId, AuditLog.AuditAction action, String actorPattern,
                                          Instant from, Instant to, Pageable pageable) {
        // Note: tenantId removed, filtering from all logs
        List<AuditLogEntity> all = repository.findAllByOrderByTimestampDesc();
        List<AuditLogEntity> filtered = all.stream()
                .filter(e -> action == null || e.getAction().equals(action.name()))
                .filter(e -> actorPattern == null ||
                        (e.getActorId() != null && e.getActorId().contains(actorPattern)))
                .filter(e -> from == null || e.getTimestamp().isAfter(from) || e.getTimestamp().equals(from))
                .filter(e -> to == null || e.getTimestamp().isBefore(to) || e.getTimestamp().equals(to))
                .toList();
        return convertToPage(filtered, pageable);
    }

    @Override
    public List<AuditLog> findByTenantIdAndTarget(String tenantId, String targetType, String targetId) {
        // Note: tenantId removed, using entityType/entityId fields
        List<AuditLogEntity> entities = repository.findByEntity(targetType, targetId);
        return entities.stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Page<AuditLog> findByTenantId(String tenantId, Pageable pageable) {
        // Note: tenantId removed from schema, returning all logs
        List<AuditLogEntity> entities = repository.findAllByOrderByTimestampDesc();
        return convertToPage(entities, pageable);
    }

    @Override
    public List<AuditLog> findLogsOlderThan(Instant cutoffTime) {
        // Filter logs by timestamp
        return repository.findAll().stream()
                .filter(e -> e.getTimestamp().isBefore(cutoffTime))
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public long countByTenantIdAndAction(String tenantId, AuditLog.AuditAction action) {
        // Note: tenantId removed from schema
        return repository.findByActionOrderByTimestampDesc(action.name()).size();
    }

    @Override
    public long countByTenantIdAndTimeBetween(String tenantId, Instant from, Instant to) {
        // Note: tenantId removed from schema
        return repository.findByDateRange(from, to).size();
    }

    @Override
    public void deleteLogsOlderThan(Instant cutoffTime) {
        List<AuditLogEntity> oldLogs = repository.findAll().stream()
                .filter(e -> e.getTimestamp().isBefore(cutoffTime))
                .toList();
        repository.deleteAll(oldLogs);
    }

    private Page<AuditLog> convertToPage(List<AuditLogEntity> entities, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), entities.size());
        List<AuditLog> pageContent = entities.subList(start, end)
                .stream()
                .map(mapper::toDomain)
                .toList();
        return new PageImpl<>(pageContent, pageable, entities.size());
    }
}
