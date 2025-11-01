package vn.viettel.vds.promotion.validation.application.service;

import com.promix.platform.core.util.IdGenerator;
import com.promix.platform.messaging.autoconfigure.utils.KafkaUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import vn.viettel.vds.promotion.validation.command.SettingValidationRuleCommand.TimeFrame;
import vn.viettel.vds.promotion.validation.domain.exception.ValidationException;
import vn.viettel.vds.promotion.validation.domain.model.Assignment;
import vn.viettel.vds.promotion.validation.event.ValidationRuleSettingAppliedEvent;
import vn.viettel.vds.promotion.validation.event.ValidationRuleSettingAppliedEventPayload;
import vn.viettel.vds.promotion.validation.event.ValidationRuleSettingFailedEvent;
import vn.viettel.vds.promotion.validation.event.ValidationRuleSettingFailedEventPayload;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Publisher for Validation Rule Setting Events using JSON serialization.
 * Publishes 2 separate events:
 * - ValidationRuleSettingAppliedEvent: khi setting rule thành công
 * - ValidationRuleSettingFailedEvent: khi setting rule thất bại
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
    private static final String EVENT_TYPE_APPLIED = "ValidationRuleSettingAppliedEvent";
    private static final String EVENT_TYPE_FAILED = "ValidationRuleSettingFailedEvent";

    private final KafkaUtils kafkaUtils;
    private final CommandMappingService mappingService;
    private final String eventTopic;
    private final String serviceName;

    public SettingValidationRuleEventPublisher(
            KafkaUtils kafkaUtils,
            CommandMappingService mappingService,
            @Value("${promix.messaging.topics.setting-validation-rule-events:promotion_validation_event}") String eventTopic,
            @Value("${spring.application.name:validation}") String serviceName) {
        this.kafkaUtils = kafkaUtils;
        this.mappingService = mappingService;
        this.eventTopic = eventTopic;
        this.serviceName = serviceName;
    }

    /**
     * Publish success event - ValidationRuleSettingAppliedEvent
     */
    public void publishSuccessEvent(String commandId, SettingValidationRuleCommandHandler.CommandProcessingResult result) {
        try {
            ValidationRuleSettingAppliedEvent event = createSuccessEvent(commandId, result);

            publishAppliedEvent(event, result.getAssignment().getId());

            logger.info("Published ValidationRuleSettingAppliedEvent: commandId={}, assignmentId={}",
                    commandId, result.getAssignment().getId());

        } catch (Exception e) {
            throw new ValidationException("Failed to publish success event for commandId: " + commandId, e);
        }
    }

    /**
     * Publish error event - ValidationRuleSettingFailedEvent
     */
    public void publishErrorEvent(String commandId, String campaignId, String errorCode, String errorMessage) {
        try {
            ValidationRuleSettingFailedEvent event = createErrorEvent(commandId, campaignId, errorCode, errorMessage);
            String subject = campaignId != null ? campaignId : commandId;
            publishFailedEvent(event, subject);

            logger.info("Published ValidationRuleSettingFailedEvent: commandId={}, campaignId={}, errorCode={}",
                    commandId, campaignId, errorCode);

        } catch (Exception e) {
            throw new ValidationException("Failed to publish error event for commandId: " + commandId, e);
        }
    }

    /**
     * Publish dead letter event as ValidationRuleSettingFailedEvent
     */
    public void publishDeadLetterEvent(String commandId) {
        try {
            String errorMessage = "Command sent to dead letter queue after max retries";
            publishErrorEvent(commandId, null, "DEAD_LETTER", errorMessage);

            logger.warn("Published dead letter event as ValidationRuleSettingFailedEvent: commandId={}", commandId);

        } catch (Exception e) {
            logger.error("Failed to publish dead letter event: commandId={}", commandId, e);
        }
    }

    /**
     * Create success event - ValidationRuleSettingAppliedEvent
     */
    private ValidationRuleSettingAppliedEvent createSuccessEvent(
            String commandId,
            SettingValidationRuleCommandHandler.CommandProcessingResult result) {

        Assignment assignment = result.getAssignment();
        CommandMappingService.ApplicabilityStats stats =
                mappingService.calculateApplicabilityStats(result.getApplicabilityData());

        // Build Assignment Result
        ValidationRuleSettingAppliedEventPayload.AssignmentResult assignmentResult =
                ValidationRuleSettingAppliedEventPayload.AssignmentResult.builder()
                .assignmentId(assignment.getId())
                .ruleId(assignment.getRuleId())
                .active(Boolean.TRUE.equals(assignment.getActive()))
                .trafficPercent(assignment.getTrafficPercent() != null ? assignment.getTrafficPercent() : 100)
                .priority(0) // Priority field can be added to assignment entity when needed
                .build();

        // Build Applicability Result
        ValidationRuleSettingAppliedEventPayload.ApplicabilityResult applicabilityResult =
                ValidationRuleSettingAppliedEventPayload.ApplicabilityResult.builder()
                .subjectType("PRODUCT") // Default subject type for applicability
                .subjectKey("*") // Default to all products
                .includedItemsCount(stats.getIncludedItemsCount())
                .excludedItemsCount(stats.getExcludedItemsCount())
                .includedAll(stats.isIncludedAll())
                .build();

        // Build Timeframe Result (if provided)
        ValidationRuleSettingAppliedEventPayload.TimeframeResult timeframeResult = buildTimeframeResultForApplied(result);

        // Build Event Payload
        ValidationRuleSettingAppliedEventPayload payload = ValidationRuleSettingAppliedEventPayload.builder()
                .commandId(commandId)
                .assignmentResult(assignmentResult)
                .applicabilityResult(applicabilityResult)
                .timeframeResult(timeframeResult)
                .processedBy(serviceName)
                .processedAt(Instant.now().toEpochMilli())
                .build();

        // Build Metadata
        Map<String, String> metadata = new HashMap<>();
        metadata.put(CORRELATION_ID_KEY, commandId);
        metadata.put(SERVICE_NAME_KEY, serviceName);
        metadata.put(SERVICE_VERSION_KEY, SERVICE_VERSION);

        // Build Complete Event
        return ValidationRuleSettingAppliedEvent.builder()
                .id(IdGenerator.generateId())
                .aggregate(AGGREGATE_VALIDATION)
                .type(EVENT_TYPE_APPLIED)
                .source(serviceName)
                .subject(assignment.getSubject().getKey())
                .occurredAt(Instant.now())
                .version(1)
                .payload(payload)
                .metadata(metadata)
                .build();
    }

    /**
     * Create error event - ValidationRuleSettingFailedEvent
     */
    private ValidationRuleSettingFailedEvent createErrorEvent(String commandId, String campaignId, String errorCode, String errorMessage) {
        // Build Event Payload for error
        ValidationRuleSettingFailedEventPayload payload = ValidationRuleSettingFailedEventPayload.builder()
                .commandId(commandId)
                .campaignId(campaignId)
                .ruleId(null) // Will be populated if available in result
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .failureReason(errorMessage)
                .processedBy(serviceName)
                .processedAt(Instant.now().toEpochMilli())
                .build();

        // Build Metadata
        Map<String, String> metadata = new HashMap<>();
        metadata.put(CORRELATION_ID_KEY, commandId);
        metadata.put(SERVICE_NAME_KEY, serviceName);
        metadata.put(SERVICE_VERSION_KEY, SERVICE_VERSION);

        // Build Complete Event
        // Use campaignId as subject if available, otherwise fall back to commandId
        String subject = campaignId != null ? campaignId : commandId;
        return ValidationRuleSettingFailedEvent.builder()
                .id(IdGenerator.generateId())
                .aggregate(AGGREGATE_VALIDATION)
                .type(EVENT_TYPE_FAILED)
                .source(serviceName)
                .subject(subject)
                .occurredAt(Instant.now())
                .version(1)
                .payload(payload)
                .metadata(metadata)
                .build();
    }


    /**
     * Publish rollback success event for saga compensation
     * Uses ValidationRuleSettingAppliedEvent with rollback metadata
     */
    public void publishRollbackSuccessEvent(String commandId, String campaignId, String validationRuleId) {
        logger.info("Rollback success - treating as applied event: commandId={}, campaignId={}, validationRuleId={}",
                commandId, campaignId, validationRuleId);
        // Rollback success can be handled by campaign saga orchestrator
        // No need to publish separate event
    }

    /**
     * Publish rollback error event for saga compensation
     * Uses ValidationRuleSettingFailedEvent
     */
    public void publishRollbackErrorEvent(String commandId, String errorCode, String errorMessage) {
        try {
            publishErrorEvent(commandId, null, errorCode, "Rollback failed: " + errorMessage);
            logger.info("Published rollback error as ValidationRuleSettingFailedEvent: commandId={}, errorCode={}",
                    commandId, errorCode);
        } catch (Exception e) {
            throw new ValidationException("Failed to publish rollback error event for commandId: " + commandId, e);
        }
    }

    /**
     * Publish ValidationRuleSettingAppliedEvent to Kafka with proper headers
     */
    private void publishAppliedEvent(
            ValidationRuleSettingAppliedEvent event,
            String key) {
        try {
            // Use KafkaUtils to send with JSON serialization
            kafkaUtils.send(eventTopic, key, event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            logger.debug("Published ValidationRuleSettingAppliedEvent to topic {}: key={}, partition={}, offset={}",
                                    eventTopic, key,
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        } else {
                            logger.error("Failed to publish ValidationRuleSettingAppliedEvent to Kafka: topic={}, key={}",
                                    eventTopic, key, ex);
                            throw new ValidationException("Failed to publish event to Kafka", ex);
                        }
                    });

        } catch (Exception e) {
            throw new ValidationException("Failed to publish ValidationRuleSettingAppliedEvent to Kafka - topic: " + eventTopic + ", key: " + key, e);
        }
    }

    /**
     * Publish ValidationRuleSettingFailedEvent to Kafka with proper headers
     */
    private void publishFailedEvent(
            ValidationRuleSettingFailedEvent event,
            String key) {
        try {
            // Use KafkaUtils to send with JSON serialization
            kafkaUtils.send(eventTopic, key, event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            logger.debug("Published ValidationRuleSettingFailedEvent to topic {}: key={}, partition={}, offset={}",
                                    eventTopic, key,
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        } else {
                            logger.error("Failed to publish ValidationRuleSettingFailedEvent to Kafka: topic={}, key={}",
                                    eventTopic, key, ex);
                            throw new ValidationException("Failed to publish event to Kafka", ex);
                        }
                    });

        } catch (Exception e) {
            throw new ValidationException("Failed to publish ValidationRuleSettingFailedEvent to Kafka - topic: " + eventTopic + ", key: " + key, e);
        }
    }

    private ValidationRuleSettingAppliedEventPayload.TimeframeResult buildTimeframeResultForApplied(
            SettingValidationRuleCommandHandler.CommandProcessingResult result) {
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
            timezone = timeframeData.getTimezone();
        }

        return ValidationRuleSettingAppliedEventPayload.TimeframeResult.builder()
                .timeFrameId(result.getTimeFrameId())
                .validFrom(validFrom)
                .validTo(validTo)
                .mode(mode)
                .timezone(timezone)
                .build();
    }

}
