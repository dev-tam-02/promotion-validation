package vn.viettel.vds.promotion.validation.application.service;

import com.promix.platform.core.util.IdGenerator;
import com.promix.platform.messaging.autoconfigure.utils.KafkaUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import vn.viettel.vds.promotion.schema.validation.command.SettingValidationRuleCommand;
import vn.viettel.vds.promotion.schema.validation.command.TimeFrame;
import vn.viettel.vds.promotion.schema.validation.event.*;
import vn.viettel.vds.promotion.validation.domain.exception.ValidationException;
import vn.viettel.vds.promotion.validation.domain.model.Assignment;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Publisher for SettingValidationRuleEvent using Avro serialization.
 * Publishes validation rule processing results to Kafka with proper headers.
 */
@Service
public class SettingValidationRuleEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(SettingValidationRuleEventPublisher.class);

    // String literal constants
    private static final String CORRELATION_ID_KEY = "correlationId";
    private static final String SERVICE_NAME_KEY = "serviceName";
    private static final String SERVICE_VERSION_KEY = "serviceVersion";
    private static final String SERVICE_VERSION = "1.0.0";
    private static final String AGGREGATE_VALIDATION = "Validation";
    private static final String EVENT_TYPE_SETTING_VALIDATION_RULE = "SettingValidationRuleEvent";

    private final KafkaUtils kafkaUtils;
    private final CommandMappingService mappingService;
    private final String eventTopic;
    private final String serviceName;

    public SettingValidationRuleEventPublisher(
            KafkaUtils kafkaUtils,
            CommandMappingService mappingService,
            @Value("${promix.messaging.topics.setting-validation-rule-events:promotion_validation_events}") String eventTopic,
            @Value("${spring.application.name:validation}") String serviceName) {
        this.kafkaUtils = kafkaUtils;
        this.mappingService = mappingService;
        this.eventTopic = eventTopic;
        this.serviceName = serviceName;
    }

    /**
     * Publish success event with Avro schema
     */
    public void publishSuccessEvent(String commandId, SettingValidationRuleCommandHandler.CommandProcessingResult result) {
        try {
            SettingValidationRuleEvent event = createSuccessEvent(commandId, result);
            String sagaId = result.getAssignment().getSubject().getKey(); // Campaign ID

            publishEvent(event, result.getAssignment().getId(), commandId, "SUCCESS", sagaId);

            logger.info("Published SettingValidationRuleEvent success: commandId={}, assignmentId={}",
                    commandId, result.getAssignment().getId());

        } catch (Exception e) {
            throw new ValidationException("Failed to publish success event for commandId: " + commandId, e);
        }
    }

    /**
     * Publish error event with Avro schema
     */
    public void publishErrorEvent(String commandId, String errorCode, String errorMessage) {
        try {
            SettingValidationRuleEvent event = createErrorEvent(commandId, errorCode, errorMessage);
            publishEvent(event, commandId, commandId, "FAILURE", null);

            logger.info("Published SettingValidationRuleEvent error: commandId={}, errorCode={}",
                    commandId, errorCode);

        } catch (Exception e) {
            throw new ValidationException("Failed to publish error event for commandId: " + commandId, e);
        }
    }

    /**
     * Publish dead letter event with Avro schema
     */
    public void publishDeadLetterEvent(String commandId, SettingValidationRuleCommand originalCommand) {
        try {
            SettingValidationRuleEvent event = createDeadLetterEvent(commandId, originalCommand);
            publishEvent(event, commandId, commandId, "DEAD_LETTER", null);

            logger.warn("Published SettingValidationRuleEvent dead letter: commandId={}", commandId);

        } catch (Exception e) {
            logger.error("Failed to publish dead letter event: commandId={}", commandId, e);
        }
    }

    /**
     * Create success event payload using Avro builder
     */
    private SettingValidationRuleEvent createSuccessEvent(
            String commandId,
            SettingValidationRuleCommandHandler.CommandProcessingResult result) {

        Assignment assignment = result.getAssignment();
        CommandMappingService.ApplicabilityStats stats =
                mappingService.calculateApplicabilityStats(result.getApplicabilityData());

        // Build Assignment Result
        AssignmentResult assignmentResult = AssignmentResult.newBuilder()
                .setAssignmentId(assignment.getId())
                .setRuleId(assignment.getId())
                .setActive(assignment.getActive() != null ? assignment.getActive() : false)
                .setTrafficPercent(assignment.getTrafficPercent() != null ? assignment.getTrafficPercent() : 100)
                .setPriority(0) // Priority field can be added to assignment entity when needed
                .build();

        // Build Applicability Result
        ApplicabilityResult applicabilityResult = ApplicabilityResult.newBuilder()
                .setSubjectType("PRODUCT") // Default subject type for applicability
                .setSubjectKey("*") // Default to all products
                .setIncludedItemsCount(stats.getIncludedItemsCount())
                .setExcludedItemsCount(stats.getExcludedItemsCount())
                .setIncludedAll(stats.isIncludedAll())
                .build();

        // Build Timeframe Result (if provided)
        TimeframeResult timeframeResult = buildTimeframeResult(result);

        // Build Event Payload
        SettingValidationRuleEventPayload payload = SettingValidationRuleEventPayload.newBuilder()
                .setCommandId(commandId)
                .setIsSuccess(true)
                .setErrorCode(null)
                .setErrorMessage(null)
                .setAssignmentResult(assignmentResult)
                .setApplicabilityResult(applicabilityResult)
                .setTimeframeResult(timeframeResult)
                .setProcessedBy(serviceName)
                .setProcessedAt(Instant.now())
                .build();

        // Build Metadata
        Map<String, String> metadata = new HashMap<>();
        metadata.put(CORRELATION_ID_KEY, commandId);
        metadata.put(SERVICE_NAME_KEY, serviceName);
        metadata.put(SERVICE_VERSION_KEY, SERVICE_VERSION);

        // Build Complete Event
        return SettingValidationRuleEvent.newBuilder()
                .setId(IdGenerator.generateId())
                .setAggregate(AGGREGATE_VALIDATION)
                .setType(EVENT_TYPE_SETTING_VALIDATION_RULE)
                .setSource(serviceName)
                .setSubject(assignment.getId())
                .setOccurredAt(Instant.now())
                .setVersion(1)
                .setPayload(payload)
                .setMetadata(metadata)
                .build();
    }

    /**
     * Create error event payload using Avro builder
     */
    private SettingValidationRuleEvent createErrorEvent(String commandId, String errorCode, String errorMessage) {
        // Build Event Payload for error
        SettingValidationRuleEventPayload payload = SettingValidationRuleEventPayload.newBuilder()
                .setCommandId(commandId)
                .setIsSuccess(false)
                .setErrorCode(errorCode)
                .setErrorMessage(errorMessage)
                .setAssignmentResult(null)
                .setApplicabilityResult(null)
                .setTimeframeResult(null)
                .setProcessedBy(serviceName)
                .setProcessedAt(Instant.now())
                .build();

        // Build Metadata
        Map<String, String> metadata = new HashMap<>();
        metadata.put(CORRELATION_ID_KEY, commandId);
        metadata.put(SERVICE_NAME_KEY, serviceName);
        metadata.put(SERVICE_VERSION_KEY, SERVICE_VERSION);

        // Build Complete Event
        return SettingValidationRuleEvent.newBuilder()
                .setId(IdGenerator.generateId())
                .setAggregate(AGGREGATE_VALIDATION)
                .setType(EVENT_TYPE_SETTING_VALIDATION_RULE)
                .setSource(serviceName)
                .setSubject(commandId)
                .setOccurredAt(Instant.now())
                .setVersion(1)
                .setPayload(payload)
                .setMetadata(metadata)
                .build();
    }

    /**
     * Create dead letter event payload using Avro builder
     */
    private SettingValidationRuleEvent createDeadLetterEvent(
            String commandId,
            SettingValidationRuleCommand originalCommand) {

        // Build Event Payload for dead letter
        SettingValidationRuleEventPayload payload = SettingValidationRuleEventPayload.newBuilder()
                .setCommandId(commandId)
                .setIsSuccess(false)
                .setErrorCode("DEAD_LETTER")
                .setErrorMessage("Command sent to dead letter queue after max retries")
                .setAssignmentResult(null)
                .setApplicabilityResult(null)
                .setTimeframeResult(null)
                .setProcessedBy(serviceName)
                .setProcessedAt(Instant.now())
                .build();

        // Build Metadata with original command info
        Map<String, String> metadata = new HashMap<>();
        metadata.put(CORRELATION_ID_KEY, commandId);
        metadata.put(SERVICE_NAME_KEY, serviceName);
        metadata.put(SERVICE_VERSION_KEY, SERVICE_VERSION);
        metadata.put("originalCommandId", originalCommand.getId());

        // Build Complete Event
        return SettingValidationRuleEvent.newBuilder()
                .setId(IdGenerator.generateId())
                .setAggregate(AGGREGATE_VALIDATION)
                .setType(EVENT_TYPE_SETTING_VALIDATION_RULE)
                .setSource(serviceName)
                .setSubject(commandId)
                .setOccurredAt(Instant.now())
                .setVersion(1)
                .setPayload(payload)
                .setMetadata(metadata)
                .build();
    }

    /**
     * Publish rollback success event for saga compensation
     */
    public void publishRollbackSuccessEvent(String commandId, String campaignId, String validationRuleId) {
        try {
            // Build Event Payload for rollback success
            SettingValidationRuleEventPayload payload = SettingValidationRuleEventPayload.newBuilder()
                    .setCommandId(commandId)
                    .setIsSuccess(true)
                    .setErrorCode(null)
                    .setErrorMessage(null)
                    .setAssignmentResult(null)
                    .setApplicabilityResult(null)
                    .setTimeframeResult(null)
                    .setProcessedBy(serviceName)
                    .setProcessedAt(Instant.now())
                    .build();

            // Build Metadata
            Map<String, String> metadata = new HashMap<>();
            metadata.put(CORRELATION_ID_KEY, commandId);
            metadata.put(SERVICE_NAME_KEY, serviceName);
            metadata.put(SERVICE_VERSION_KEY, SERVICE_VERSION);
            metadata.put("eventType", "ROLLBACK_SUCCESS");
            metadata.put("campaignId", campaignId);
            if (validationRuleId != null) {
                metadata.put("validationRuleId", validationRuleId);
            }

            // Build Complete Event
            SettingValidationRuleEvent event = SettingValidationRuleEvent.newBuilder()
                    .setId(IdGenerator.generateId())
                    .setAggregate(AGGREGATE_VALIDATION)
                    .setType("ValidationRollbackSuccessEvent")
                    .setSource(serviceName)
                    .setSubject(campaignId)
                    .setOccurredAt(Instant.now())
                    .setVersion(1)
                    .setPayload(payload)
                    .setMetadata(metadata)
                    .build();

            publishEvent(event, campaignId, commandId, "ROLLBACK_SUCCESS", campaignId);

            logger.info("Published rollback success event: commandId={}, campaignId={}, validationRuleId={}",
                    commandId, campaignId, validationRuleId);

        } catch (Exception e) {
            throw new ValidationException("Failed to publish rollback success event for commandId: " + commandId, e);
        }
    }

    /**
     * Publish rollback error event for saga compensation
     */
    public void publishRollbackErrorEvent(String commandId, String errorCode, String errorMessage) {
        try {
            // Build Event Payload for rollback error
            SettingValidationRuleEventPayload payload = SettingValidationRuleEventPayload.newBuilder()
                    .setCommandId(commandId)
                    .setIsSuccess(false)
                    .setErrorCode(errorCode)
                    .setErrorMessage(errorMessage)
                    .setAssignmentResult(null)
                    .setApplicabilityResult(null)
                    .setTimeframeResult(null)
                    .setProcessedBy(serviceName)
                    .setProcessedAt(Instant.now())
                    .build();

            // Build Metadata
            Map<String, String> metadata = new HashMap<>();
            metadata.put(CORRELATION_ID_KEY, commandId);
            metadata.put(SERVICE_NAME_KEY, serviceName);
            metadata.put(SERVICE_VERSION_KEY, SERVICE_VERSION);
            metadata.put("eventType", "ROLLBACK_ERROR");

            // Build Complete Event
            SettingValidationRuleEvent event = SettingValidationRuleEvent.newBuilder()
                    .setId(IdGenerator.generateId())
                    .setAggregate(AGGREGATE_VALIDATION)
                    .setType("ValidationRollbackErrorEvent")
                    .setSource(serviceName)
                    .setSubject(commandId)
                    .setOccurredAt(Instant.now())
                    .setVersion(1)
                    .setPayload(payload)
                    .setMetadata(metadata)
                    .build();

            publishEvent(event, commandId, commandId, "ROLLBACK_ERROR", null);

            logger.info("Published rollback error event: commandId={}, errorCode={}, errorMessage={}",
                    commandId, errorCode, errorMessage);

        } catch (Exception e) {
            throw new vn.viettel.vds.promotion.validation.domain.exception.ValidationException(
                    "Failed to publish rollback error event for commandId: " + commandId, e);
        }
    }

    /**
     * Publish Avro event to Kafka with proper headers
     */
    private void publishEvent(
            SettingValidationRuleEvent event,
            String key,
            String correlationId,
            String resultStatus,
            String sagaId) {
        try {
            // Build message with headers
            MessageBuilder<SettingValidationRuleEvent> messageBuilder = MessageBuilder
                    .withPayload(event)
                    .setHeader(KafkaHeaders.KEY, key)
                    .setHeader("correlation-id", correlationId)
                    .setHeader("result-status", resultStatus);

            // Add saga-id header if provided
            if (sagaId != null) {
                messageBuilder.setHeader("saga-id", sagaId);
            }

            Message<SettingValidationRuleEvent> message = messageBuilder.build();

            // Use KafkaUtils to send with Avro serialization
            kafkaUtils.send(eventTopic, key, event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            logger.debug("Published Avro event to topic {}: key={}, partition={}, offset={}",
                                    eventTopic, key,
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        } else {
                            logger.error("Failed to publish Avro event to Kafka: topic={}, key={}",
                                    eventTopic, key, ex);
                            throw new vn.viettel.vds.promotion.validation.domain.exception.ValidationException(
                                    "Failed to publish event to Kafka", ex);
                        }
                    });

        } catch (Exception e) {
            throw new ValidationException("Failed to publish Avro event to Kafka - topic: " + eventTopic + ", key: " + key, e);
        }
    }

    private TimeframeResult buildTimeframeResult(SettingValidationRuleCommandHandler.CommandProcessingResult result) {
        if (result.getTimeFrameId() == null) {
            return null;
        }

        TimeFrame timeframeData = result.getTimeframeData();
        Long validFrom = null;
        Long validTo = null;
        String mode = "ALLOW";
        String timezone = "UTC";

        if (timeframeData != null) {
            if (timeframeData.getValidityTimeframe() != null) {
                java.time.Instant startDate = timeframeData.getValidityTimeframe().getStartDate();
                java.time.Instant expirationDate = timeframeData.getValidityTimeframe().getExpirationDate();
                validFrom = startDate != null ? startDate.toEpochMilli() : null;
                validTo = expirationDate != null ? expirationDate.toEpochMilli() : null;
            }
            mode = timeframeData.getMode().toString();
            timezone = timeframeData.getTimezone().toString();
        }

        return TimeframeResult.newBuilder()
                .setTimeFrameId(result.getTimeFrameId())
                .setValidFrom(validFrom != null ? Instant.ofEpochMilli(validFrom) : null)
                .setValidTo(validTo != null ? Instant.ofEpochMilli(validTo) : null)
                .setMode(mode)
                .setTimezone(timezone)
                .build();
    }

}
