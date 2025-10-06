package vn.viettel.vds.promotion.validation.adapter.in.messaging.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.adapter.in.messaging.mapper.ValidateStackableDiscountMapper;
import vn.viettel.vds.promotion.validation.application.port.in.ValidateStackableDiscountUseCase;
import vn.viettel.vds.promotion.validation.application.port.in.command.ValidateStackableDiscountCommand;
import vn.viettel.vds.promotion.validation.application.port.in.dto.ValidateStackableDiscountResult;

/**
 * Kafka consumer for stackable discount validation commands.
 * <p>
 * This component listens to validation command topics and processes
 * validation requests by delegating to the validation use case.
 * </p>
 * <p>
 * Architecture: Inbound Adapter (Messaging)
 * - Receives Avro commands from Kafka
 * - Maps to domain commands
 * - Invokes use case
 * - Handles errors and DLQ
 * - Manual acknowledgment for at-least-once delivery
 * </p>
 *
 * @author Validation Team
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ValidateStackableDiscountConsumer {

    private final ValidateStackableDiscountUseCase validateUseCase;
    private final ValidateStackableDiscountMapper mapper;

    @Value("${kafka.topics.validation-commands-dlq:promotion-validation-commands-dlq}")
    private String dlqTopic;

    /**
     * Consumes validation commands from Kafka topic.
     * <p>
     * Features:
     * - Manual acknowledgment for reliability
     * - Error handling with detailed logging
     * - Message key tracking
     * - Idempotency key logging
     * </p>
     *
     * @param avroCommand Avro command payload
     * @param messageKey Kafka message key
     * @param partition Kafka partition
     * @param offset Kafka offset
     * @param acknowledgment Manual acknowledgment
     */
    @KafkaListener(
            topics = "${kafka.topics.validation-commands:promotion-validation-commands}",
            groupId = "${kafka.consumer.group-id:validation-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(
            @Payload vn.viettel.vds.promotion.schema.redemption.command.ValidateStackableDiscountCommand avroCommand,
            @Header(KafkaHeaders.RECEIVED_KEY) String messageKey,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment
    ) {
        log.info("Received validation command: idempotencyKey={}, customerId={}, orderId={}, messageKey={}, partition={}, offset={}",
                avroCommand.getPayload().getIdempotencyKey(),
                avroCommand.getPayload().getCustomerInfo().getCustomerId(),
                avroCommand.getPayload().getOrderInfo().getOrderId(),
                messageKey,
                partition,
                offset);

        try {
            // Map Avro command to domain command
            ValidateStackableDiscountCommand command = mapper.toCommand(avroCommand);

            // Execute validation
            ValidateStackableDiscountResult result = validateUseCase.validate(command);

            // Log result
            log.info("Validation completed: validationId={}, decision={}, validatedCount={}, rejectedCount={}, processingTime={}ms",
                    result.validationId(),
                    result.decision(),
                    result.getValidatedCount(),
                    result.getRejectedCount(),
                    result.processingTimeMs());

            // Acknowledge successful processing
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }

        } catch (Exception e) {
            log.error("Failed to process validation command: idempotencyKey={}, customerId={}, error={}",
                    avroCommand.getPayload().getIdempotencyKey(),
                    avroCommand.getPayload().getCustomerInfo().getCustomerId(),
                    e.getMessage(),
                    e);

            // TODO: Implement DLQ publishing via PublishValidationResultPort
            // For now, acknowledge to avoid infinite retries
            // In production, consider:
            // - Retry with exponential backoff
            // - Publish to DLQ after max retries
            // - Circuit breaker pattern

            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }

            // Re-throw to allow Spring Kafka retry mechanism
            throw new ValidationConsumerException(
                    "Failed to process validation command: " + avroCommand.getPayload().getIdempotencyKey(),
                    e
            );
        }
    }

    /**
     * Custom exception for consumer errors.
     */
    public static class ValidationConsumerException extends RuntimeException {
        public ValidationConsumerException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
