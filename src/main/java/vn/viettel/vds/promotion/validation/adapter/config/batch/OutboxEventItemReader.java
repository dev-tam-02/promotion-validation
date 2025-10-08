package vn.viettel.vds.promotion.validation.adapter.config.batch;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.application.port.out.OutboxEventPersistencePort;
import vn.viettel.vds.promotion.validation.domain.model.OutboxEvent;

import java.util.Iterator;
import java.util.List;

/**
 * Spring Batch ItemReader for reading pending outbox events.
 * Reads events with status PENDING or FAILED (that can be retried).
 */
@Component
@RequiredArgsConstructor
public class OutboxEventItemReader implements ItemReader<OutboxEvent> {

    private static final Logger logger = LoggerFactory.getLogger(OutboxEventItemReader.class);

    private final OutboxEventPersistencePort outboxEventPersistencePort;

    @Value("${promix.outbox.batch.page-size:1000}")
    private int pageSize;

    private Iterator<OutboxEvent> eventIterator;
    private boolean initialized = false;

    @Override
    public OutboxEvent read() {
        if (!initialized) {
            initialize();
        }

        if (eventIterator != null && eventIterator.hasNext()) {
            OutboxEvent event = eventIterator.next();
            logger.debug("Read outbox event: id={}, type={}, attempts={}/{}",
                    event.getId(), event.getEventType(), event.getAttempts(), event.getMaxAttempts());
            return event;
        }

        // No more events to process
        logger.debug("No more events to read in this batch");
        return null;
    }

    /**
     * Initialize the reader by fetching pending events
     */
    private void initialize() {
        logger.info("Initializing OutboxEventItemReader with page size: {}", pageSize);

        List<OutboxEvent> pendingEvents = outboxEventPersistencePort.findPendingEvents(pageSize);
        logger.info("Found {} pending events to process", pendingEvents.size());

        this.eventIterator = pendingEvents.iterator();
        this.initialized = true;
    }

    /**
     * Reset the reader for the next job execution
     * This is called automatically by Spring Batch between job runs
     */
    public void reset() {
        logger.debug("Resetting OutboxEventItemReader");
        this.eventIterator = null;
        this.initialized = false;
    }
}
