package vn.viettel.vds.promotion.validation.adapter.out.persistence.mongo.repository;

import com.promix.platform.autoconfigure.mongo.condition.ConditionalOnPromixMongo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import vn.viettel.vds.promotion.validation.domain.entity.AuditLog;

import java.time.Instant;
import java.util.List;

@Repository
@ConditionalOnPromixMongo
public interface AuditLogRepository extends MongoRepository<AuditLog, String> {

    /**
     * Find audit logs by tenant and action
     */
    Page<AuditLog> findByTenantIdAndActionOrderByAtDesc(String tenantId, AuditLog.AuditAction action, Pageable pageable);

    /**
     * Find audit logs by tenant and time range
     */
    @Query("{ 'tenantId': ?0, 'at': { $gte: ?1, $lte: ?2 } }")
    Page<AuditLog> findByTenantIdAndTimeBetween(String tenantId, Instant from, Instant to, Pageable pageable);

    /**
     * Find audit logs by tenant and actor
     */
    Page<AuditLog> findByTenantIdAndActorOrderByAtDesc(String tenantId, String actor, Pageable pageable);

    /**
     * Find audit logs with filters
     */
    @Query("{ 'tenantId': ?0, " +
            "$and: [ " +
            "  { $or: [ { 'action': { $exists: false } }, { 'action': ?1 } ] }, " +
            "  { $or: [ { 'actor': { $exists: false } }, { 'actor': { $regex: ?2, $options: 'i' } } ] }, " +
            "  { $or: [ { 'at': { $exists: false } }, { 'at': { $gte: ?3, $lte: ?4 } } ] } " +
            "] }")
    Page<AuditLog> findWithFilters(String tenantId, AuditLog.AuditAction action, String actorPattern, Instant from, Instant to, Pageable pageable);

    /**
     * Find audit logs by target type and ID
     */
    @Query("{ 'tenantId': ?0, 'target.type': ?1, 'target.id': ?2 }")
    List<AuditLog> findByTenantIdAndTargetTypeAndTargetIdOrderByAtDesc(String tenantId, String targetType, String targetId);

    /**
     * Find recent audit logs by tenant
     */
    Page<AuditLog> findByTenantIdOrderByAtDesc(String tenantId, Pageable pageable);

    /**
     * Find audit logs older than retention period for cleanup
     */
    @Query("{ 'at': { $lt: ?0 } }")
    List<AuditLog> findLogsOlderThan(Instant cutoffTime);

    /**
     * Count audit logs by tenant and action
     */
    long countByTenantIdAndAction(String tenantId, AuditLog.AuditAction action);

    /**
     * Count audit logs by tenant in time range
     */
    @Query(value = "{ 'tenantId': ?0, 'at': { $gte: ?1, $lte: ?2 } }", count = true)
    long countByTenantIdAndTimeBetween(String tenantId, Instant from, Instant to);

    /**
     * Delete audit logs older than retention period
     */
    @Query(value = "{ 'at': { $lt: ?0 } }", delete = true)
    void deleteLogsOlderThan(Instant cutoffTime);
}