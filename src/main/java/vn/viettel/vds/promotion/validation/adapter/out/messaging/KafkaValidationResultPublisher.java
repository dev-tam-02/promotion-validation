package vn.viettel.vds.promotion.validation.adapter.out.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.application.port.out.PublishValidationResultPort;
import vn.viettel.vds.promotion.schema.redemption.event.ValidateStackableDiscountResultEvent;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka adapter for publishing validation results.
 * <p>
 * This component implements the outbound port for publishing validation
 * result events to Kafka topics.
 * </p>
 * <p>
 * Architecture: Outbound Adapter (Messaging)
 * - Publishes validation result events to Kafka
 * - Handles DLQ publishing for failed messages
 * - Publishes error events for validation failures
 * - Async publishing with callback logging
 * - Message key generation for partitioning
 * </p>
 *
 * @author Validation Team
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaValidationResultPublisher implements PublishValidationResultPort {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.validation-results:promotion-validation-results}")
    private String validationResultsTopic;

    @Value("${kafka.topics.validation-results-dlq:promotion-validation-results-dlq}")
    private String validationResultsDlqTopic;

    @Value("${kafka.topics.validation-errors:promotion-validation-errors}")
    private String validationErrorsTopic;

    /**
     * Publishes validation result event to Kafka.
     * <p>
     * Features:
     * - Async publishing with CompletableFuture
     * - Message key based on idempotency key for ordered processing
     * - Success/failure callback logging
     * - Detailed metadata logging
     * </p>
     *
     * @param event validation result event to publish
     */
    @Override
    public void publishValidationResult(ValidateStackableDiscountResultEvent event) {
        String messageKey = event.getPayload().getIdempotencyKey();

        log.info("Publishing validation result: topic={}, idempotencyKey={}, decision={}",
                validationResultsTopic,
                messageKey,
                event.getPayload().getValidationSummary().getOverallValid() ? "VALID" : "INVALID");

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(validationResultsTopic, messageKey, event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Validation result published successfully: topic={}, partition={}, offset={}, idempotencyKey={}",
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset(),
                        messageKey);
            } else {
                log.error("Failed to publish validation result: topic={}, idempotencyKey={}, error={}",
                        validationResultsTopic,
                        messageKey,
                        ex.getMessage(),
                        ex);

                // Attempt to send to DLQ
                publishToDeadLetterQueue(validationResultsDlqTopic, event, ex.getMessage());
            }
        });
    }

    /**
     * Publishes failed message to dead letter queue.
     * <p>
     * Called when:
     * - Original event publishing fails
     * - Validation processing encounters unrecoverable errors
     * - Message format is invalid
     * </p>
     *
     * @param topic DLQ topic name
     * @param message original message that failed
     * @param errorReason failure reason
     */
    @Override
    public void publishToDeadLetterQueue(String topic, Object message, String errorReason) {
        String messageKey = extractMessageKey(message);

        log.warn("Publishing to DLQ: topic={}, messageKey={}, reason={}",
                topic,
                messageKey,
                errorReason);

        try {
            CompletableFuture<SendResult<String, Object>> future =
                    kafkaTemplate.send(topic, messageKey, message);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Message published to DLQ successfully: partition={}, offset={}, messageKey={}",
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset(),
                            messageKey);
                } else {
                    log.error("Failed to publish to DLQ: topic={}, messageKey={}, error={}",
                            topic,
                            messageKey,
                            ex.getMessage(),
                            ex);
                }
            });
        } catch (Exception e) {
            log.error("Exception publishing to DLQ: topic={}, messageKey={}, error={}",
                    topic,
                    messageKey,
                    e.getMessage(),
                    e);
        }
    }

    /**
     * Publishes validation error event.
     * <p>
     * Used for:
     * - Validation processing errors
     * - Invalid request errors
     * - System errors during validation
     * </p>
     *
     * @param correlationId correlation ID for tracking
     * @param errorCode error code
     * @param errorMessage error description
     * @param errorDetails additional error details
     */
    @Override
    public void publishValidationError(String correlationId, String errorCode,
                                       String errorMessage, String errorDetails) {
        log.warn("Publishing validation error: topic={}, correlationId={}, errorCode={}, message={}",
                validationErrorsTopic,
                correlationId,
                errorCode,
                errorMessage);

        try {
            // Create simple error event (can be enhanced with proper Avro schema later)
            ValidationErrorEvent errorEvent = new ValidationErrorEvent(
                    correlationId,
                    errorCode,
                    errorMessage,
                    errorDetails,
                    System.currentTimeMillis()
            );

            CompletableFuture<SendResult<String, Object>> future =
                    kafkaTemplate.send(validationErrorsTopic, correlationId, errorEvent);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Validation error published successfully: partition={}, offset={}, correlationId={}",
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset(),
                            correlationId);
                } else {
                    log.error("Failed to publish validation error: topic={}, correlationId={}, error={}",
                            validationErrorsTopic,
                            correlationId,
                            ex.getMessage(),
                            ex);
                }
            });
        } catch (Exception e) {
            log.error("Exception publishing validation error: topic={}, correlationId={}, error={}",
                    validationErrorsTopic,
                    correlationId,
                    e.getMessage(),
                    e);
        }
    }

    /**
     * Simple error event DTO.
     * TODO: Replace with proper Avro schema when available.
     */
    private record ValidationErrorEvent(
            String correlationId,
            String errorCode,
            String errorMessage,
            String errorDetails,
            long timestamp
    ) {}

    /**
     * Extracts message key from various message types.
     *
     * @param message the message object
     * @return message key for Kafka partitioning
     */
    private String extractMessageKey(Object message) {
        if (message instanceof ValidateStackableDiscountResultEvent event) {
            return event.getPayload().getIdempotencyKey();
        } else if (message instanceof vn.viettel.vds.promotion.schema.redemption.command.ValidateStackableDiscountCommand command) {
            return command.getPayload().getIdempotencyKey();
        }
        return "unknown-" + System.currentTimeMillis();
    }
}
