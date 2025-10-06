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
import vn.viettel.vds.promotion.validation.domain.entity.AuditLog;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

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
        List<AuditLogEntity> entities = repository.findByTenantIdAndActionOrderByAtDesc(
                tenantId,
                AuditLogEntity.AuditAction.valueOf(action.name())
        );
        return convertToPage(entities, pageable);
    }

    @Override
    public Page<AuditLog> findByTenantIdAndTimeBetween(String tenantId, Instant from, Instant to, Pageable pageable) {
        List<AuditLogEntity> entities = repository.findByTenantIdAndDateRange(tenantId, from, to);
        return convertToPage(entities, pageable);
    }

    @Override
    public Page<AuditLog> findByTenantIdAndActor(String tenantId, String actor, Pageable pageable) {
        // JPA repository doesn't have this method - filtering manually
        List<AuditLogEntity> all = repository.findByTenantIdOrderByAtDesc(tenantId);
        List<AuditLogEntity> filtered = all.stream()
                .filter(e -> e.getActor() != null && e.getActor().equals(actor))
                .collect(Collectors.toList());
        return convertToPage(filtered, pageable);
    }

    @Override
    public Page<AuditLog> findWithFilters(String tenantId, AuditLog.AuditAction action, String actorPattern,
                                         Instant from, Instant to, Pageable pageable) {
        List<AuditLogEntity> all = repository.findByTenantIdOrderByAtDesc(tenantId);
        List<AuditLogEntity> filtered = all.stream()
                .filter(e -> action == null || e.getAction().name().equals(action.name()))
                .filter(e -> actorPattern == null ||
                        (e.getActor() != null && e.getActor().contains(actorPattern)))
                .filter(e -> from == null || e.getAt().isAfter(from) || e.getAt().equals(from))
                .filter(e -> to == null || e.getAt().isBefore(to) || e.getAt().equals(to))
                .collect(Collectors.toList());
        return convertToPage(filtered, pageable);
    }

    @Override
    public List<AuditLog> findByTenantIdAndTarget(String tenantId, String targetType, String targetId) {
        List<AuditLogEntity> entities = repository.findByTenantIdAndTarget(tenantId, targetType, targetId);
        return entities.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Page<AuditLog> findByTenantId(String tenantId, Pageable pageable) {
        List<AuditLogEntity> entities = repository.findByTenantIdOrderByAtDesc(tenantId);
        return convertToPage(entities, pageable);
    }

    @Override
    public List<AuditLog> findLogsOlderThan(Instant cutoffTime) {
        // JPA repository doesn't have this method - need to add or use findAll with filter
        return repository.findAll().stream()
                .filter(e -> e.getAt().isBefore(cutoffTime))
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long countByTenantIdAndAction(String tenantId, AuditLog.AuditAction action) {
        return repository.findByTenantIdAndActionOrderByAtDesc(
                tenantId,
                AuditLogEntity.AuditAction.valueOf(action.name())
        ).size();
    }

    @Override
    public long countByTenantIdAndTimeBetween(String tenantId, Instant from, Instant to) {
        return repository.findByTenantIdAndDateRange(tenantId, from, to).size();
    }

    @Override
    public void deleteLogsOlderThan(Instant cutoffTime) {
        List<AuditLogEntity> oldLogs = repository.findAll().stream()
                .filter(e -> e.getAt().isBefore(cutoffTime))
                .collect(Collectors.toList());
        repository.deleteAll(oldLogs);
    }

    private Page<AuditLog> convertToPage(List<AuditLogEntity> entities, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), entities.size());
        List<AuditLog> pageContent = entities.subList(start, end)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
        return new PageImpl<>(pageContent, pageable, entities.size());
    }
}
