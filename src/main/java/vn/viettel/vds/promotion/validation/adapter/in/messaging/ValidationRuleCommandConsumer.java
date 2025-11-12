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
import vn.viettel.vds.promotion.validation.domain.exception.CommandProcessingException;

import java.util.List;

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
     * Unified consumer for all ValidationRuleCommand types with batch processing.
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
     * Error handling strategy:
     * - Let exceptions propagate naturally to promix-messaging
     * - No try-catch - framework handles error classification
     * - ValidationException → DLQ immediately (non-retryable)
     * - Other exceptions → Retry logic then DLQ
     *
     * @param commands       Batch of deserialized commands (polymorphic type)
     * @param topic          Kafka topic name
     * @param partition      Partition number
     * @param offsets        List of offsets for each message
     * @param acknowledgment Manual acknowledgment callback
     */
    @KafkaListener(
            topics = "${kafka.topics.validation-command:promotion_validation_command}",
            groupId = "${kafka.consumer.group-id:promotion-validation-consumer}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeCommand(
            List<ValidationRuleCommand> commands,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) List<Long> offsets,
            Acknowledgment acknowledgment) {

        logger.info("Received batch of {} commands from topic={}, partition={}, offsets=[{}-{}]",
            commands.size(), topic, partition,
            offsets.isEmpty() ? "N/A" : offsets.get(0),
            offsets.isEmpty() ? "N/A" : offsets.get(offsets.size() - 1));

        // Process each command in batch
        for (int i = 0; i < commands.size(); i++) {
            ValidationRuleCommand command = commands.get(i);
            long offset = offsets.get(i);

            // Null check - deserialization can fail and return null
            if (command == null) {
                logger.error("Received null command at index {}/{}, offset={}",
                    i + 1, commands.size(), offset);
                throw new IllegalArgumentException("Received null command from Kafka");
            }

            logger.debug("Processing command {}/{}: commandId={}, type={}, offset={}",
                i + 1, commands.size(), command.getId(), command.getType(), offset);

            // Type-based routing using Java 21 pattern matching
            // Let exceptions propagate - no try-catch
            boolean success = switch (command) {
                case SettingValidationRuleCommand c -> handleSettingCommand(c);
                case RollbackValidationRuleCommand c -> handleRollbackCommand(c);
                default -> handleUnknownCommand(command);
            };

            if (!success) {
                logger.error("Command processing returned false: commandId={}, type={}",
                    command.getId(), command.getType());
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
     * Consumer for dead letter topic with batch processing.
     * Uses kafkaDlqListenerContainerFactory WITHOUT DLQ to prevent infinite loop.
     */
    @KafkaListener(
            topics = "${kafka.topics.validation-command-dlq:promotion_validation_command_dlq}",
            groupId = "${kafka.consumer.group-id:promotion-validation-consumer}-dlq",
            containerFactory = "kafkaDlqListenerContainerFactory"
    )
    public void handleDeadLetterMessage(
            List<ValidationRuleCommand> commands,
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
            ValidationRuleCommand command = commands.get(i);
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

                // Route to appropriate DLQ handler based on command type
                switch (command) {
                    case SettingValidationRuleCommand c -> settingCommandHandler.handleDeadLetterCommand(c);
                    case RollbackValidationRuleCommand c -> rollbackCommandHandler.handleDeadLetterCommand(c);
                    default -> logger.error("Unknown command type in DLQ: {}", command.getType());
                }

            } catch (Exception e) {
                logger.error("Error processing dead letter message {}/{}: {}", i + 1, commands.size(), e.getMessage(), e);
            }
        }

        // Always acknowledge DLQ messages
        acknowledgment.acknowledge();
        logger.info("Acknowledged batch of {} DLQ messages", commands.size());
    }
}
