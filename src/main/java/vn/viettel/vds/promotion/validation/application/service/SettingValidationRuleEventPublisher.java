package vn.viettel.vds.promotion.validation.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.promix.platform.core.util.IdGenerator;
import com.promix.platform.messaging.autoconfigure.utils.KafkaUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import vn.viettel.vds.promotion.schema.validation.command.SettingValidationRuleCommand;
import vn.viettel.vds.promotion.schema.validation.command.TimeFrame;
import vn.viettel.vds.promotion.validation.domain.entity.Assignment;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Publisher for SettingValidationRuleEvent
 */
@Service
public class SettingValidationRuleEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(SettingValidationRuleEventPublisher.class);

    private final KafkaUtils kafkaUtils;
    private final ObjectMapper objectMapper;
    private final CommandMappingService mappingService;
    private final String eventTopic;
    private final String serviceName;

    public SettingValidationRuleEventPublisher(
            KafkaUtils kafkaUtils,
            ObjectMapper objectMapper,
            CommandMappingService mappingService,
            @Value("${promix.messaging.topics.setting-validation-rule-events:setting-validation-rule-events}") String eventTopic,
            @Value("${spring.application.name:validation}") String serviceName) {
        this.kafkaUtils = kafkaUtils;
        this.objectMapper = objectMapper;
        this.mappingService = mappingService;
        this.eventTopic = eventTopic;
        this.serviceName = serviceName;
    }

    /**
     * Publish success event
     */
    public void publishSuccessEvent(String commandId, SettingValidationRuleCommandHandler.CommandProcessingResult result) {
        try {
            Map<String, Object> event = createSuccessEvent(commandId, result);
            publishEvent(event, result.getAssignment().getId());

            logger.info("Published SettingValidationRuleEvent success: commandId={}, assignmentId={}",
                    commandId, result.getAssignment().getId());

        } catch (Exception e) {
            logger.error("Failed to publish success event: commandId={}", commandId, e);
            throw new RuntimeException("Failed to publish success event", e);
        }
    }

    /**
     * Publish error event
     */
    public void publishErrorEvent(String commandId, String errorCode, String errorMessage) {
        try {
            Map<String, Object> event = createErrorEvent(commandId, errorCode, errorMessage);
            publishEvent(event, commandId);

            logger.info("Published SettingValidationRuleEvent error: commandId={}, errorCode={}",
                    commandId, errorCode);

        } catch (Exception e) {
            logger.error("Failed to publish error event: commandId={}", commandId, e);
            throw new RuntimeException("Failed to publish error event", e);
        }
    }

    /**
     * Publish dead letter event
     */
    public void publishDeadLetterEvent(String commandId, SettingValidationRuleCommand originalCommand) {
        try {
            Map<String, Object> event = createDeadLetterEvent(commandId, originalCommand);
            publishEvent(event, commandId);

            logger.warn("Published SettingValidationRuleEvent dead letter: commandId={}", commandId);

        } catch (Exception e) {
            logger.error("Failed to publish dead letter event: commandId={}", commandId, e);
        }
    }

    /**
     * Create success event payload
     */
    private Map<String, Object> createSuccessEvent(String commandId, SettingValidationRuleCommandHandler.CommandProcessingResult result) {
        Assignment assignment = result.getAssignment();
        CommandMappingService.ApplicabilityStats stats = mappingService.calculateApplicabilityStats(result.getApplicabilityData());

        Map<String, Object> event = new HashMap<>();
        event.put("id", IdGenerator.generateId());
        event.put("type", "SettingValidationRuleEvent");
        event.put("source", serviceName);
        event.put("subject", assignment.getId());
        event.put("occurredAt", Instant.now().toEpochMilli());
        event.put("version", 1);

        // Create payload
        Map<String, Object> payload = new HashMap<>();
        payload.put("commandId", commandId);
        payload.put("isSuccess", true);
        payload.put("errorCode", null);
        payload.put("errorMessage", null);

        // Assignment result
        Map<String, Object> assignmentResult = new HashMap<>();
        assignmentResult.put("assignmentId", assignment.getId());
        assignmentResult.put("ruleId", assignment.getRuleId());
        assignmentResult.put("active", assignment.getActive());
        assignmentResult.put("trafficPercent", assignment.getTrafficPercent());
        assignmentResult.put("priority", 0); // TODO: Add priority to assignment entity
        payload.put("assignmentResult", assignmentResult);

        // Applicability result
        Map<String, Object> applicabilityResult = new HashMap<>();
        applicabilityResult.put("subjectType", stats.getSubjectType());
        applicabilityResult.put("subjectKey", stats.getSubjectKey());
        applicabilityResult.put("includedItemsCount", stats.getIncludedItemsCount());
        applicabilityResult.put("excludedItemsCount", stats.getExcludedItemsCount());
        applicabilityResult.put("includedAll", stats.isIncludedAll());
        payload.put("applicabilityResult", applicabilityResult);

        // Timeframe result (if provided)
        if (result.getTimeFrameId() != null) {
            Map<String, Object> timeframeResult = new HashMap<>();
            timeframeResult.put("timeFrameId", result.getTimeFrameId());

            // Extract timeframe details from command data
            TimeFrame timeframeData = result.getTimeframeData();
            if (timeframeData != null) {
                // Extract from validityTimeframe if available
                Long validFrom = null;
                Long validTo = null;
                if (timeframeData.getValidityTimeframe() != null) {
                    java.time.Instant startDate = timeframeData.getValidityTimeframe().getStartDate();
                    java.time.Instant expirationDate = timeframeData.getValidityTimeframe().getExpirationDate();
                    validFrom = startDate != null ? startDate.toEpochMilli() : null;
                    validTo = expirationDate != null ? expirationDate.toEpochMilli() : null;
                }

                timeframeResult.put("validFrom", validFrom);
                timeframeResult.put("validTo", validTo);
                timeframeResult.put("mode", timeframeData.getMode().toString());
                timeframeResult.put("timezone", timeframeData.getTimezone().toString());
            } else {
                timeframeResult.put("validFrom", null);
                timeframeResult.put("validTo", null);
                timeframeResult.put("mode", "ALLOW");
                timeframeResult.put("timezone", "UTC");
            }
            payload.put("timeframeResult", timeframeResult);
        }

        payload.put("processedBy", serviceName);
        payload.put("processedAt", Instant.now().toEpochMilli());

        event.put("payload", payload);
        event.put("metadata", createMetadata(commandId));

        return event;
    }

    /**
     * Create error event payload
     */
    private Map<String, Object> createErrorEvent(String commandId, String errorCode, String errorMessage) {
        Map<String, Object> event = new HashMap<>();
        event.put("id", IdGenerator.generateId());
        event.put("type", "SettingValidationRuleEvent");
        event.put("source", serviceName);
        event.put("subject", commandId);
        event.put("occurredAt", Instant.now().toEpochMilli());
        event.put("version", 1);

        // Create payload
        Map<String, Object> payload = new HashMap<>();
        payload.put("commandId", commandId);
        payload.put("isSuccess", false);
        payload.put("errorCode", errorCode);
        payload.put("errorMessage", errorMessage);
        payload.put("assignmentResult", null);
        payload.put("applicabilityResult", null);
        payload.put("timeframeResult", null);
        payload.put("processedBy", serviceName);
        payload.put("processedAt", Instant.now().toEpochMilli());

        event.put("payload", payload);
        event.put("metadata", createMetadata(commandId));

        return event;
    }

    /**
     * Create dead letter event payload
     */
    private Map<String, Object> createDeadLetterEvent(String commandId, SettingValidationRuleCommand originalCommand) {
        Map<String, Object> event = new HashMap<>();
        event.put("id", IdGenerator.generateId());
        event.put("type", "SettingValidationRuleEvent");
        event.put("source", serviceName);
        event.put("subject", commandId);
        event.put("occurredAt", Instant.now().toEpochMilli());
        event.put("version", 1);

        // Create payload
        Map<String, Object> payload = new HashMap<>();
        payload.put("commandId", commandId);
        payload.put("isSuccess", false);
        payload.put("errorCode", "DEAD_LETTER");
        payload.put("errorMessage", "Command sent to dead letter queue after max retries");
        payload.put("assignmentResult", null);
        payload.put("applicabilityResult", null);
        payload.put("timeframeResult", null);
        payload.put("processedBy", serviceName);
        payload.put("processedAt", Instant.now().toEpochMilli());

        event.put("payload", payload);

        // Include original command in metadata for debugging
        Map<String, Object> metadata = createMetadata(commandId);
        metadata.put("originalCommand", originalCommand);
        event.put("metadata", metadata);

        return event;
    }

    /**
     * Publish event to Kafka using KafkaUtils
     */
    private void publishEvent(Map<String, Object> event, String key) {
        try {
            // KafkaUtils will automatically add traceId and message metadata
            kafkaUtils.send(eventTopic, key, event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            logger.debug("Published event to topic {}: key={}, partition={}, offset={}",
                                    eventTopic, key,
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        } else {
                            logger.error("Failed to publish event to Kafka: topic={}, key={}", eventTopic, key, ex);
                            throw new RuntimeException("Failed to publish event to Kafka", ex);
                        }
                    });

        } catch (Exception e) {
            logger.error("Failed to publish event to Kafka: topic={}, key={}", eventTopic, key, e);
            throw new RuntimeException("Failed to publish event to Kafka", e);
        }
    }

    /**
     * Create event metadata
     */
    private Map<String, Object> createMetadata(String commandId) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("correlationId", commandId);
        metadata.put("serviceName", serviceName);
        metadata.put("serviceVersion", "1.0.0"); // TODO: Get from build properties
        return metadata;
    }

    /**
     * Extract nested value from timeframe data using dot notation
     */
    private Object extractTimeframeValue(Map<String, Object> data, String path) {
        String[] parts = path.split("\\.");
        Object current = data;

        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(part);
                if (current == null) {
                    return null;
                }
            } else {
                return null;
            }
        }

        return current;
    }
}