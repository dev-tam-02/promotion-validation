package vn.viettel.vds.promotion.validation.adapter.out.messaging;

import vn.viettel.vds.promotion.validation.domain.model.OutboxEvent;

/**
 * Event publisher interface for outbox pattern.
 * Implementations publish events to various destinations (HTTP, Kafka, etc.).
 */
public interface EventPublisher {

    /**
     * Check if this publisher supports the given destination
     *
     * @param destination The destination URL or identifier
     * @return true if this publisher can handle the destination
     */
    boolean supports(String destination);

    /**
     * Publish an outbox event to its destination
     *
     * @param event The outbox event to publish
     * @throws RuntimeException if publishing fails
     */
    void publish(OutboxEvent event);
}
