package vn.viettel.vds.promotion.validation.adapter.out.persistence.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import vn.viettel.vds.promotion.validation.domain.entity.OutboxEvent;

import java.time.Instant;
import java.util.List;

@Repository
public interface OutboxEventRepository extends MongoRepository<OutboxEvent, String> {

    /**
     * Find pending events ordered by creation time
     */
    @Query("{ 'status': 'PENDING' }")
    List<OutboxEvent> findPendingEventsOrderByCreatedAt(Pageable pageable);

    /**
     * Find events by tenant and status
     */
    List<OutboxEvent> findByTenantIdAndStatus(String tenantId, OutboxEvent.EventStatus status);

    /**
     * Find events by tenant, status and creation time range
     */
    @Query("{ 'tenantId': ?0, 'status': ?1, 'createdAt': { $gte: ?2, $lte: ?3 } }")
    Page<OutboxEvent> findByTenantIdAndStatusAndCreatedAtBetween(String tenantId, OutboxEvent.EventStatus status, Instant from, Instant to, Pageable pageable);

    /**
     * Find failed events with retry attempts less than max
     */
    @Query("{ 'status': 'FAILED', 'attempts': { $lt: ?0 } }")
    List<OutboxEvent> findFailedEventsForRetry(int maxAttempts, Pageable pageable);

    /**
     * Find events by type and tenant
     */
    List<OutboxEvent> findByTenantIdAndType(String tenantId, OutboxEvent.EventType type);

    /**
     * Find events older than specified time for cleanup
     */
    @Query("{ 'status': { $in: ['SENT', 'FAILED'] }, 'createdAt': { $lt: ?0 } }")
    List<OutboxEvent> findEventsOlderThan(Instant cutoffTime);

    /**
     * Count pending events by tenant
     */
    long countByTenantIdAndStatus(String tenantId, OutboxEvent.EventStatus status);

    /**
     * Find events by tenant with pagination and ordering
     */
    Page<OutboxEvent> findByTenantIdOrderByCreatedAtDesc(String tenantId, Pageable pageable);

    /**
     * Delete events older than specified time
     */
    @Query(value = "{ 'status': { $in: ['SENT'] }, 'createdAt': { $lt: ?0 } }", delete = true)
    void deleteOldSentEvents(Instant cutoffTime);

    /**
     * Count events by status
     */
    long countByStatus(OutboxEvent.EventStatus status);

    /**
     * Count events by status and creation time
     */
    long countByStatusAndCreatedAtBefore(OutboxEvent.EventStatus status, Instant createdAt);

    /**
     * Find events by status ordered by creation time
     */
    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(OutboxEvent.EventStatus status, Pageable pageable);

    /**
     * Find old processed events for cleanup
     */
    @Query("{ 'status': { $in: ['SENT', 'COMPLETED'] }, 'createdAt': { $lt: ?0 } }")
    List<OutboxEvent> findOldProcessedEvents(Instant cutoffTime, Pageable pageable);
}