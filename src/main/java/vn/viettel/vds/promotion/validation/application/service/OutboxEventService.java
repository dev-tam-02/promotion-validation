package vn.viettel.vds.promotion.validation.application.service;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.viettel.vds.promotion.validation.application.port.out.OutboxEventPersistencePort;
import vn.viettel.vds.promotion.validation.domain.enums.OutboxEventStatus;
import vn.viettel.vds.promotion.validation.domain.model.OutboxEvent;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Application service for managing outbox events.
 * Replaces the promix-outbox OutboxService with custom implementation.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class OutboxEventService {

    private static final Logger logger = LoggerFactory.getLogger(OutboxEventService.class);

    private final OutboxEventPersistencePort outboxEventPersistencePort;

    /**
     * Create a new outbox event
     */
    public OutboxEvent createEvent(
            String aggregateType,
            String aggregateId,
            String eventType,
            Map<String, Object> payload,
            String destination,
            Map<String, Object> metadata,
            int maxAttempts
    ) {
        logger.debug("Creating outbox event: aggregateType={}, aggregateId={}, eventType={}",
                aggregateType, aggregateId, eventType);

        String eventId = UUID.randomUUID().toString();
        Instant now = Instant.now();

        // Extract tenant ID from metadata if available
        String tenantId = metadata != null ? (String) metadata.get("tenantId") : null;

        OutboxEvent event = OutboxEvent.builder()
                .id(eventId)
                .tenantId(tenantId)
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .eventType(eventType)
                .payload(payload)
                .destination(destination)
                .metadata(metadata)
                .status(OutboxEventStatus.PENDING)
                .attempts(0)
                .maxAttempts(maxAttempts)
                .createdAt(now)
                .version(0L)
                .build();

        OutboxEvent saved = outboxEventPersistencePort.save(event);
        logger.info("Created outbox event: id={}, type={}", saved.getId(), saved.getEventType());

        return saved;
    }

    /**
     * Get outbox event by ID
     */
    @Transactional(readOnly = true)
    public Optional<OutboxEvent> getEventById(String id) {
        return outboxEventPersistencePort.findById(id);
    }

    /**
     * Get events by status
     */
    @Transactional(readOnly = true)
    public List<OutboxEvent> getEventsByStatus(OutboxEventStatus status) {
        return outboxEventPersistencePort.findByStatus(status);
    }

    /**
     * Get pending events for batch processing
     */
    @Transactional(readOnly = true)
    public List<OutboxEvent> getPendingEvents(int limit) {
        return outboxEventPersistencePort.findPendingEvents(limit);
    }

    /**
     * Mark event as processing
     */
    public OutboxEvent markAsProcessing(String eventId) {
        OutboxEvent event = outboxEventPersistencePort.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));

        OutboxEvent updated = event.markAsProcessing()
                .withIncrementedAttempts();

        return outboxEventPersistencePort.save(updated);
    }

    /**
     * Mark event as published
     */
    public OutboxEvent markAsPublished(String eventId) {
        OutboxEvent event = outboxEventPersistencePort.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));

        OutboxEvent updated = event.markAsPublished();
        OutboxEvent saved = outboxEventPersistencePort.save(updated);

        logger.info("Marked event as published: id={}", eventId);
        return saved;
    }

    /**
     * Mark event as failed
     */
    public OutboxEvent markAsFailed(String eventId, String errorMessage) {
        OutboxEvent event = outboxEventPersistencePort.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));

        OutboxEvent updated = event.markAsFailed(errorMessage);
        OutboxEvent saved = outboxEventPersistencePort.save(updated);

        if (saved.getStatus() == OutboxEventStatus.DEAD_LETTER) {
            logger.warn("Event moved to dead letter: id={}, error={}", eventId, errorMessage);
        } else {
            logger.warn("Event marked as failed: id={}, attempts={}/{}, error={}",
                    eventId, saved.getAttempts(), saved.getMaxAttempts(), errorMessage);
        }

        return saved;
    }

    /**
     * Get outbox statistics
     */
    @Transactional(readOnly = true)
    public OutboxStatistics getStatistics() {
        long pendingCount = outboxEventPersistencePort.countByStatus(OutboxEventStatus.PENDING);
        long processingCount = outboxEventPersistencePort.countByStatus(OutboxEventStatus.PROCESSING);
        long publishedCount = outboxEventPersistencePort.countByStatus(OutboxEventStatus.PUBLISHED);
        long failedCount = outboxEventPersistencePort.countByStatus(OutboxEventStatus.FAILED);
        long deadLetterCount = outboxEventPersistencePort.countByStatus(OutboxEventStatus.DEAD_LETTER);
        long totalCount = outboxEventPersistencePort.count();

        return OutboxStatistics.builder()
                .pendingCount(pendingCount)
                .processingCount(processingCount)
                .publishedCount(publishedCount)
                .failedCount(failedCount)
                .deadLetterCount(deadLetterCount)
                .totalCount(totalCount)
                .build();
    }

    /**
     * Cleanup old published events
     */
    public int cleanupPublishedEvents(Instant olderThan) {
        List<OutboxEvent> oldEvents = outboxEventPersistencePort.findPublishedEventsOlderThan(olderThan);

        if (oldEvents.isEmpty()) {
            logger.debug("No published events to cleanup");
            return 0;
        }

        outboxEventPersistencePort.deleteAll(oldEvents);
        logger.info("Cleaned up {} published events older than {}", oldEvents.size(), olderThan);

        return oldEvents.size();
    }

    /**
     * Outbox statistics data model
     */
    @Value
    @Builder
    public static class OutboxStatistics {
        long pendingCount;
        long processingCount;
        long publishedCount;
        long failedCount;
        long deadLetterCount;
        long totalCount;
    }
}
