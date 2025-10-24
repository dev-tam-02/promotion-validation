package vn.viettel.vds.promotion.validation.adapter.in.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.application.service.RollbackValidationRuleCommandHandler;
import vn.viettel.vds.promotion.validation.application.service.SettingValidationRuleCommandHandler;
import vn.viettel.vds.promotion.validation.command.RollbackValidationRuleCommand;
import vn.viettel.vds.promotion.validation.command.SettingValidationRuleCommand;
import vn.viettel.vds.promotion.validation.command.ValidationRuleCommand;

/**
 * Unified Kafka consumer for ValidationRuleCommand with type-based routing.
 * <p>
 * This consumer uses Java 21 pattern matching to handle different command types
 * from a single topic, providing a cleaner and more maintainable alternative to
 * multiple specialized consumers.
 *
 * <p>Supported command types:</p>
 * <ul>
 *   <li>{@link SettingValidationRuleCommand} - Assign validation rules to campaigns</li>
 *   <li>{@link RollbackValidationRuleCommand} - Rollback validation rule assignments</li>
 * </ul>
 *
 * <p>The consumer leverages Jackson's {@code @JsonTypeInfo} and {@code @JsonSubTypes}
 * for polymorphic deserialization, automatically routing to the appropriate handler
 * based on the "type" field in the message.</p>
 *
 * @see ValidationRuleCommand
 * @see SettingValidationRuleCommandHandler
 * @see RollbackValidationRuleCommandHandler
 */
@Component
public class ValidationRuleCommandConsumer {

    private static final Logger logger = LoggerFactory.getLogger(ValidationRuleCommandConsumer.class);

    private final SettingValidationRuleCommandHandler settingCommandHandler;
    private final RollbackValidationRuleCommandHandler rollbackCommandHandler;

    public ValidationRuleCommandConsumer(
            SettingValidationRuleCommandHandler settingCommandHandler,
            RollbackValidationRuleCommandHandler rollbackCommandHandler) {
        this.settingCommandHandler = settingCommandHandler;
        this.rollbackCommandHandler = rollbackCommandHandler;
    }

    /**
     * Unified consumer for all ValidationRuleCommand types.
     *
     * <p>Uses Java 21 pattern matching switch to route commands to appropriate handlers:</p>
     * <pre>{@code
     * switch (command) {
     *     case SettingValidationRuleCommand c -> handleSetting(c);
     *     case RollbackValidationRuleCommand c -> handleRollback(c);
     *     default -> handleUnknown(command);
     * }
     * }</pre>
     *
     * @param command        The deserialized command (polymorphic type)
     * @param topic          Kafka topic name
     * @param partition      Partition number
     * @param offset         Message offset
     * @param key            Message key (optional)
     * @param acknowledgment Manual acknowledgment callback
     */
    @KafkaListener(
            topics = "${kafka.topics.validation-command}",
            groupId = "${kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeCommand(
            @Payload(required = false) ValidationRuleCommand command,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(value = KafkaHeaders.RECEIVED_KEY, required = false) String key,
            Acknowledgment acknowledgment) {

        // Handle null/tombstone messages
        if (command == null) {
            logger.warn("Received null/tombstone message: topic={}, partition={}, offset={}, key={} - Skipping",
                    topic, partition, offset, key);
            acknowledgment.acknowledge();
            return;
        }

        logger.info("Received ValidationRuleCommand: topic={}, partition={}, offset={}, key={}, commandId={}, type={}",
                topic, partition, offset, key, command.getId(), command.getType());

        try {
            // Type-based routing using Java 21 pattern matching
            boolean success = switch (command) {
                case SettingValidationRuleCommand c -> handleSettingCommand(c);
                case RollbackValidationRuleCommand c -> handleRollbackCommand(c);
                default -> handleUnknownCommand(command);
            };

            if (success) {
                logger.info("Successfully processed command: commandId={}, type={}",
                        command.getId(), command.getType());
                acknowledgment.acknowledge();
            } else {
                logger.error("Failed to process command: commandId={}, type={}",
                        command.getId(), command.getType());
                // Don't acknowledge - message will be retried
            }

        } catch (Exception e) {
            logger.error("Error processing command: topic={}, partition={}, offset={}, commandId={}, type={}, error={}",
                    topic, partition, offset, command.getId(), command.getType(), e.getMessage(), e);

            // For critical errors, acknowledge to avoid infinite retry
            acknowledgment.acknowledge();
        }
    }

    /**
     * Handle SettingValidationRuleCommand.
     */
    private boolean handleSettingCommand(SettingValidationRuleCommand command) {
        logger.debug("Routing to SettingValidationRuleCommandHandler: commandId={}", command.getId());
        return settingCommandHandler.handleCommand(command);
    }

    /**
     * Handle RollbackValidationRuleCommand.
     */
    private boolean handleRollbackCommand(RollbackValidationRuleCommand command) {
        logger.debug("Routing to RollbackValidationRuleCommandHandler: commandId={}", command.getId());
        return rollbackCommandHandler.handleCommand(command);
    }

    /**
     * Handle unknown command types (fallback).
     */
    private boolean handleUnknownCommand(ValidationRuleCommand command) {
        logger.error("Unknown command type received: commandId={}, type={} - No handler available",
                command.getId(), command.getType());
        throw new IllegalArgumentException("Unknown command type: " + command.getType());
    }

    /**
     * Consumer for dead letter topic.
     * Uses kafkaDlqListenerContainerFactory WITHOUT DLQ to prevent infinite loop.
     */
    @KafkaListener(
            topics = "${kafka.topics.validation-command-dlq}",
            groupId = "${kafka.consumer.group-id}-dlq",
            containerFactory = "kafkaDlqListenerContainerFactory"
    )
    public void handleDeadLetterMessage(
            @Payload(required = false) ValidationRuleCommand command,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        // Skip null/tombstone messages in DLQ
        if (command == null) {
            logger.warn("Received null/tombstone message in DLQ: topic={}, partition={}, offset={} - Skipping",
                    topic, partition, offset);
            acknowledgment.acknowledge();
            return;
        }

        logger.warn("Received message from dead letter queue: topic={}, partition={}, offset={}, commandId={}, type={}",
                topic, partition, offset, command.getId(), command.getType());

        try {
            // Log the failed command for manual investigation
            logger.error("Dead letter command details: commandId={}, type={}, source={}",
                    command.getId(),
                    command.getType(),
                    command.getSource());

            // Route to appropriate DLQ handler based on command type
            switch (command) {
                case SettingValidationRuleCommand c -> settingCommandHandler.handleDeadLetterCommand(c);
                case RollbackValidationRuleCommand c -> rollbackCommandHandler.handleDeadLetterCommand(c);
                default -> logger.error("Unknown command type in DLQ: {}", command.getType());
            }

        } catch (Exception e) {
            logger.error("Error processing dead letter message: {}", e.getMessage(), e);
        } finally {
            acknowledgment.acknowledge();
        }
    }
}
