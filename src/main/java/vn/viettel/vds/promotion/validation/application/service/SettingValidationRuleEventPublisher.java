package vn.viettel.vds.promotion.validation.application.service;

import com.promix.platform.core.util.IdGenerator;
import com.promix.platform.messaging.autoconfigure.utils.KafkaUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import vn.viettel.vds.promotion.validation.command.SettingValidationRuleCommand.TimeFrame;
import vn.viettel.vds.promotion.validation.domain.exception.EventPublishingException;
import vn.viettel.vds.promotion.validation.domain.model.RuleBinding;
import vn.viettel.vds.promotion.validation.event.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Publisher for Validation Rule Setting Events using JSON serialization.
 * Publishes 2 separate events:
 * - ValidationRuleSettingAppliedEvent: khi setting rule thành công
 * - ValidationRuleSettingFailedEvent: khi setting rule thất bại
 */
@ConditionalOnProperty(prefix = "promix.messaging", name = "enabled", havingValue = "true")
@Service
public class SettingValidationRuleEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(SettingValidationRuleEventPublisher.class);

    // String literal constants
    private static final String CORRELATION_ID_KEY = "correlationId";
    private static final String SERVICE_NAME_KEY = "serviceName";
    private static final String SERVICE_VERSION_KEY = "serviceVersion";
    private static final String SERVICE_VERSION = "1.0.0";
    private static final String AGGREGATE_VALIDATION = "Validation";
    private static final String DEFAULT_SUBJECT_TYPE = "CAMPAIGN";
    private static final String EVENT_TYPE_APPLIED = "ValidationRuleSettingAppliedEvent";
    private static final String EVENT_TYPE_FAILED = "ValidationRuleSettingFailedEvent";
    private static final String COMPENSATION_STATUS_SUCCESS = "SUCCESS";

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
    @SuppressWarnings("java:S2139")
    public void publishSuccessEvent(String commandId, SettingValidationRuleCommandHandler.CommandProcessingResult result) {
        try {
            logger.info("[SAGA-DEBUG] publishSuccessEvent ENTRY: commandId={}, topic={}", commandId, eventTopic);
            ValidationRuleSettingAppliedEvent event = createSuccessEvent(commandId, result);

            publishAppliedEvent(event, result.getRuleBinding().getId());

            logger.info("[SAGA-DEBUG] publishSuccessEvent kafka send initiated: commandId={}, topic={}, bindingId={}",
                    commandId, eventTopic, result.getRuleBinding().getId());
            logger.info("Published ValidationRuleSettingAppliedEvent: commandId={}, bindingId={}",
                    commandId, result.getRuleBinding().getId());

        } catch (Exception e) {
            logger.info("[SAGA-DEBUG] publishSuccessEvent FAILED: commandId={}, error={}", commandId, e.getMessage());
            throw new EventPublishingException("Failed to publish success event for commandId: " + commandId, e);
        }
    }

    /**
     * Publish error event - ValidationRuleSettingFailedEvent
     */
    @SuppressWarnings("java:S2139")
    public void publishErrorEvent(String commandId, String campaignId, String errorCode, String errorMessage) {
        try {
            logger.info("[SAGA-DEBUG] publishErrorEvent ENTRY: commandId={}, campaignId={}, errorCode={}, topic={}",
                    commandId, campaignId, errorCode, eventTopic);
            ValidationRuleSettingFailedEvent event = createErrorEvent(commandId, campaignId, errorCode, errorMessage);
            String subject = campaignId != null ? campaignId : commandId;
            publishFailedEvent(event, subject);

            logger.info("[SAGA-DEBUG] publishErrorEvent kafka send initiated: commandId={}, errorCode={}, topic={}",
                    commandId, errorCode, eventTopic);
            logger.info("Published ValidationRuleSettingFailedEvent: commandId={}, campaignId={}, errorCode={}",
                    commandId, campaignId, errorCode);

        } catch (Exception e) {
            logger.info("[SAGA-DEBUG] publishErrorEvent FAILED: commandId={}, error={}", commandId, e.getMessage());
            throw new EventPublishingException("Failed to publish error event for commandId: " + commandId, e);
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

        RuleBinding binding = result.getRuleBinding();
        CommandMappingService.ApplicabilityStats stats =
                mappingService.calculateApplicabilityStats(result.getApplicabilityData());

        // Build Assignment Result — populate bundleHash and sourceVersion so the
        // pp-rule-engine consumer can upsert the assignment idempotently.
        ValidationRuleSettingAppliedEventPayload.AssignmentResult assignmentResult =
                ValidationRuleSettingAppliedEventPayload.AssignmentResult.builder()
                        .assignmentId(binding.getId())
                        .ruleId(binding.getRuleId())
                        .bundleHash(binding.getBundleHash())
                        .sourceVersion(binding.getVersion())
                        .active(Boolean.TRUE.equals(binding.getActive()))
                        .trafficPercent(binding.getTrafficPercent() != null ? binding.getTrafficPercent() : 100)
                        .priority(binding.getPriority() != null ? binding.getPriority() : 0)
                        .build();

        // Build Applicability Result — carry the canonical (subjectType, subjectKey)
        // straight from the binding so the data plane can key off the same pair.
        ValidationRuleSettingAppliedEventPayload.ApplicabilityResult applicabilityResult =
                ValidationRuleSettingAppliedEventPayload.ApplicabilityResult.builder()
                        .subjectType(normalizeSubjectType(binding.getObjectType()))
                        .subjectKey(binding.getObjectId())
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
                .subject(binding.getObjectId())
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
     * Publishes ValidationCompensationResultEvent to notify campaign saga
     */
    @SuppressWarnings("java:S2139") // Exception is logged with context before rethrowing with additional information
    public void publishRollbackSuccessEvent(String commandId, String campaignId, String validationRuleId) {
        try {
            logger.info("Publishing rollback success event: commandId={}, campaignId={}, validationRuleId={}",
                    commandId, campaignId, validationRuleId);

            // Build compensation result
            ValidationCompensationResultEvent.CompensationResult compensationResult =
                    ValidationCompensationResultEvent.CompensationResult.builder()
                            .assignmentId(validationRuleId)
                            .ruleId(validationRuleId)
                            .rollbackAction("DEACTIVATE")
                            .unassignmentId(validationRuleId)
                            .deactivatedAt(Instant.now())
                            .build();

            // Build payload
            ValidationCompensationResultEvent.ValidationCompensationPayload payload =
                    ValidationCompensationResultEvent.ValidationCompensationPayload.builder()
                            .commandId(commandId)
                            .isSuccess(true)
                            .compensationStatus(COMPENSATION_STATUS_SUCCESS)
                            .compensationResult(compensationResult)
                            .processedBy(serviceName)
                            .processedAt(Instant.now())
                            .build();

            // Build metadata
            Map<String, String> metadata = new HashMap<>();
            metadata.put(CORRELATION_ID_KEY, commandId);
            metadata.put(SERVICE_NAME_KEY, serviceName);
            metadata.put(SERVICE_VERSION_KEY, SERVICE_VERSION);

            // Build event
            ValidationCompensationResultEvent event = ValidationCompensationResultEvent.builder()
                    .id(IdGenerator.generateId())
                    .aggregate(AGGREGATE_VALIDATION)
                    .type("ValidationCompensationResultEvent")
                    .source(serviceName)
                    .subject(campaignId != null ? campaignId : commandId)
                    .occurredAt(Instant.now())
                    .version(1)
                    .payload(payload)
                    .metadata(metadata)
                    .compensationStatus(COMPENSATION_STATUS_SUCCESS)
                    .assignmentId(validationRuleId)
                    .ruleId(validationRuleId)
                    .unassignmentId(validationRuleId)
                    .deactivatedAt(Instant.now())
                    .build();

            // Publish event
            String key = campaignId != null ? campaignId : commandId;
            kafkaUtils.send(eventTopic, key, event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            logger.info("Published ValidationCompensationResultEvent: commandId={}, campaignId={}, topic={}, partition={}, offset={}",
                                    commandId, campaignId, eventTopic,
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        } else {
                            logger.error("Failed to publish ValidationCompensationResultEvent: commandId={}, campaignId={}",
                                    commandId, campaignId, ex);
                        }
                    });

        } catch (Exception e) {
            logger.error("Error publishing rollback success event: commandId={}, campaignId={}", commandId, campaignId, e);
            throw new EventPublishingException("Failed to publish rollback success event for commandId: " + commandId, e);
        }
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
            throw new EventPublishingException("Failed to publish rollback error event for commandId: " + commandId, e);
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
            logger.info("[SAGA-DEBUG] Kafka send APPLIED event: topic={}, key={}", eventTopic, key);
            kafkaUtils.send(eventTopic, key, event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            logger.info("[SAGA-DEBUG] Kafka send APPLIED callback SUCCESS: topic={}, key={}, partition={}, offset={}",
                                    eventTopic, key,
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                            logger.debug("Published ValidationRuleSettingAppliedEvent to topic {}: key={}, partition={}, offset={}",
                                    eventTopic, key,
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        } else {
                            logger.info("[SAGA-DEBUG] Kafka send APPLIED callback FAIL: topic={}, key={}, error={}",
                                    eventTopic, key, ex.getMessage());
                            logger.error("Failed to publish ValidationRuleSettingAppliedEvent to Kafka: topic={}, key={}",
                                    eventTopic, key, ex);
                            throw new EventPublishingException("Failed to publish event to Kafka", ex);
                        }
                    });

        } catch (Exception e) {
            throw new EventPublishingException("Failed to publish ValidationRuleSettingAppliedEvent to Kafka - topic: " + eventTopic + ", key: " + key, e);
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
            logger.info("[SAGA-DEBUG] Kafka send FAILED event: topic={}, key={}", eventTopic, key);
            kafkaUtils.send(eventTopic, key, event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            logger.info("[SAGA-DEBUG] Kafka send FAILED callback SUCCESS: topic={}, key={}, partition={}, offset={}",
                                    eventTopic, key,
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                            logger.debug("Published ValidationRuleSettingFailedEvent to topic {}: key={}, partition={}, offset={}",
                                    eventTopic, key,
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        } else {
                            logger.info("[SAGA-DEBUG] Kafka send FAILED callback FAIL: topic={}, key={}, error={}",
                                    eventTopic, key, ex.getMessage());
                            logger.error("Failed to publish ValidationRuleSettingFailedEvent to Kafka: topic={}, key={}",
                                    eventTopic, key, ex);
                            throw new EventPublishingException("Failed to publish event to Kafka", ex);
                        }
                    });

        } catch (Exception e) {
            throw new EventPublishingException("Failed to publish ValidationRuleSettingFailedEvent to Kafka - topic: " + eventTopic + ", key: " + key, e);
        }
    }

    private ValidationRuleSettingAppliedEventPayload.TimeframeResult buildTimeframeResultForApplied(
            SettingValidationRuleCommandHandler.CommandProcessingResult result) {
        RuleBinding binding = result.getRuleBinding();

        // Check if binding has temporal constraints
        if (binding == null || !binding.hasTemporalConstraints()) {
            return null;
        }

        Long validFrom = binding.getValidFrom() != null ? binding.getValidFrom().toEpochMilli() : null;
        Long validTo = binding.getValidTo() != null ? binding.getValidTo().toEpochMilli() : null;
        String timezone = binding.getTimezone() != null ? binding.getTimezone() : "Asia/Ho_Chi_Minh";

        TimeFrame timeframeData = result.getTimeframeData();
        String mode = timeframeData != null && timeframeData.getMode() != null
                ? timeframeData.getMode().toString()
                : "REQUIRED";

        return ValidationRuleSettingAppliedEventPayload.TimeframeResult.builder()
                .timeFrameId(binding.getId()) // Use binding ID as timeframe reference
                .validFrom(validFrom)
                .validTo(validTo)
                .mode(mode)
                .timezone(timezone)
                .build();
    }

    /**
     * Publish a typed ValidationRuleDeletedEvent for a single binding removal.
     * <p>
     * The pp-rule-engine consumer reads {@code subjectType} / {@code subjectKey}
     * from the payload to locate the matching assignment row and fast-check
     * config row to evict. Passing the binding directly (instead of just an ID)
     * lets us carry those canonical coordinates without a second repository
     * lookup inside the publisher.
     */
    @SuppressWarnings("java:S2139")
    public void publishDeleteSuccessEvent(String commandId, String campaignId, RuleBinding binding) {
        String validationRuleId = binding != null ? binding.getId() : null;
        String subjectType = binding != null ? normalizeSubjectType(binding.getObjectType()) : DEFAULT_SUBJECT_TYPE;
        String subjectKey = binding != null && binding.getObjectId() != null ? binding.getObjectId() : campaignId;
        try {
            logger.info("Publishing ValidationRuleDeletedEvent: commandId={}, campaignId={}, subject={}:{}",
                    commandId, campaignId, subjectType, subjectKey);

            ValidationRuleDeletedEventPayload payload = ValidationRuleDeletedEventPayload.builder()
                    .commandId(commandId)
                    .campaignId(campaignId)
                    .subjectType(subjectType)
                    .subjectKey(subjectKey)
                    .validationRuleId(validationRuleId)
                    .deletedBy(serviceName)
                    .deletedAt(Instant.now().toEpochMilli())
                    .build();

            ValidationRuleDeletedEvent event = ValidationRuleDeletedEvent.builder()
                    .id(IdGenerator.generateId())
                    .aggregate(AGGREGATE_VALIDATION)
                    .type("ValidationRuleDeletedEvent")
                    .source(serviceName)
                    .subject(subjectKey != null ? subjectKey : commandId)
                    .occurredAt(Instant.now())
                    .version(1)
                    .payload(payload)
                    .metadata(buildMetadata(commandId))
                    .build();

            String key = subjectKey != null ? subjectKey : commandId;
            kafkaUtils.send(eventTopic, key, event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            logger.info("Published ValidationRuleDeletedEvent: commandId={}, subject={}:{}, partition={}, offset={}",
                                    commandId, subjectType, subjectKey,
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        } else {
                            logger.error("Failed to publish ValidationRuleDeletedEvent: commandId={}, subject={}:{}",
                                    commandId, subjectType, subjectKey, ex);
                        }
                    });

        } catch (Exception e) {
            logger.error("Error publishing delete success event: commandId={}, campaignId={}", commandId, campaignId, e);
            throw new EventPublishingException("Failed to publish delete success event for commandId: " + commandId, e);
        }
    }

    /**
     * Publish delete error event cho DeleteValidationRuleCommand.
     * Sử dụng ValidationRuleSettingFailedEvent.
     *
     * @param commandId    Command ID
     * @param campaignId   Campaign ID
     * @param errorCode    Error code
     * @param errorMessage Error message
     */
    public void publishDeleteErrorEvent(String commandId, String campaignId, String errorCode, String errorMessage) {
        try {
            publishErrorEvent(commandId, campaignId, errorCode, "Delete failed: " + errorMessage);
            logger.info("Published delete error as ValidationRuleSettingFailedEvent: commandId={}, errorCode={}",
                    commandId, errorCode);
        } catch (Exception e) {
            throw new EventPublishingException("Failed to publish delete error event for commandId: " + commandId, e);
        }
    }

    /**
     * Publish a typed ValidationRuleEnabledEvent for the enabled binding.
     * <p>
     * Includes the post-enable {@code bundleHash} so the data-plane consumer
     * can re-upsert the assignment row using the current compiled bundle
     * without a separate snapshot fetch.
     */
    @SuppressWarnings("java:S2139")
    public void publishEnableSuccessEvent(String commandId, String campaignId, RuleBinding binding) {
        String validationRuleId = binding != null ? binding.getId() : null;
        String subjectType = binding != null ? normalizeSubjectType(binding.getObjectType()) : DEFAULT_SUBJECT_TYPE;
        String subjectKey = binding != null && binding.getObjectId() != null ? binding.getObjectId() : campaignId;
        String bundleHash = binding != null ? binding.getBundleHash() : null;
        try {
            logger.info("Publishing ValidationRuleEnabledEvent: commandId={}, campaignId={}, subject={}:{}, bundleHash={}",
                    commandId, campaignId, subjectType, subjectKey, bundleHash);

            ValidationRuleEnabledEventPayload payload = ValidationRuleEnabledEventPayload.builder()
                    .commandId(commandId)
                    .campaignId(campaignId)
                    .subjectType(subjectType)
                    .subjectKey(subjectKey)
                    .validationRuleId(validationRuleId)
                    .bundleHash(bundleHash)
                    .enabledBy(serviceName)
                    .enabledAt(Instant.now().toEpochMilli())
                    .build();

            ValidationRuleEnabledEvent event = ValidationRuleEnabledEvent.builder()
                    .id(IdGenerator.generateId())
                    .aggregate(AGGREGATE_VALIDATION)
                    .type("ValidationRuleEnabledEvent")
                    .source(serviceName)
                    .subject(subjectKey != null ? subjectKey : commandId)
                    .occurredAt(Instant.now())
                    .version(1)
                    .payload(payload)
                    .metadata(buildMetadata(commandId))
                    .build();

            String key = subjectKey != null ? subjectKey : commandId;
            kafkaUtils.send(eventTopic, key, event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            logger.info("Published ValidationRuleEnabledEvent: commandId={}, subject={}:{}, partition={}, offset={}",
                                    commandId, subjectType, subjectKey,
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        } else {
                            logger.error("Failed to publish ValidationRuleEnabledEvent: commandId={}, subject={}:{}",
                                    commandId, subjectType, subjectKey, ex);
                        }
                    });

        } catch (Exception e) {
            logger.error("Error publishing enable success event: commandId={}, campaignId={}", commandId, campaignId, e);
            throw new EventPublishingException("Failed to publish enable success event for commandId: " + commandId, e);
        }
    }

    /**
     * Publish enable error event cho EnableValidationRuleCommand.
     *
     * @param commandId    Command ID
     * @param campaignId   Campaign ID
     * @param errorCode    Error code
     * @param errorMessage Error message
     */
    public void publishEnableErrorEvent(String commandId, String campaignId, String errorCode, String errorMessage) {
        try {
            String subject = campaignId != null ? campaignId : commandId;

            ValidationRuleEnableFailedEventPayload payload = ValidationRuleEnableFailedEventPayload.builder()
                    .commandId(commandId)
                    .campaignId(campaignId)
                    .errorCode(errorCode)
                    .errorMessage(errorMessage)
                    .failedAt(Instant.now().toEpochMilli())
                    .build();

            ValidationRuleEnableFailedEvent event = ValidationRuleEnableFailedEvent.builder()
                    .id(IdGenerator.generateId())
                    .aggregate(AGGREGATE_VALIDATION)
                    .type("ValidationRuleEnableFailedEvent")
                    .source(serviceName)
                    .subject(subject)
                    .occurredAt(Instant.now())
                    .version(1)
                    .payload(payload)
                    .metadata(buildMetadata(commandId))
                    .build();

            kafkaUtils.send(eventTopic, subject, event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            logger.info("Published ValidationRuleEnableFailedEvent: commandId={}, campaignId={}, errorCode={}",
                                    commandId, campaignId, errorCode);
                        } else {
                            logger.error("Failed to publish ValidationRuleEnableFailedEvent: commandId={}, campaignId={}",
                                    commandId, campaignId, ex);
                        }
                    });
        } catch (Exception e) {
            throw new EventPublishingException("Failed to publish enable error event for commandId: " + commandId, e);
        }
    }

    /**
     * Publish a typed ValidationRuleDisabledEvent when a binding is deactivated.
     * <p>
     * The data-plane consumer treats disable as a hard eviction of the
     * assignment row so no traffic reaches the (now-inactive) bundle.
     */
    @SuppressWarnings("java:S2139")
    public void publishDisableSuccessEvent(String commandId, String campaignId, RuleBinding binding) {
        String validationRuleId = binding != null ? binding.getId() : null;
        String subjectType = binding != null ? normalizeSubjectType(binding.getObjectType()) : DEFAULT_SUBJECT_TYPE;
        String subjectKey = binding != null && binding.getObjectId() != null ? binding.getObjectId() : campaignId;
        try {
            logger.info("Publishing ValidationRuleDisabledEvent: commandId={}, campaignId={}, subject={}:{}",
                    commandId, campaignId, subjectType, subjectKey);

            ValidationRuleDisabledEventPayload payload = ValidationRuleDisabledEventPayload.builder()
                    .commandId(commandId)
                    .campaignId(campaignId)
                    .subjectType(subjectType)
                    .subjectKey(subjectKey)
                    .validationRuleId(validationRuleId)
                    .disabledBy(serviceName)
                    .disabledAt(Instant.now().toEpochMilli())
                    .build();

            ValidationRuleDisabledEvent event = ValidationRuleDisabledEvent.builder()
                    .id(IdGenerator.generateId())
                    .aggregate(AGGREGATE_VALIDATION)
                    .type("ValidationRuleDisabledEvent")
                    .source(serviceName)
                    .subject(subjectKey != null ? subjectKey : commandId)
                    .occurredAt(Instant.now())
                    .version(1)
                    .payload(payload)
                    .metadata(buildMetadata(commandId))
                    .build();

            String key = subjectKey != null ? subjectKey : commandId;
            kafkaUtils.send(eventTopic, key, event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            logger.info("Published ValidationRuleDisabledEvent: commandId={}, subject={}:{}, partition={}, offset={}",
                                    commandId, subjectType, subjectKey,
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        } else {
                            logger.error("Failed to publish ValidationRuleDisabledEvent: commandId={}, subject={}:{}",
                                    commandId, subjectType, subjectKey, ex);
                        }
                    });

        } catch (Exception e) {
            logger.error("Error publishing disable success event: commandId={}, campaignId={}", commandId, campaignId, e);
            throw new EventPublishingException("Failed to publish disable success event for commandId: " + commandId, e);
        }
    }

    private Map<String, String> buildMetadata(String commandId) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put(CORRELATION_ID_KEY, commandId);
        metadata.put(SERVICE_NAME_KEY, serviceName);
        metadata.put(SERVICE_VERSION_KEY, SERVICE_VERSION);
        return metadata;
    }

    /**
     * Normalize RuleBinding.objectType to the canonical uppercase subjectType
     * used by the data plane (pp-rule-engine stores "CAMPAIGN", "VOUCHER", etc.).
     */
    private String normalizeSubjectType(String objectType) {
        return objectType != null ? objectType.toUpperCase() : DEFAULT_SUBJECT_TYPE;
    }

    /**
     * Publish disable error event cho DisableValidationRuleCommand.
     *
     * @param commandId    Command ID
     * @param campaignId   Campaign ID
     * @param errorCode    Error code
     * @param errorMessage Error message
     */
    public void publishDisableErrorEvent(String commandId, String campaignId, String errorCode, String errorMessage) {
        try {
            String subject = campaignId != null ? campaignId : commandId;

            ValidationRuleDisableFailedEventPayload payload = ValidationRuleDisableFailedEventPayload.builder()
                    .commandId(commandId)
                    .campaignId(campaignId)
                    .errorCode(errorCode)
                    .errorMessage(errorMessage)
                    .failedAt(Instant.now().toEpochMilli())
                    .build();

            ValidationRuleDisableFailedEvent event = ValidationRuleDisableFailedEvent.builder()
                    .id(IdGenerator.generateId())
                    .aggregate(AGGREGATE_VALIDATION)
                    .type("ValidationRuleDisableFailedEvent")
                    .source(serviceName)
                    .subject(subject)
                    .occurredAt(Instant.now())
                    .version(1)
                    .payload(payload)
                    .metadata(buildMetadata(commandId))
                    .build();

            kafkaUtils.send(eventTopic, subject, event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            logger.info("Published ValidationRuleDisableFailedEvent: commandId={}, campaignId={}, errorCode={}",
                                    commandId, campaignId, errorCode);
                        } else {
                            logger.error("Failed to publish ValidationRuleDisableFailedEvent: commandId={}, campaignId={}",
                                    commandId, campaignId, ex);
                        }
                    });
        } catch (Exception e) {
            throw new EventPublishingException("Failed to publish disable error event for commandId: " + commandId, e);
        }
    }

}
