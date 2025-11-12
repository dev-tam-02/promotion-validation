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

import java.util.List;

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
     * Consume batch of SettingValidationRuleCommand from Kafka topic.
     *
     * Error handling strategy:
     * - Let exceptions propagate naturally to promix-messaging
     * - No try-catch - framework handles error classification
     * - ValidationException → DLQ immediately (non-retryable)
     * - Other exceptions → Retry logic then DLQ
     */
    @KafkaListener(
            topics = "${kafka.topics.setting-validation-rule-events}",
            groupId = "${kafka.consumer.group-id}-setting",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleSettingValidationRuleCommand(
            List<SettingValidationRuleCommand> commands,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) List<Long> offsets,
            Acknowledgment acknowledgment) {

        logger.info("Received batch of {} SettingValidationRuleCommands from topic={}, partition={}, offsets=[{}-{}]",
            commands.size(), topic, partition,
            offsets.isEmpty() ? "N/A" : offsets.get(0),
            offsets.isEmpty() ? "N/A" : offsets.get(offsets.size() - 1));

        // Process each command in batch
        for (int i = 0; i < commands.size(); i++) {
            SettingValidationRuleCommand command = commands.get(i);
            long offset = offsets.get(i);

            // Null check - deserialization can fail and return null
            if (command == null) {
                logger.error("Received null command at index {}/{}, offset={}",
                    i + 1, commands.size(), offset);
                throw new IllegalArgumentException("Received null command from Kafka");
            }

            logger.debug("Processing command {}/{}: commandId={}, type={}, offset={}",
                i + 1, commands.size(), command.getId(), command.getType(), offset);

            // Handle the command - let exceptions propagate
            boolean success = commandHandler.handleCommand(command);

            if (!success) {
                logger.error("Command processing returned false: commandId={}",
                    command.getId());
                throw new CommandProcessingException("Command processing failed for commandId=" + command.getId());
            }

            logger.debug("Successfully processed command {}/{}: commandId={}, offset={}",
                i + 1, commands.size(), command.getId(), offset);
        }

        // Acknowledge entire batch after successful processing
        acknowledgment.acknowledge();
        logger.info("Acknowledged batch of {} commands successfully", commands.size());
    }

    /**
     * Consumer for dead letter topic with batch processing.
     * Uses kafkaDlqListenerContainerFactory WITHOUT DLQ to prevent infinite loop.
     */
    @KafkaListener(
            topics = "${kafka.topics.validation-command-dlq}",
            groupId = "${kafka.consumer.group-id}-setting-dlq",
            containerFactory = "kafkaDlqListenerContainerFactory"
    )
    public void handleDeadLetterMessage(
            List<SettingValidationRuleCommand> commands,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) List<Long> offsets,
            Acknowledgment acknowledgment) {

        logger.warn("Received batch of {} messages from dead letter queue: topic={}, partition={}, offsets=[{}-{}]",
            commands.size(), topic, partition,
            offsets.isEmpty() ? "N/A" : offsets.get(0),
            offsets.isEmpty() ? "N/A" : offsets.get(offsets.size() - 1));

        // Process each command in batch
        for (int i = 0; i < commands.size(); i++) {
            SettingValidationRuleCommand command = commands.get(i);
            long offset = offsets.get(i);

            // Skip null/tombstone messages in DLQ
            if (command == null) {
                logger.warn("Received null/tombstone message in DLQ at index {}/{}, offset={} - Skipping",
                    i + 1, commands.size(), offset);
                continue;
            }

            logger.warn("Processing DLQ message {}/{}: commandId={}, type={}, offset={}",
                i + 1, commands.size(), command.getId(), command.getType(), offset);

            try {
                // Log the failed command for manual investigation
                logger.error("Dead letter command details: commandId={}, type={}, source={}",
                        command.getId(),
                        command.getType(),
                        command.getSource());

                // Could trigger alerting, store in DB for manual processing, etc.
                commandHandler.handleDeadLetterCommand(command);

            } catch (Exception e) {
                logger.error("Error processing dead letter message {}/{}: {}", i + 1, commands.size(), e.getMessage(), e);
            }
        }

        // Always acknowledge DLQ messages
        acknowledgment.acknowledge();
        logger.info("Acknowledged batch of {} DLQ messages", commands.size());
    }
}