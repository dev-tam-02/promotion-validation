package vn.viettel.vds.promotion.validation.application.port.out;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.viettel.vds.promotion.validation.domain.enums.OutboxEventStatus;
import vn.viettel.vds.promotion.validation.domain.model.OutboxEvent;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Outbound port for OutboxEvent persistence.
 * Defines the interface for storing and retrieving outbox events.
 */
public interface OutboxEventPersistencePort {

    /**
     * Save an outbox event
     */
    OutboxEvent save(OutboxEvent event);

    /**
     * Find an outbox event by ID
     */
    Optional<OutboxEvent> findById(String id);

    /**
     * Find all outbox events by status
     */
    List<OutboxEvent> findByStatus(OutboxEventStatus status);

    /**
     * Find outbox events by status with pagination
     */
    Page<OutboxEvent> findByStatus(OutboxEventStatus status, Pageable pageable);

    /**
     * Find pending events for batch processing
     * Returns events with status PENDING or FAILED that can be retried
     */
    List<OutboxEvent> findPendingEvents(int limit);

    /**
     * Find events by tenant ID and status
     */
    List<OutboxEvent> findByTenantIdAndStatus(String tenantId, OutboxEventStatus status);

    /**
     * Find events created before a specific timestamp
     * Useful for cleanup operations
     */
    List<OutboxEvent> findEventsCreatedBefore(Instant timestamp);

    /**
     * Find published events older than retention period
     * For cleanup of successfully processed events
     */
    List<OutboxEvent> findPublishedEventsOlderThan(Instant timestamp);

    /**
     * Count events by status
     */
    long countByStatus(OutboxEventStatus status);

    /**
     * Count all events
     */
    long count();

    /**
     * Delete an outbox event by ID
     */
    void deleteById(String id);

    /**
     * Delete multiple outbox events
     */
    void deleteAll(List<OutboxEvent> events);

    /**
     * Delete events created before a specific timestamp
     */
    int deleteEventsCreatedBefore(Instant timestamp);

    /**
     * Check if an event exists by ID
     */
    boolean existsById(String id);

    /**
     * Find events by aggregate type and aggregate ID
     * Useful for finding all events related to a specific aggregate
     */
    List<OutboxEvent> findByAggregateTypeAndAggregateId(String aggregateType, String aggregateId);

    /**
     * Find events by event type
     */
    List<OutboxEvent> findByEventType(String eventType);
}
