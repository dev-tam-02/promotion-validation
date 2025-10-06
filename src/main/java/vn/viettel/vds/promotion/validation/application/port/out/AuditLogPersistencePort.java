package vn.viettel.vds.promotion.validation.application.port.out;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.viettel.vds.promotion.validation.domain.model.AuditLog;

import java.time.Instant;
import java.util.List;

/**
 * Outbound port for AuditLog persistence operations.
 */
public interface AuditLogPersistencePort {

    /**
     * Save audit log
     */
    AuditLog save(AuditLog auditLog);

    /**
     * Find audit logs by tenant and action
     */
    Page<AuditLog> findByTenantIdAndAction(String tenantId, AuditLog.AuditAction action, Pageable pageable);

    /**
     * Find audit logs by tenant and time range
     */
    Page<AuditLog> findByTenantIdAndTimeBetween(String tenantId, Instant from, Instant to, Pageable pageable);

    /**
     * Find audit logs by tenant and actor
     */
    Page<AuditLog> findByTenantIdAndActor(String tenantId, String actor, Pageable pageable);

    /**
     * Find audit logs with filters
     */
    Page<AuditLog> findWithFilters(String tenantId, AuditLog.AuditAction action, String actorPattern,
                                   Instant from, Instant to, Pageable pageable);

    /**
     * Find audit logs by target type and ID
     */
    List<AuditLog> findByTenantIdAndTarget(String tenantId, String targetType, String targetId);

    /**
     * Find recent audit logs by tenant
     */
    Page<AuditLog> findByTenantId(String tenantId, Pageable pageable);

    /**
     * Find audit logs older than retention period
     */
    List<AuditLog> findLogsOlderThan(Instant cutoffTime);

    /**
     * Count audit logs by tenant and action
     */
    long countByTenantIdAndAction(String tenantId, AuditLog.AuditAction action);

    /**
     * Count audit logs by tenant in time range
     */
    long countByTenantIdAndTimeBetween(String tenantId, Instant from, Instant to);

    /**
     * Delete audit logs older than retention period
     */
    void deleteLogsOlderThan(Instant cutoffTime);
}
