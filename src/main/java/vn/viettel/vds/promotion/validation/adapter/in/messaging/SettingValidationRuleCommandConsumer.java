package vn.viettel.vds.promotion.validation.adapter.in.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.application.service.SettingValidationRuleCommandHandler;
import vn.viettel.vds.promotion.validation.command.SettingValidationRuleCommand;
import vn.viettel.vds.promotion.validation.domain.exception.CommandProcessingException;

/**
 * Kafka consumer for SettingValidationRuleCommand messages with batch processing.
 *
 * NOTE: This consumer is deprecated in favor of ValidationRuleCommandConsumer
 * which uses polymorphic pattern matching. Keep this for backward compatibility
 * if needed, but ValidationRuleCommandConsumer is the recommended approach.
 */
@Component
public class SettingValidationRuleCommandConsumer {

    private static final Logger logger = LoggerFactory.getLogger(SettingValidationRuleCommandConsumer.class);

    private final SettingValidationRuleCommandHandler commandHandler;

    public SettingValidationRuleCommandConsumer(
            SettingValidationRuleCommandHandler commandHandler) {
        this.commandHandler = commandHandler;
    }

    /**
     * Consume single SettingValidationRuleCommand from Kafka topic.
     *
     * Error handling strategy:
     * - Let exceptions propagate naturally to promix-messaging
     * - No try-catch - framework handles error classification
     * - ValidationException → DLQ immediately (non-retryable)
     * - Other exceptions → Retry logic then DLQ
     */
    @KafkaListener(
            topics = "${kafka.topics.setting-validation-rule-events:promotion_validation_event}",
            groupId = "${kafka.consumer.group-id:promotion-validation-consumer}-setting",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleSettingValidationRuleCommand(
            @Payload SettingValidationRuleCommand command,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        logger.info("Received SettingValidationRuleCommand from topic={}, partition={}, offset={}",
            topic, partition, offset);

        // Null check - deserialization can fail and return null
        if (command == null) {
            logger.error("Received null command at offset={}", offset);
            throw new IllegalArgumentException("Received null command from Kafka");
        }

        logger.debug("Processing command: commandId={}, type={}, offset={}",
            command.getId(), command.getType(), offset);

        // Handle the command - let exceptions propagate
        boolean success = commandHandler.handleCommand(command);

        if (!success) {
            logger.error("Command processing returned false: commandId={}",
                command.getId());
            throw new CommandProcessingException("Command processing failed for commandId=" + command.getId());
        }

        logger.debug("Successfully processed command: commandId={}, offset={}",
            command.getId(), offset);

        // Acknowledge message after successful processing
        acknowledgment.acknowledge();
        logger.info("Acknowledged command successfully: commandId={}", command.getId());
    }

    /**
     * Consumer for dead letter topic with single message processing.
     * Uses kafkaDlqListenerContainerFactory WITHOUT DLQ to prevent infinite loop.
     */
    @KafkaListener(
            topics = "${kafka.topics.validation-command-dlq:promotion_validation_command_dlq}",
            groupId = "${kafka.consumer.group-id:promotion-validation-consumer}-setting-dlq",
            containerFactory = "kafkaDlqListenerContainerFactory"
    )
    public void handleDeadLetterMessage(
            @Payload SettingValidationRuleCommand command,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        logger.warn("Received message from dead letter queue: topic={}, partition={}, offset={}",
            topic, partition, offset);

        // Skip null/tombstone messages in DLQ
        if (command == null) {
            logger.warn("Received null/tombstone message in DLQ at offset={} - Skipping", offset);
            acknowledgment.acknowledge();
            return;
        }

        logger.warn("Processing DLQ message: commandId={}, type={}, offset={}",
            command.getId(), command.getType(), offset);

        try {
            // Log the failed command for manual investigation
            logger.error("Dead letter command details: commandId={}, type={}, source={}",
                    command.getId(),
                    command.getType(),
                    command.getSource());

            // Could trigger alerting, store in DB for manual processing, etc.
            commandHandler.handleDeadLetterCommand(command);

        } catch (Exception e) {
            logger.error("Error processing dead letter message: {}", e.getMessage(), e);
        }

        // Always acknowledge DLQ messages
        acknowledgment.acknowledge();
        logger.info("Acknowledged DLQ message: commandId={}", command.getId());
    }
}