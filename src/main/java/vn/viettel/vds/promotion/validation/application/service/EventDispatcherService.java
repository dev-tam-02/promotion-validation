package vn.viettel.vds.promotion.validation.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.repository.OutboxEventRepository;
import vn.viettel.vds.promotion.validation.domain.entity.OutboxEvent;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class EventDispatcherService {

    private static final Logger logger = LoggerFactory.getLogger(EventDispatcherService.class);
    private static final int BATCH_SIZE = 50;
    private static final int RETRY_BATCH_SIZE = 20;

    private final OutboxEventService outboxEventService;
    private final EventPublisherService eventPublisherService;
    private final OutboxEventRepository outboxEventRepository;

    public EventDispatcherService(OutboxEventService outboxEventService,
                                 EventPublisherService eventPublisherService,
                                 OutboxEventRepository outboxEventRepository) {
        this.outboxEventService = outboxEventService;
        this.eventPublisherService = eventPublisherService;
        this.outboxEventRepository = outboxEventRepository;
    }

    /**
     * Scheduled task to process pending events
     */
    @Scheduled(fixedDelay = 5000) // Every 5 seconds
    public void processPendingEvents() {
        try {
            List<OutboxEvent> pendingEvents = outboxEventService.getPendingEvents(BATCH_SIZE);

            if (!pendingEvents.isEmpty()) {
                logger.info("Processing {} pending outbox events", pendingEvents.size());

                for (OutboxEvent event : pendingEvents) {
                    processEventAsync(event);
                }
            }
        } catch (Exception e) {
            logger.error("Error processing pending events", e);
        }
    }

    /**
     * Scheduled task to retry failed events
     */
    @Scheduled(fixedDelay = 30000) // Every 30 seconds
    public void retryFailedEvents() {
        try {
            List<OutboxEvent> failedEvents = outboxEventService.getRetryableFailedEvents(RETRY_BATCH_SIZE);

            if (!failedEvents.isEmpty()) {
                logger.info("Retrying {} failed outbox events", failedEvents.size());

                for (OutboxEvent event : failedEvents) {
                    outboxEventService.resetEventForRetry(event.getId());
                }
            }
        } catch (Exception e) {
            logger.error("Error retrying failed events", e);
        }
    }

    /**
     * Scheduled cleanup of old events
     */
    @Scheduled(cron = "0 0 2 * * ?") // Daily at 2 AM
    public void cleanupOldEvents() {
        try {
            logger.info("Starting cleanup of old outbox events");
            outboxEventService.cleanupOldEvents();
        } catch (Exception e) {
            logger.error("Error during scheduled cleanup", e);
        }
    }

    /**
     * Process a single event asynchronously
     */
    @Async("taskExecutor")
    public CompletableFuture<Void> processEventAsync(OutboxEvent event) {
        return CompletableFuture.runAsync(() -> processEvent(event));
    }

    /**
     * Process a single event
     */
    private void processEvent(OutboxEvent event) {
        logger.debug("Processing outbox event: id={}, type={}", event.getId(), event.getType());

        try {
            boolean success = false;

            switch (event.getType()) {
                case RULE_PUBLISHED:
                    success = eventPublisherService.publishRulePublishedEvent(event);
                    break;
                case ASSIGNMENT_CHANGED:
                    success = eventPublisherService.publishAssignmentChangedEvent(event);
                    break;
                default:
                    logger.warn("Unknown event type: {}", event.getType());
                    outboxEventService.markEventAsFailed(event.getId(), "Unknown event type");
                    return;
            }

            if (success) {
                outboxEventService.markEventAsSent(event.getId());
                logger.debug("Event processed successfully: id={}", event.getId());
            } else {
                outboxEventService.markEventAsFailed(event.getId(), "Event publishing failed");
            }

        } catch (Exception e) {
            logger.error("Error processing event: id={}", event.getId(), e);
            outboxEventService.markEventAsFailed(event.getId(), "Processing error: " + e.getMessage());
        }
    }

    /**
     * Manual trigger for event processing (for testing/admin)
     */
    public ProcessingStats manualProcessEvents(int maxEvents) {
        logger.info("Manual processing triggered for max {} events", maxEvents);

        List<OutboxEvent> pendingEvents = outboxEventService.getPendingEvents(maxEvents);
        int processedCount = 0;
        int successCount = 0;
        int failureCount = 0;

        for (OutboxEvent event : pendingEvents) {
            try {
                processEvent(event);
                processedCount++;

                // Check if event was successfully sent
                if (outboxEventRepository.findById(event.getId())
                    .map(e -> e.getStatus() == OutboxEvent.EventStatus.SENT)
                    .orElse(false)) {
                    successCount++;
                } else {
                    failureCount++;
                }
            } catch (Exception e) {
                logger.error("Error in manual processing of event: {}", event.getId(), e);
                failureCount++;
                processedCount++;
            }
        }

        ProcessingStats stats = new ProcessingStats(processedCount, successCount, failureCount);
        logger.info("Manual processing completed: {}", stats);
        return stats;
    }

    /**
     * Get current processing status
     */
    public ProcessingStatus getProcessingStatus() {
        // This could be enhanced with actual metrics from a metrics registry
        return new ProcessingStatus(
            true, // assume healthy for now
            System.currentTimeMillis(),
            "Active"
        );
    }


    /**
     * Processing statistics
     */
    public static class ProcessingStats {
        private final int totalProcessed;
        private final int successful;
        private final int failed;

        public ProcessingStats(int totalProcessed, int successful, int failed) {
            this.totalProcessed = totalProcessed;
            this.successful = successful;
            this.failed = failed;
        }

        public int getTotalProcessed() { return totalProcessed; }
        public int getSuccessful() { return successful; }
        public int getFailed() { return failed; }

        @Override
        public String toString() {
            return String.format("ProcessingStats{total=%d, success=%d, failed=%d}",
                totalProcessed, successful, failed);
        }
    }

    /**
     * Processing status information
     */
    public static class ProcessingStatus {
        private final boolean healthy;
        private final long lastProcessedAt;
        private final String status;

        public ProcessingStatus(boolean healthy, long lastProcessedAt, String status) {
            this.healthy = healthy;
            this.lastProcessedAt = lastProcessedAt;
            this.status = status;
        }

        public boolean isHealthy() { return healthy; }
        public long getLastProcessedAt() { return lastProcessedAt; }
        public String getStatus() { return status; }
    }
}