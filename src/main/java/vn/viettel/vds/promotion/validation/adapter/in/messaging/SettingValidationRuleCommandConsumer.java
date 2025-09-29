package vn.viettel.vds.promotion.validation.adapter.in.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.schema.validation.command.SettingValidationRuleCommand;
import vn.viettel.vds.promotion.validation.application.service.SettingValidationRuleCommandHandler;

/**
 * Kafka consumer for SettingValidationRuleCommand messages
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
     * Consume SettingValidationRuleCommand from Kafka topic
     */
    @KafkaListener(
        topics = "setting-validation-rule-commands",
        groupId = "validation-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleSettingValidationRuleCommand(
            @Payload SettingValidationRuleCommand command,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(value = KafkaHeaders.RECEIVED_KEY, required = false) String key,
            Acknowledgment acknowledgment) {

        logger.info("Received SettingValidationRuleCommand: topic={}, partition={}, offset={}, key={}, commandId={}",
                topic, partition, offset, key, command.getId());

        try {
            logger.debug("Processing SettingValidationRuleCommand: commandId={}, type={}, source={}",
                    command.getId(), command.getType(), command.getSource());

            // Handle the command
            boolean success = commandHandler.handleCommand(command);

            if (success) {
                logger.info("Successfully processed SettingValidationRuleCommand: commandId={}",
                        command.getId());
                acknowledgment.acknowledge();
            } else {
                logger.error("Failed to process SettingValidationRuleCommand: commandId={}",
                        command.getId());
                // Don't acknowledge - message will be retried
            }

        } catch (Exception e) {
            logger.error("Error processing SettingValidationRuleCommand: topic={}, partition={}, offset={}, commandId={}, error={}",
                    topic, partition, offset, command.getId(), e.getMessage(), e);

            // For critical errors, we might want to acknowledge to avoid infinite retry
            // This depends on your error handling strategy
            acknowledgment.acknowledge();
        }
    }

    /**
     * Consumer for dead letter topic (optional)
     */
    @KafkaListener(
        topics = "setting-validation-rule-commands-dlq",
        groupId = "validation-group-dlq",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleDeadLetterMessage(
            @Payload SettingValidationRuleCommand command,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        logger.warn("Received message from dead letter queue: topic={}, partition={}, offset={}, commandId={}",
                topic, partition, offset, command.getId());

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
        } finally {
            acknowledgment.acknowledge();
        }
    }
}