package vn.viettel.vds.promotion.validation.adapter.config.batch;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.application.service.OutboxEventService;
import vn.viettel.vds.promotion.validation.domain.exception.OutboxCleanupException;

import java.time.Duration;
import java.time.Instant;

/**
 * Spring Batch Tasklet for cleaning up old published outbox events.
 * Removes successfully published events that are older than the retention period.
 */
@Component
@RequiredArgsConstructor
public class OutboxCleanupTasklet implements Tasklet {

    private static final Logger logger = LoggerFactory.getLogger(OutboxCleanupTasklet.class);

    private final OutboxEventService outboxEventService;

    @Value("${promix.outbox.cleanup.retention-period:7d}")
    private String retentionPeriod;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        logger.info("Starting outbox cleanup tasklet with retention period: {}", retentionPeriod);

        try {
            // Parse retention period (e.g., "7d" -> 7 days)
            Duration retention = parseRetentionPeriod(retentionPeriod);
            Instant cutoffTime = Instant.now().minus(retention);

            logger.info("Cleaning up published events older than: {}", cutoffTime);

            // Cleanup old published events
            int deletedCount = outboxEventService.cleanupPublishedEvents(cutoffTime);

            logger.info("Cleanup completed: deleted {} published events", deletedCount);

            // Update step contribution for monitoring
            contribution.incrementWriteCount(deletedCount);

            return RepeatStatus.FINISHED;

        } catch (Exception e) {
            throw new OutboxCleanupException("Error during outbox cleanup: " + e.getMessage(), e);
        }
    }

    /**
     * Parse retention period string (e.g., "7d", "24h", "30m")
     */
    private Duration parseRetentionPeriod(String period) {
        if (period == null || period.isBlank()) {
            return Duration.ofDays(7); // Default to 7 days
        }

        period = period.trim().toLowerCase();

        try {
            if (period.endsWith("d")) {
                long days = Long.parseLong(period.substring(0, period.length() - 1));
                return Duration.ofDays(days);
            } else if (period.endsWith("h")) {
                long hours = Long.parseLong(period.substring(0, period.length() - 1));
                return Duration.ofHours(hours);
            } else if (period.endsWith("m")) {
                long minutes = Long.parseLong(period.substring(0, period.length() - 1));
                return Duration.ofMinutes(minutes);
            } else {
                // Try to parse as ISO-8601 duration
                return Duration.parse(period);
            }
        } catch (Exception e) {
            logger.warn("Failed to parse retention period '{}', using default 7 days", period);
            return Duration.ofDays(7);
        }
    }
}