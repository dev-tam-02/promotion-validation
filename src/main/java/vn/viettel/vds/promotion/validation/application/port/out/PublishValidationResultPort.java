package vn.viettel.vds.promotion.validation.application.port.out;

import vn.viettel.vds.promotion.schema.redemption.event.ValidateStackableDiscountResultEvent;

/**
 * Output port for publishing validation results to message broker.
 * <p>
 * This port abstracts the messaging infrastructure and allows the application
 * layer to publish validation results without depending on specific Kafka or
 * messaging implementation details.
 * </p>
 *
 * @author Validation Team
 * @since 1.0.0
 */
public interface PublishValidationResultPort {

    /**
     * Publishes a validation result event to the configured Kafka topic.
     * <p>
     * The event will be consumed by the redemption service to proceed with
     * actual discount redemption based on the validation outcome.
     * </p>
     *
     * @param event the validation result event containing validation decision and details
     * @throws IllegalArgumentException   if event is null
     * @throws ValidationPublishException if publishing fails
     */
    void publishValidationResult(ValidateStackableDiscountResultEvent event);

    /**
     * Publishes a failed message to the Dead Letter Queue (DLQ) for manual investigation.
     * <p>
     * Messages that cannot be processed successfully are sent to DLQ to prevent
     * message loss and enable debugging and recovery.
     * </p>
     *
     * @param topic       the DLQ topic name
     * @param message     the original message that failed processing
     * @param errorReason the reason for failure
     * @throws IllegalArgumentException if any parameter is null
     */
    void publishToDeadLetterQueue(String topic, Object message, String errorReason);

    /**
     * Publishes a validation error event when validation process encounters
     * an unexpected error or exception.
     *
     * @param correlationId the correlation ID for tracking
     * @param errorCode     the error code
     * @param errorMessage  the error message
     * @param errorDetails  additional error details
     */
    void publishValidationError(String correlationId, String errorCode,
                                String errorMessage, String errorDetails);
}
