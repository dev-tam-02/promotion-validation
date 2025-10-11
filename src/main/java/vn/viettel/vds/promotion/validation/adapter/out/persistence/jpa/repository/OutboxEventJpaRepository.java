package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.OutboxEventEntity;
import vn.viettel.vds.promotion.validation.domain.enums.OutboxEventStatus;

import java.time.Instant;
import java.util.List;

/**
 * JPA repository for OutboxEvent entities.
 * Provides data access methods for outbox event persistence.
 */
@Repository
public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventEntity, String> {

    /**
     * Find events by status
     */
    List<OutboxEventEntity> findByStatus(OutboxEventStatus status);

    /**
     * Find events by status with pagination
     */
    Page<OutboxEventEntity> findByStatus(OutboxEventStatus status, Pageable pageable);

    /**
     * Find pending events for batch processing
     * Includes PENDING events and FAILED events that haven't exceeded max attempts
     */
    @Query("SELECT e FROM OutboxEventEntity e WHERE " +
            "(e.status = 'PENDING' OR (e.status = 'FAILED' AND e.attempts < e.maxAttempts)) " +
            "ORDER BY e.createdAt ASC")
    List<OutboxEventEntity> findPendingEvents(Pageable pageable);

    /**
     * Find events created before a specific timestamp
     */
    List<OutboxEventEntity> findByCreatedAtBefore(Instant timestamp);

    /**
     * Find published events older than retention period
     */
    @Query("SELECT e FROM OutboxEventEntity e WHERE e.status = 'PUBLISHED' AND e.publishedAt < :timestamp")
    List<OutboxEventEntity> findPublishedEventsOlderThan(@Param("timestamp") Instant timestamp);

    /**
     * Count events by status
     */
    long countByStatus(OutboxEventStatus status);

    /**
     * Delete events created before a specific timestamp
     */
    @Modifying
    @Query("DELETE FROM OutboxEventEntity e WHERE e.createdAt < :timestamp")
    int deleteByCreatedAtBefore(@Param("timestamp") Instant timestamp);

    /**
     * Find events by aggregate type and aggregate ID
     */
    List<OutboxEventEntity> findByAggregateTypeAndAggregateId(String aggregateType, String aggregateId);

    /**
     * Find events by event type
     */
    List<OutboxEventEntity> findByEventType(String eventType);

    /**
     * Count pending events (PENDING + retryable FAILED)
     */
    @Query("SELECT COUNT(e) FROM OutboxEventEntity e WHERE " +
            "e.status = 'PENDING' OR (e.status = 'FAILED' AND e.attempts < e.maxAttempts)")
    long countPendingEvents();

    /**
     * Count processing events
     */
    @Query("SELECT COUNT(e) FROM OutboxEventEntity e WHERE e.status = 'PROCESSING'")
    long countProcessingEvents();

    /**
     * Count published events
     */
    @Query("SELECT COUNT(e) FROM OutboxEventEntity e WHERE e.status = 'PUBLISHED'")
    long countPublishedEvents();

    /**
     * Count failed events (that can still be retried)
     */
    @Query("SELECT COUNT(e) FROM OutboxEventEntity e WHERE e.status = 'FAILED' AND e.attempts < e.maxAttempts")
    long countFailedEvents();

    /**
     * Count dead letter events
     */
    @Query("SELECT COUNT(e) FROM OutboxEventEntity e WHERE e.status = 'DEAD_LETTER'")
    long countDeadLetterEvents();
}
