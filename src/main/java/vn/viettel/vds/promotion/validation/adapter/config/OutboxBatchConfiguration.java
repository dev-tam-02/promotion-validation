package vn.viettel.vds.promotion.validation.adapter.config;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import vn.viettel.vds.promotion.validation.adapter.config.batch.OutboxCleanupTasklet;
import vn.viettel.vds.promotion.validation.adapter.config.batch.OutboxEventItemProcessor;
import vn.viettel.vds.promotion.validation.adapter.config.batch.OutboxEventItemReader;
import vn.viettel.vds.promotion.validation.adapter.config.batch.OutboxEventItemWriter;
import vn.viettel.vds.promotion.validation.domain.model.OutboxEvent;

/**
 * Spring Batch configuration for outbox event processing.
 * Configures jobs and steps for processing and cleaning up outbox events.
 */
@Configuration
@RequiredArgsConstructor
public class OutboxBatchConfiguration {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final OutboxEventItemReader itemReader;
    private final OutboxEventItemProcessor itemProcessor;
    private final OutboxEventItemWriter itemWriter;
    private final OutboxCleanupTasklet cleanupTasklet;

    @Value("${promix.outbox.batch.chunk-size:100}")
    private int chunkSize;

    /**
     * Job for processing outbox events
     */
    @Bean
    public Job processOutboxEventsJob() {
        return new JobBuilder("processOutboxEventsJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(processOutboxEventsStep())
                .build();
    }

    /**
     * Step for processing outbox events
     */
    @Bean
    public Step processOutboxEventsStep() {
        return new StepBuilder("processOutboxEventsStep", jobRepository)
                .<OutboxEvent, OutboxEvent>chunk(chunkSize, transactionManager)
                .reader(itemReader)
                .processor(itemProcessor)
                .writer(itemWriter)
                .build();
    }

    /**
     * Job for cleaning up old outbox events
     */
    @Bean
    public Job cleanupOutboxEventsJob() {
        return new JobBuilder("cleanupOutboxEventsJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(cleanupOutboxEventsStep())
                .build();
    }

    /**
     * Step for cleaning up old outbox events
     */
    @Bean
    public Step cleanupOutboxEventsStep() {
        return new StepBuilder("cleanupOutboxEventsStep", jobRepository)
                .tasklet(cleanupTasklet, transactionManager)
                .build();
    }
}
