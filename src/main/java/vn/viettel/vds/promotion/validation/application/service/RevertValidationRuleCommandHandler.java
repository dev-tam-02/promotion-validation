package vn.viettel.vds.promotion.validation.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.promix.platform.core.exception.factory.ExceptionFactory;
import com.promix.platform.core.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.ValidationRuleEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.ValidationRuleSnapshotEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.ValidationRuleJpaRepository;
import vn.viettel.vds.promotion.validation.command.RevertValidationRuleCommand;
import vn.viettel.vds.promotion.validation.command.RevertValidationRuleCommand.RevertValidationRuleCommandPayload;
import vn.viettel.vds.promotion.validation.event.ValidationRuleRevertedEvent;
import vn.viettel.vds.promotion.validation.event.ValidationRuleRevertFailedEvent;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Handler for RevertValidationRuleCommand.
 *
 * This handler is responsible for restoring a validation rule to a previous version
 * during saga compensation. It uses the snapshot stored before the update to restore
 * the aggregate to its previous state.
 *
 * Flow:
 * 1. Receive RevertValidationRuleCommand with targetVersion and currentVersion
 * 2. Validate the command (idempotency, version check)
 * 3. Load snapshot for targetVersion
 * 4. Restore ValidationRule aggregate from snapshot
 * 5. Publish ValidationRuleRevertedEvent
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RevertValidationRuleCommandHandler {

    private static final String SOURCE = "validation-service";

    private final ValidationRuleSnapshotService snapshotService;
    private final ValidationRuleJpaRepository validationRuleRepository;
    private final IdempotencyService idempotencyService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${kafka.topics.validation-event:promotion_validation_event}")
    private String validationEventTopic;

    /**
     * Handle RevertValidationRuleCommand.
     *
     * @param command the revert command
     * @return true if successfully processed, false if already processed (idempotent)
     */
    @Transactional
    public boolean handleCommand(RevertValidationRuleCommand command) {
        String commandId = command.getId();

        try {
            log.info("Processing RevertValidationRuleCommand: commandId={}, sagaId={}",
                    commandId, command.getMetadata() != null ? command.getMetadata().get("sagaId") : null);

            // Idempotency check
            if (idempotencyService.isProcessed(commandId)) {
                log.info("Command already processed (idempotent check): commandId={}", commandId);
                return false;
            }

            // Validate payload
            RevertValidationRuleCommandPayload payload = command.getPayload();
            if (payload == null) {
                log.error("Command payload is null: commandId={}", commandId);
                throw ExceptionFactory.createValidationException("INVALID_PAYLOAD", "Command payload is missing");
            }

            String validationRuleId = payload.getValidationRuleId();
            Long targetVersion = payload.getTargetVersion();
            Long currentVersion = payload.getCurrentVersion();
            String sagaId = payload.getSagaId();

            log.info("Reverting validation rule: ruleId={}, from version {} to version {}, sagaId={}",
                    validationRuleId, currentVersion, targetVersion, sagaId);

            // Validate rule exists
            ValidationRuleEntity rule = validationRuleRepository.findById(validationRuleId)
                    .orElseThrow(() -> {
                        log.error("Validation rule not found: ruleId={}", validationRuleId);
                        return ExceptionFactory.createValidationException(
                                "RULE_NOT_FOUND",
                                "Validation rule not found: " + validationRuleId);
                    });

            // Validate current version matches (optimistic lock check)
            // This prevents reverting if the rule was modified after the compensation was triggered
            if (currentVersion != null && !currentVersion.equals(rule.getRuleVersion())) {
                Long actualVersion = rule.getRuleVersion();
                log.warn("Version mismatch detected: expected={}, actual={}, ruleId={}",
                        currentVersion, actualVersion, validationRuleId);

                // If actual version is LESS than expected, rule may have already been reverted
                if (actualVersion != null && actualVersion < currentVersion) {
                    log.info("Rule appears to already be reverted or at earlier version. " +
                                    "actualVersion={} < expectedVersion={}. Skipping revert.",
                            actualVersion, currentVersion);
                    // Mark as processed to prevent retry loops
                    idempotencyService.markAsProcessed(commandId, Map.of(
                            "validationRuleId", validationRuleId,
                            "status", "SKIPPED_ALREADY_REVERTED",
                            "actualVersion", actualVersion.toString(),
                            "expectedVersion", currentVersion.toString()
                    ));
                    return true;
                }

                // If actual version is GREATER than expected, rule was modified after compensation triggered
                // This is a conflict - log warning but proceed with revert as saga compensation takes priority
                log.warn("Rule was modified after compensation triggered. " +
                                "actualVersion={} > expectedVersion={}. Proceeding with revert as saga compensation takes priority.",
                        actualVersion, currentVersion);
            }

            // Check if snapshot exists for target version
            if (!snapshotService.snapshotExists(validationRuleId, targetVersion)) {
                log.error("Snapshot not found for targetVersion: ruleId={}, version={}",
                        validationRuleId, targetVersion);
                throw ExceptionFactory.createValidationException(
                        "SNAPSHOT_NOT_FOUND",
                        String.format("Snapshot not found for rule %s version %d", validationRuleId, targetVersion));
            }

            // Restore from snapshot
            ValidationRuleEntity restoredRule = snapshotService.restoreFromSnapshot(validationRuleId, targetVersion);
            log.info("Successfully restored validation rule from snapshot: ruleId={}, restoredVersion={}",
                    validationRuleId, restoredRule.getRuleVersion());

            // Mark as processed
            idempotencyService.markAsProcessed(commandId, Map.of(
                    "validationRuleId", validationRuleId,
                    "restoredToVersion", targetVersion.toString(),
                    "rolledBackFromVersion", currentVersion != null ? currentVersion.toString() : "unknown"
            ));

            // Publish success event
            publishRevertedEvent(command, restoredRule, targetVersion, currentVersion);

            log.info("Successfully processed RevertValidationRuleCommand: commandId={}, ruleId={}",
                    commandId, validationRuleId);
            return true;

        } catch (Exception e) {
            log.error("Error processing RevertValidationRuleCommand: commandId={}", commandId, e);

            // Publish failure event
            publishRevertFailedEvent(command, e.getMessage());

            throw e;
        }
    }

    /**
     * Publish ValidationRuleRevertedEvent on successful revert.
     */
    private void publishRevertedEvent(RevertValidationRuleCommand command,
                                       ValidationRuleEntity restoredRule,
                                       Long restoredToVersion,
                                       Long rolledBackFromVersion) {
        try {
            String eventId = IdGenerator.generateId();

            Map<String, String> metadata = new HashMap<>();
            if (command.getMetadata() != null) {
                metadata.putAll(command.getMetadata());
            }
            metadata.put("commandId", command.getId());
            metadata.put("source", SOURCE);

            ValidationRuleRevertedEvent.ValidationRuleRevertedEventPayload payload =
                    ValidationRuleRevertedEvent.ValidationRuleRevertedEventPayload.builder()
                            .validationRuleId(restoredRule.getId())
                            .campaignId(command.getPayload().getCampaignId())
                            .correlationId(command.getPayload().getCorrelationId())
                            .sagaId(command.getPayload().getSagaId())
                            .restoredToVersion(restoredToVersion)
                            .rolledBackFromVersion(rolledBackFromVersion)
                            .revertedAt(Instant.now())
                            .build();

            ValidationRuleRevertedEvent event = ValidationRuleRevertedEvent.builder()
                    .id(eventId)
                    .type("ValidationRuleRevertedEvent")
                    .source(SOURCE)
                    .subject(restoredRule.getId())
                    .occurredAt(Instant.now())
                    .version(1)
                    .payload(payload)
                    .metadata(metadata)
                    .build();

            kafkaTemplate.send(validationEventTopic, restoredRule.getId(), event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish ValidationRuleRevertedEvent: ruleId={}",
                                    restoredRule.getId(), ex);
                        } else {
                            log.info("Published ValidationRuleRevertedEvent: eventId={}, ruleId={}, topic={}",
                                    eventId, restoredRule.getId(), validationEventTopic);
                        }
                    });

        } catch (Exception e) {
            log.error("Error publishing ValidationRuleRevertedEvent: ruleId={}", restoredRule.getId(), e);
        }
    }

    /**
     * Publish failure event when revert fails.
     */
    private void publishRevertFailedEvent(RevertValidationRuleCommand command, String errorMessage) {
        try {
            String eventId = IdGenerator.generateId();
            RevertValidationRuleCommandPayload payload = command.getPayload();

            Map<String, String> metadata = new HashMap<>();
            if (command.getMetadata() != null) {
                metadata.putAll(command.getMetadata());
            }
            metadata.put("commandId", command.getId());
            metadata.put("source", SOURCE);
            metadata.put("errorMessage", errorMessage);

            // Build typed failure event payload
            ValidationRuleRevertFailedEvent.ValidationRuleRevertFailedPayload failurePayload =
                    ValidationRuleRevertFailedEvent.ValidationRuleRevertFailedPayload.builder()
                            .validationRuleId(payload != null ? payload.getValidationRuleId() : null)
                            .campaignId(payload != null ? payload.getCampaignId() : null)
                            .sagaId(payload != null ? payload.getSagaId() : null)
                            .correlationId(payload != null ? payload.getCorrelationId() : null)
                            .targetVersion(payload != null ? payload.getTargetVersion() : null)
                            .currentVersion(payload != null ? payload.getCurrentVersion() : null)
                            .errorMessage(errorMessage)
                            .failedAt(Instant.now())
                            .build();

            // Build typed failure event
            ValidationRuleRevertFailedEvent failureEvent = ValidationRuleRevertFailedEvent.builder()
                    .id(eventId)
                    .type("ValidationRuleRevertFailedEvent")
                    .source(SOURCE)
                    .subject(payload != null ? payload.getValidationRuleId() : null)
                    .occurredAt(Instant.now())
                    .version(1)
                    .payload(failurePayload)
                    .metadata(metadata)
                    .build();

            String key = payload != null ? payload.getValidationRuleId() : command.getId();
            kafkaTemplate.send(validationEventTopic, key, failureEvent)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish ValidationRuleRevertFailedEvent", ex);
                        } else {
                            log.info("Published ValidationRuleRevertFailedEvent: eventId={}", eventId);
                        }
                    });

        } catch (Exception e) {
            log.error("Error publishing ValidationRuleRevertFailedEvent", e);
        }
    }

    /**
     * Handle dead letter commands.
     */
    public void handleDeadLetterCommand(RevertValidationRuleCommand command) {
        String commandId = command.getId();
        log.error("Processing dead letter RevertValidationRuleCommand: commandId={}", commandId);

        try {
            log.error("Dead letter command details: commandId={}, type={}, source={}, validationRuleId={}",
                    command.getId(),
                    command.getType(),
                    command.getSource(),
                    command.getPayload() != null ? command.getPayload().getValidationRuleId() : null);
        } catch (Exception e) {
            log.error("Failed to process dead letter command: commandId={}", commandId, e);
        }
    }
}
