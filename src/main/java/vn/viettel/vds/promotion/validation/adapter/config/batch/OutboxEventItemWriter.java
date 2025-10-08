package vn.viettel.vds.promotion.validation.adapter.config.batch;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.application.port.out.OutboxEventPersistencePort;
import vn.viettel.vds.promotion.validation.domain.model.OutboxEvent;

/**
 * Spring Batch ItemWriter for persisting processed outbox events.
 * Updates the status of events after processing.
 */
@Component
@RequiredArgsConstructor
public class OutboxEventItemWriter implements ItemWriter<OutboxEvent> {

    private static final Logger logger = LoggerFactory.getLogger(OutboxEventItemWriter.class);

    private final OutboxEventPersistencePort outboxEventPersistencePort;

    @Override
    public void write(Chunk<? extends OutboxEvent> chunk) {
        logger.debug("Writing {} processed outbox events", chunk.size());

        for (OutboxEvent event : chunk.getItems()) {
            try {
                outboxEventPersistencePort.save(event);
                logger.debug("Saved outbox event: id={}, status={}", event.getId(), event.getStatus());
            } catch (Exception e) {
                logger.error("Failed to save outbox event: id={}", event.getId(), e);
                // In a production system, you might want to handle this differently
                // For now, we'll let the exception propagate to trigger batch retry
                throw new RuntimeException("Failed to save outbox event: " + event.getId(), e);
            }
        }

        logger.info("Successfully wrote {} outbox events", chunk.size());
    }
}
