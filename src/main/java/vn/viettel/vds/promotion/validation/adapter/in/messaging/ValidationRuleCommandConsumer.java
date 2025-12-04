package vn.viettel.vds.promotion.validation.adapter.in.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.application.service.RevertValidationRuleCommandHandler;
import vn.viettel.vds.promotion.validation.application.service.RollbackValidationRuleCommandHandler;
import vn.viettel.vds.promotion.validation.application.service.SettingValidationRuleCommandHandler;
import vn.viettel.vds.promotion.validation.application.service.UpdateValidationRuleCommandHandler;
import vn.viettel.vds.promotion.validation.command.RevertValidationRuleCommand;
import vn.viettel.vds.promotion.validation.command.RollbackValidationRuleCommand;
import vn.viettel.vds.promotion.validation.command.SettingValidationRuleCommand;
import vn.viettel.vds.promotion.validation.command.UpdateValidationRuleCommand;
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
 *   <li>{@link UpdateValidationRuleCommand} - Update existing validation rule assignments</li>
 *   <li>{@link RollbackValidationRuleCommand} - Rollback validation rule assignments</li>
 *   <li>{@link RevertValidationRuleCommand} - Revert validation rule to previous version (saga compensation)</li>
 * </ul>
 *
 * <p>The consumer leverages Jackson's {@code @JsonTypeInfo} and {@code @JsonSubTypes}
 * for polymorphic deserialization, automatically routing to the appropriate handler
 * based on the "type" field in the message.</p>
 *
 * @see ValidationRuleCommand
 * @see SettingValidationRuleCommandHandler
 * @see UpdateValidationRuleCommandHandler
 * @see RollbackValidationRuleCommandHandler
 * @see RevertValidationRuleCommandHandler
 */
@Component
public class ValidationRuleCommandConsumer {

    private static final Logger logger = LoggerFactory.getLogger(ValidationRuleCommandConsumer.class);

    private final SettingValidationRuleCommandHandler settingCommandHandler;
    private final UpdateValidationRuleCommandHandler updateCommandHandler;
    private final RollbackValidationRuleCommandHandler rollbackCommandHandler;
    private final RevertValidationRuleCommandHandler revertCommandHandler;

    public ValidationRuleCommandConsumer(
            SettingValidationRuleCommandHandler settingCommandHandler,
            UpdateValidationRuleCommandHandler updateCommandHandler,
            RollbackValidationRuleCommandHandler rollbackCommandHandler,
            RevertValidationRuleCommandHandler revertCommandHandler) {
        this.settingCommandHandler = settingCommandHandler;
        this.updateCommandHandler = updateCommandHandler;
        this.rollbackCommandHandler = rollbackCommandHandler;
        this.revertCommandHandler = revertCommandHandler;
    }

    /**
     * Unified consumer for all ValidationRuleCommand types with single message processing.
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
     * @param command        Single deserialized command (polymorphic type)
     * @param topic          Kafka topic name
     * @param partition      Partition number
     * @param offset         Message offset
     * @param acknowledgment Manual acknowledgment callback
     */
    @KafkaListener(
            topics = "${kafka.topics.validation-command:promotion_validation_command}",
            groupId = "${kafka.consumer.group-id:promotion-validation-consumer}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeCommand(
            @Payload ValidationRuleCommand command,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        logger.info("Received command from topic={}, partition={}, offset={}",
            topic, partition, offset);

        // Null check - deserialization can fail and return null
        if (command == null) {
            logger.error("Received null command at offset={}", offset);
            throw new IllegalArgumentException("Received null command from Kafka");
        }

        logger.debug("Processing command: commandId={}, type={}, offset={}",
            command.getId(), command.getType(), offset);

        // Type-based routing using Java 21 pattern matching
        // Handlers now throw BusinessException with error code if processing fails
        // Let exceptions propagate naturally to promix-messaging for proper error handling
        switch (command) {
            case SettingValidationRuleCommand c -> handleSettingCommand(c);
            case UpdateValidationRuleCommand c -> handleUpdateCommand(c);
            case RollbackValidationRuleCommand c -> handleRollbackCommand(c);
            case RevertValidationRuleCommand c -> handleRevertCommand(c);
            default -> handleUnknownCommand(command);
        }

        logger.debug("Successfully processed command: commandId={}, offset={}",
            command.getId(), offset);

        // Acknowledge message after successful processing
        acknowledgment.acknowledge();
        logger.info("Acknowledged command successfully: commandId={}", command.getId());
    }

    /**
     * Handle SettingValidationRuleCommand.
     * Throws BusinessException with error code if processing fails.
     */
    private void handleSettingCommand(SettingValidationRuleCommand command) {
        logger.debug("Routing to SettingValidationRuleCommandHandler: commandId={}", command.getId());
        settingCommandHandler.handleCommand(command);
    }

    /**
     * Handle UpdateValidationRuleCommand.
     * Throws BusinessException with error code if processing fails.
     */
    private void handleUpdateCommand(UpdateValidationRuleCommand command) {
        logger.debug("Routing to UpdateValidationRuleCommandHandler: commandId={}", command.getId());
        updateCommandHandler.handleCommand(command);
    }

    /**
     * Handle RollbackValidationRuleCommand.
     * Throws BusinessException with error code if processing fails.
     */
    private void handleRollbackCommand(RollbackValidationRuleCommand command) {
        logger.debug("Routing to RollbackValidationRuleCommandHandler: commandId={}", command.getId());
        rollbackCommandHandler.handleCommand(command);
    }

    /**
     * Handle RevertValidationRuleCommand.
     * Used for saga compensation to restore rule to previous version.
     * Throws BusinessException with error code if processing fails.
     */
    private void handleRevertCommand(RevertValidationRuleCommand command) {
        logger.debug("Routing to RevertValidationRuleCommandHandler: commandId={}", command.getId());
        revertCommandHandler.handleCommand(command);
    }

    /**
     * Handle unknown command types (fallback).
     * Always throws IllegalArgumentException.
     */
    private void handleUnknownCommand(ValidationRuleCommand command) {
        logger.error("Unknown command type received: commandId={}, type={} - No handler available",
                command.getId(), command.getType());
        throw new IllegalArgumentException("Unknown command type: " + command.getType());
    }

    /**
     * Consumer for dead letter topic with single message processing.
     * Uses kafkaDlqListenerContainerFactory WITHOUT DLQ to prevent infinite loop.
     */
    @KafkaListener(
            topics = "${kafka.topics.validation-command-dlq:promotion_validation_command_dlq}",
            groupId = "${kafka.consumer.group-id:promotion-validation-consumer}-dlq",
            containerFactory = "kafkaDlqListenerContainerFactory"
    )
    public void handleDeadLetterMessage(
            @Payload ValidationRuleCommand command,
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

            // Route to appropriate DLQ handler based on command type
            switch (command) {
                case SettingValidationRuleCommand c -> settingCommandHandler.handleDeadLetterCommand(c);
                case UpdateValidationRuleCommand c -> updateCommandHandler.handleDeadLetterCommand(c);
                case RollbackValidationRuleCommand c -> rollbackCommandHandler.handleDeadLetterCommand(c);
                case RevertValidationRuleCommand c -> revertCommandHandler.handleDeadLetterCommand(c);
                default -> logger.error("Unknown command type in DLQ: {}", command.getType());
            }

        } catch (Exception e) {
            logger.error("Error processing dead letter message: {}", e.getMessage(), e);
        }

        // Always acknowledge DLQ messages
        acknowledgment.acknowledge();
        logger.info("Acknowledged DLQ message: commandId={}", command.getId());
    }
}
