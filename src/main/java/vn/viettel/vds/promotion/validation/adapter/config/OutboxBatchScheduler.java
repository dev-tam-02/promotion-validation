package vn.viettel.vds.promotion.validation.adapter.config;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Scheduler for outbox batch jobs.
 * Triggers outbox event processing and cleanup jobs at configured intervals.
 */
@Configuration
@EnableScheduling
@RequiredArgsConstructor
@ConditionalOnProperty(name = "promix.outbox.batch.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxBatchScheduler {

    private static final Logger logger = LoggerFactory.getLogger(OutboxBatchScheduler.class);

    private final JobLauncher jobLauncher;
    private final Job processOutboxEventsJob;
    private final Job cleanupOutboxEventsJob;

    @Value("${promix.outbox.batch.enabled:true}")
    private boolean batchEnabled;

    @Value("${promix.outbox.cleanup.enabled:true}")
    private boolean cleanupEnabled;

    /**
     * Process outbox events at configured interval
     */
    @Scheduled(fixedDelayString = "${promix.outbox.batch.interval-ms:60000}")
    public void processOutboxEvents() {
        if (!batchEnabled) {
            logger.debug("Outbox batch processing is disabled");
            return;
        }

        try {
            logger.debug("Starting scheduled outbox event processing");

            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(processOutboxEventsJob, jobParameters);

            logger.debug("Completed scheduled outbox event processing");

        } catch (Exception e) {
            logger.error("Error during scheduled outbox event processing", e);
        }
    }

    /**
     * Cleanup old outbox events at configured interval
     */
    @Scheduled(fixedDelayString = "${promix.outbox.cleanup.interval-ms:21600000}")
    public void cleanupOutboxEvents() {
        if (!cleanupEnabled) {
            logger.debug("Outbox cleanup is disabled");
            return;
        }

        try {
            logger.debug("Starting scheduled outbox event cleanup");

            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(cleanupOutboxEventsJob, jobParameters);

            logger.debug("Completed scheduled outbox event cleanup");

        } catch (Exception e) {
            logger.error("Error during scheduled outbox event cleanup", e);
        }
    }
}
