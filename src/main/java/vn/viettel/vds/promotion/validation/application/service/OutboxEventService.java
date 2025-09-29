package vn.viettel.vds.promotion.validation.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.repository.OutboxEventRepository;
import vn.viettel.vds.promotion.validation.domain.entity.OutboxEvent;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class OutboxEventService {

    private static final Logger logger = LoggerFactory.getLogger(OutboxEventService.class);
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final int CLEANUP_RETENTION_DAYS = 7;

    private final OutboxEventRepository outboxEventRepository;

    public OutboxEventService(OutboxEventRepository outboxEventRepository) {
        this.outboxEventRepository = outboxEventRepository;
    }

    /**
     * Publish a rule published event
     */
    public void publishRulePublishedEvent(String tenantId, String ruleId, Integer ruleVersion,
                                         String code, String publishedBy, Instant publishedAt) {
        logger.info("Publishing RulePublished event: tenant={}, rule={}, version={}",
            tenantId, ruleId, ruleVersion);

        Map<String, Object> payload = Map.of(
            "ruleId", ruleId,
            "ruleVersion", ruleVersion,
            "code", code,
            "publishedBy", publishedBy,
            "publishedAt", publishedAt.toString()
        );

        createOutboxEvent(tenantId, OutboxEvent.EventType.RULE_PUBLISHED, payload);
    }

    /**
     * Publish an assignment changed event
     */
    public void publishAssignmentChangedEvent(String tenantId, String assignmentId, String ruleId,
                                            String subjectType, String subjectKey, Integer assignmentVersion,
                                            String action, String changedBy) {
        logger.info("Publishing AssignmentChanged event: tenant={}, assignment={}, action={}",
            tenantId, assignmentId, action);

        Map<String, Object> payload = Map.of(
            "assignmentId", assignmentId,
            "ruleId", ruleId,
            "subjectType", subjectType,
            "subjectKey", subjectKey,
            "assignmentVersion", assignmentVersion,
            "action", action,
            "changedBy", changedBy,
            "changedAt", Instant.now().toString()
        );

        createOutboxEvent(tenantId, OutboxEvent.EventType.ASSIGNMENT_CHANGED, payload);
    }

    /**
     * Create a generic outbox event
     */
    public OutboxEvent createOutboxEvent(String tenantId, OutboxEvent.EventType eventType, Map<String, Object> payload) {
        OutboxEvent event = new OutboxEvent();
        event.setTenantId(tenantId);
        event.setType(eventType);
        event.setPayload(payload);
        event.setStatus(OutboxEvent.EventStatus.PENDING);
        event.setAttempts(0);
        event.setCreatedAt(Instant.now());

        OutboxEvent saved = outboxEventRepository.save(event);

        logger.debug("Outbox event created: id={}, type={}", saved.getId(), eventType);
        return saved;
    }

    /**
     * Get pending events for processing
     */
    @Transactional(readOnly = true)
    public List<OutboxEvent> getPendingEvents(int limit) {
        return outboxEventRepository.findPendingEventsOrderByCreatedAt(PageRequest.of(0, limit));
    }

    /**
     * Mark event as sent
     */
    public void markEventAsSent(String eventId) {
        outboxEventRepository.findById(eventId).ifPresent(event -> {
            event.setStatus(OutboxEvent.EventStatus.SENT);
            event.setLastTriedAt(Instant.now());
            outboxEventRepository.save(event);

            logger.debug("Event marked as sent: id={}", eventId);
        });
    }

    /**
     * Mark event as failed and increment retry count
     */
    public void markEventAsFailed(String eventId, String error) {
        outboxEventRepository.findById(eventId).ifPresent(event -> {
            event.setStatus(OutboxEvent.EventStatus.FAILED);
            event.setAttempts(event.getAttempts() + 1);
            event.setLastTriedAt(Instant.now());

            // Add error information to payload
            if (event.getPayload() != null) {
                event.getPayload().put("lastError", error);
                event.getPayload().put("lastFailedAt", Instant.now().toString());
            }

            outboxEventRepository.save(event);

            logger.warn("Event marked as failed: id={}, attempts={}, error={}",
                eventId, event.getAttempts(), error);
        });
    }

    /**
     * Get failed events that can be retried
     */
    @Transactional(readOnly = true)
    public List<OutboxEvent> getRetryableFailedEvents(int limit) {
        return outboxEventRepository.findFailedEventsForRetry(MAX_RETRY_ATTEMPTS, PageRequest.of(0, limit));
    }

    /**
     * Reset failed event for retry
     */
    public void resetEventForRetry(String eventId) {
        outboxEventRepository.findById(eventId).ifPresent(event -> {
            if (event.getAttempts() < MAX_RETRY_ATTEMPTS) {
                event.setStatus(OutboxEvent.EventStatus.PENDING);
                outboxEventRepository.save(event);

                logger.info("Event reset for retry: id={}, attempts={}", eventId, event.getAttempts());
            } else {
                logger.warn("Event exceeded max retry attempts: id={}, attempts={}", eventId, event.getAttempts());
            }
        });
    }

    /**
     * Get event statistics for monitoring
     */
    @Transactional(readOnly = true)
    public EventStats getEventStats(String tenantId) {
        long pendingCount = outboxEventRepository.countByTenantIdAndStatus(tenantId, OutboxEvent.EventStatus.PENDING);
        long sentCount = outboxEventRepository.countByTenantIdAndStatus(tenantId, OutboxEvent.EventStatus.SENT);
        long failedCount = outboxEventRepository.countByTenantIdAndStatus(tenantId, OutboxEvent.EventStatus.FAILED);

        return new EventStats(pendingCount, sentCount, failedCount);
    }

    /**
     * Cleanup old sent events
     */
    public void cleanupOldEvents() {
        try {
            Instant cutoffTime = Instant.now().minusSeconds(CLEANUP_RETENTION_DAYS * 24 * 60 * 60);
            outboxEventRepository.deleteOldSentEvents(cutoffTime);

            logger.info("Cleaned up old outbox events older than {}", cutoffTime);
        } catch (Exception e) {
            logger.error("Error cleaning up old outbox events", e);
        }
    }

    /**
     * Get events by tenant with pagination
     */
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<OutboxEvent> getEventsByTenant(String tenantId,
                                                                               org.springframework.data.domain.Pageable pageable) {
        return outboxEventRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable);
    }

    /**
     * Get events by type for tenant
     */
    @Transactional(readOnly = true)
    public List<OutboxEvent> getEventsByType(String tenantId, OutboxEvent.EventType eventType) {
        return outboxEventRepository.findByTenantIdAndType(tenantId, eventType);
    }

    /**
     * Get outbox metrics for monitoring
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getOutboxMetrics() {
        try {
            long totalEvents = outboxEventRepository.count();
            long pendingEvents = outboxEventRepository.countByStatus(OutboxEvent.EventStatus.PENDING);
            long processingEvents = outboxEventRepository.countByStatus(OutboxEvent.EventStatus.PROCESSING);
            long sentEvents = outboxEventRepository.countByStatus(OutboxEvent.EventStatus.SENT);
            long failedEvents = outboxEventRepository.countByStatus(OutboxEvent.EventStatus.FAILED);

            // Check for stuck events (older than 1 hour in PENDING status)
            Instant oneHourAgo = Instant.now().minusSeconds(3600);
            long stuckEvents = outboxEventRepository.countByStatusAndCreatedAtBefore(
                OutboxEvent.EventStatus.PENDING, oneHourAgo);

            // Get oldest pending event
            List<OutboxEvent> oldestPending = outboxEventRepository.findByStatusOrderByCreatedAtAsc(
                OutboxEvent.EventStatus.PENDING, PageRequest.of(0, 1));

            Instant oldestPendingTime = oldestPending.isEmpty() ? null : oldestPending.get(0).getCreatedAt();

            return Map.of(
                "totalEvents", totalEvents,
                "pendingEvents", pendingEvents,
                "processingEvents", processingEvents,
                "sentEvents", sentEvents,
                "failedEvents", failedEvents,
                "stuckEvents", stuckEvents,
                "oldestPendingEvent", oldestPendingTime != null ? oldestPendingTime.toString() : null,
                "healthStatus", determineHealthStatus(stuckEvents, pendingEvents),
                "lastUpdated", Instant.now().toString()
            );

        } catch (Exception e) {
            logger.error("Error retrieving outbox metrics", e);
            return Map.of(
                "error", "Failed to retrieve metrics",
                "message", e.getMessage(),
                "lastUpdated", Instant.now().toString()
            );
        }
    }

    private String determineHealthStatus(long stuckEvents, long pendingEvents) {
        if (stuckEvents > 10) {
            return "CRITICAL";
        } else if (stuckEvents > 0 || pendingEvents > 100) {
            return "WARNING";
        } else {
            return "HEALTHY";
        }
    }

    /**
     * Process outbox events manually
     */
    public int processOutboxEvents() {
        try {
            List<OutboxEvent> pendingEvents = getPendingEvents(50);

            for (OutboxEvent event : pendingEvents) {
                event.setStatus(OutboxEvent.EventStatus.PROCESSING);
                event.setLastTriedAt(Instant.now());
                outboxEventRepository.save(event);
            }

            logger.info("Marked {} events for processing", pendingEvents.size());
            return pendingEvents.size();

        } catch (Exception e) {
            logger.error("Error processing outbox events", e);
            throw new RuntimeException("Failed to process outbox events", e);
        }
    }

    /**
     * Event statistics for monitoring
     */
    public static class EventStats {
        private final long pending;
        private final long sent;
        private final long failed;

        public EventStats(long pending, long sent, long failed) {
            this.pending = pending;
            this.sent = sent;
            this.failed = failed;
        }

        public long getPending() { return pending; }
        public long getSent() { return sent; }
        public long getFailed() { return failed; }
        public long getTotal() { return pending + sent + failed; }
    }
}