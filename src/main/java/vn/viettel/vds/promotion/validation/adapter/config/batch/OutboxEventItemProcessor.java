package vn.viettel.vds.promotion.validation.adapter.config.batch;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.adapter.out.messaging.EventPublisher;
import vn.viettel.vds.promotion.validation.domain.model.OutboxEvent;

/**
 * Spring Batch ItemProcessor for processing outbox events.
 * Publishes events to their destinations and updates their status.
 */
@Component
@RequiredArgsConstructor
public class OutboxEventItemProcessor implements ItemProcessor<OutboxEvent, OutboxEvent> {

    private static final Logger logger = LoggerFactory.getLogger(OutboxEventItemProcessor.class);

    private final EventPublisher eventPublisher;

    @Override
    public OutboxEvent process(OutboxEvent event) {
        logger.debug("Processing outbox event: id={}, type={}, destination={}",
                event.getId(), event.getEventType(), event.getDestination());

        try {
            // Mark as processing and increment attempts
            OutboxEvent processingEvent = event.markAsProcessing().withIncrementedAttempts();

            // Check if publisher supports this destination
            if (!eventPublisher.supports(processingEvent.getDestination())) {
                String error = "No publisher supports destination: " + processingEvent.getDestination();
                logger.warn("Event processing failed: id={}, error={}", event.getId(), error);
                return processingEvent.markAsFailed(error);
            }

            // Publish the event
            eventPublisher.publish(processingEvent);

            // Mark as published
            OutboxEvent publishedEvent = processingEvent.markAsPublished();
            logger.info("Successfully processed outbox event: id={}, type={}",
                    event.getId(), event.getEventType());

            return publishedEvent;

        } catch (Exception e) {
            String errorMessage = "Event processing failed: " + e.getMessage();
            logger.error("Error processing outbox event: id={}", event.getId(), e);

            // Mark as failed (will auto-move to dead letter if max attempts exceeded)
            OutboxEvent failedEvent = event
                    .withIncrementedAttempts()
                    .markAsFailed(errorMessage);

            return failedEvent;
        }
    }
}
