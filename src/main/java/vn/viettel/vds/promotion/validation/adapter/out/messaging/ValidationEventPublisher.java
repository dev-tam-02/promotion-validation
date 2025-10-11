package vn.viettel.vds.promotion.validation.adapter.out.messaging;

import com.promix.platform.messaging.autoconfigure.utils.KafkaUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.adapter.out.messaging.event.RuleCreatedEvent;
import vn.viettel.vds.promotion.validation.adapter.out.messaging.event.RuleDeployedEvent;
import vn.viettel.vds.promotion.validation.adapter.out.messaging.event.ValidationCompletedEvent;
import vn.viettel.vds.promotion.validation.adapter.out.messaging.event.ValidationFailedEvent;
import vn.viettel.vds.promotion.validation.application.port.out.EventPublisherPort;
import vn.viettel.vds.promotion.validation.domain.model.Rule;
import vn.viettel.vds.promotion.validation.domain.model.ValidationResult;

import java.time.Instant;
import java.util.UUID;

/**
 * Adapter for publishing validation events to Kafka using KafkaUtils
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ValidationEventPublisher implements EventPublisherPort {

    // Topic names
    private static final String VALIDATION_EVENTS_TOPIC = "validation-events";
    private static final String RULE_EVENTS_TOPIC = "rule-management-events";
    private static final String ALERT_EVENTS_TOPIC = "validation-alerts";
    private final KafkaUtils kafkaUtils;

    @Override
    public void publishValidationCompleted(ValidationResult result) {
        log.debug("Publishing validation completed event for: {}",
                result.getValidationId());

        try {
            ValidationCompletedEvent event = ValidationCompletedEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .validationId(result.getValidationId())
                    .decision(result.getDecision().toString())
                    .isAllowed(result.isAllowed())
                    .reasonCodes(result.getReasonCodes())
                    .explanations(result.getExplanations())
                    .processingTimeMs(result.getProcessingTimeMs())
                    .timestamp(result.getTimestamp())
                    .eventTimestamp(Instant.now())
                    .build();

            // KafkaUtils automatically adds traceId and metadata
            kafkaUtils.send(VALIDATION_EVENTS_TOPIC, result.getValidationId(), event)
                    .whenComplete((sendResult, ex) -> {
                        if (ex == null) {
                            log.info("Validation completed event published: {}, partition={}, offset={}",
                                    event.getEventId(),
                                    sendResult.getRecordMetadata().partition(),
                                    sendResult.getRecordMetadata().offset());
                        } else {
                            log.error("Failed to publish validation completed event", ex);
                        }
                    });

        } catch (Exception e) {
            log.error("Failed to publish validation completed event", e);
        }
    }

    @Override
    public void publishValidationFailed(String validationId, String error) {
        log.debug("Publishing validation failed event for: {}", validationId);

        try {
            ValidationFailedEvent event = ValidationFailedEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .validationId(validationId)
                    .errorMessage(error)
                    .timestamp(Instant.now())
                    .build();

            // Send to validation events topic
            kafkaUtils.send(VALIDATION_EVENTS_TOPIC, validationId, event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish validation failed event to {}", VALIDATION_EVENTS_TOPIC, ex);
                        }
                    });

            // Also send to alerts topic for monitoring
            kafkaUtils.send(ALERT_EVENTS_TOPIC, validationId, event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("Validation failed event published: {}", event.getEventId());
                        } else {
                            log.error("Failed to publish validation failed event to {}", ALERT_EVENTS_TOPIC, ex);
                        }
                    });

        } catch (Exception e) {
            log.error("Failed to publish validation failed event", e);
        }
    }

    @Override
    public void publishRuleCreated(Rule rule) {
        log.debug("Publishing rule created event for: {}", rule.getId());

        try {
            RuleCreatedEvent event = RuleCreatedEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .ruleId(rule.getId())
                    .ruleCode(rule.getRuleCode())
                    .ruleName(rule.getName())
                    .ruleType(rule.getType() != null ? rule.getType().toString() : null)
                    .priority(rule.getPriority())
                    .active(rule.isActive())
                    .createdAt(rule.getCreatedAt())
                    .eventTimestamp(Instant.now())
                    .build();

            kafkaUtils.send(RULE_EVENTS_TOPIC, rule.getId(), event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("Rule created event published: {}", event.getEventId());
                        } else {
                            log.error("Failed to publish rule created event", ex);
                        }
                    });

        } catch (Exception e) {
            log.error("Failed to publish rule created event", e);
        }
    }

    @Override
    public void publishRuleUpdated(Rule rule) {
        log.debug("Publishing rule updated event for: {}", rule.getId());

        try {
            // Using RuleCreatedEvent with different event type
            RuleCreatedEvent event = RuleCreatedEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .ruleId(rule.getId())
                    .ruleCode(rule.getRuleCode())
                    .ruleName(rule.getName())
                    .ruleType(rule.getType() != null ? rule.getType().toString() : null)
                    .priority(rule.getPriority())
                    .active(rule.isActive())
                    .createdAt(rule.getUpdatedAt()) // Use updated time
                    .eventTimestamp(Instant.now())
                    .eventType("RULE_UPDATED")
                    .build();

            kafkaUtils.send(RULE_EVENTS_TOPIC, rule.getId(), event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("Rule updated event published: {}", event.getEventId());
                        } else {
                            log.error("Failed to publish rule updated event", ex);
                        }
                    });

        } catch (Exception e) {
            log.error("Failed to publish rule updated event", e);
        }
    }

    @Override
    public void publishRuleDeleted(String ruleId) {
        log.debug("Publishing rule deleted event for: {}", ruleId);

        try {
            RuleCreatedEvent event = RuleCreatedEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .ruleId(ruleId)
                    .eventType("RULE_DELETED")
                    .eventTimestamp(Instant.now())
                    .build();

            kafkaUtils.send(RULE_EVENTS_TOPIC, ruleId, event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("Rule deleted event published: {}", event.getEventId());
                        } else {
                            log.error("Failed to publish rule deleted event", ex);
                        }
                    });

        } catch (Exception e) {
            log.error("Failed to publish rule deleted event", e);
        }
    }

    @Override
    public void publishRuleDeployed(String ruleSetId, boolean success) {
        log.debug("Publishing rule deployed event for rule set: {}", ruleSetId);

        try {
            RuleDeployedEvent event = RuleDeployedEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .ruleSetId(ruleSetId)
                    .deploymentSuccess(success)
                    .deployedAt(Instant.now())
                    .eventTimestamp(Instant.now())
                    .build();

            kafkaUtils.send(RULE_EVENTS_TOPIC, ruleSetId, event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("Rule deployed event published: {}", event.getEventId());
                        } else {
                            log.error("Failed to publish rule deployed event", ex);
                        }
                    });

            if (!success) {
                // Send to alerts topic if deployment failed
                kafkaUtils.send(ALERT_EVENTS_TOPIC, ruleSetId, event)
                        .whenComplete((result, ex) -> {
                            if (ex != null) {
                                log.error("Failed to publish alert for failed deployment", ex);
                            }
                        });
            }

        } catch (Exception e) {
            log.error("Failed to publish rule deployed event", e);
        }
    }

    @Override
    public void publishHighRiskValidation(ValidationResult result) {
        log.debug("Publishing high risk validation alert for: {}",
                result.getValidationId());

        try {
            ValidationCompletedEvent event = ValidationCompletedEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .validationId(result.getValidationId())
                    .decision(result.getDecision().toString())
                    .isAllowed(result.isAllowed())
                    .reasonCodes(result.getReasonCodes())
                    .explanations(result.getExplanations())
                    .eventType("HIGH_RISK_VALIDATION")
                    .timestamp(result.getTimestamp())
                    .eventTimestamp(Instant.now())
                    .build();

            // Send to alerts topic for monitoring
            kafkaUtils.send(ALERT_EVENTS_TOPIC, result.getValidationId(), event)
                    .whenComplete((sendResult, ex) -> {
                        if (ex == null) {
                            log.info("High risk validation alert published: {}", event.getEventId());
                        } else {
                            log.error("Failed to publish high risk validation alert", ex);
                        }
                    });

        } catch (Exception e) {
            log.error("Failed to publish high risk validation alert", e);
        }
    }

    @Override
    public void publishAsync(Object event) {
        log.debug("Publishing async event: {}", event.getClass().getSimpleName());
        // Delegate to appropriate specific method or log
    }
}