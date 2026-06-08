package vn.viettel.vds.promotion.validation.application.service;

import com.promix.platform.core.exception.BusinessRuleException;
import com.promix.platform.core.util.IdGenerator;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.viettel.vds.promotion.validation.adapter.in.messaging.dto.UpdateValidationRuleCommandDTO;
import vn.viettel.vds.promotion.validation.adapter.in.messaging.mapper.UpdateValidationRuleCommandDTOMapper;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.ValidationRuleSnapshotEntity;
import vn.viettel.vds.promotion.validation.application.port.out.RuleBindingPersistencePort;
import vn.viettel.vds.promotion.validation.application.port.out.ValidationRuleRepositoryPort;
import vn.viettel.vds.promotion.validation.command.SettingValidationRuleCommand;
import vn.viettel.vds.promotion.validation.command.UpdateValidationRuleCommand;
import vn.viettel.vds.promotion.validation.command.UpdateValidationRuleCommand.ApplicabilityScope;
import vn.viettel.vds.promotion.validation.command.UpdateValidationRuleCommand.TimeFrame;
import vn.viettel.vds.promotion.validation.command.UpdateValidationRuleCommand.UpdateValidationRuleCommandPayload;
import vn.viettel.vds.promotion.validation.domain.exception.InvalidCommandDataException;
import vn.viettel.vds.promotion.validation.domain.model.RuleBinding;
import vn.viettel.vds.promotion.validation.event.ValidationSettingUpdateResultEvent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service to handle UpdateValidationRuleCommand processing.
 * <p>
 * Refactored to use unified RuleBinding model.
 */
@ConditionalOnProperty(prefix = "promix.messaging", name = "enabled", havingValue = "true")
@Service
@Transactional
public class UpdateValidationRuleCommandHandler {

    private static final Logger logger = LoggerFactory.getLogger(UpdateValidationRuleCommandHandler.class);
    private static final String SOURCE = "validation-service";

    private final RuleBindingPersistencePort ruleBindingPort;
    private final ValidationRuleRepositoryPort validationRulePort;
    private final IdempotencyService idempotencyService;
    private final RulePublishingService rulePublishingService;
    private final ValidationRuleSnapshotService snapshotService;
    private final SettingValidationRuleCommandHandler settingHandler;
    private final Validator validator;
    private final UpdateValidationRuleCommandDTOMapper dtoMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.validation-event:promotion_validation_event}")
    private String validationEventTopic;

    public UpdateValidationRuleCommandHandler(
            RuleBindingPersistencePort ruleBindingPort,
            ValidationRuleRepositoryPort validationRulePort,
            IdempotencyService idempotencyService,
            RulePublishingService rulePublishingService,
            ValidationRuleSnapshotService snapshotService,
            SettingValidationRuleCommandHandler settingHandler,
            Validator validator,
            UpdateValidationRuleCommandDTOMapper dtoMapper,
            KafkaTemplate<String, Object> kafkaTemplate) {
        this.ruleBindingPort = ruleBindingPort;
        this.validationRulePort = validationRulePort;
        this.idempotencyService = idempotencyService;
        this.rulePublishingService = rulePublishingService;
        this.snapshotService = snapshotService;
        this.settingHandler = settingHandler;
        this.validator = validator;
        this.dtoMapper = dtoMapper;
        this.kafkaTemplate = kafkaTemplate;
    }

    @SuppressWarnings("java:S2139")
    public boolean handleCommand(UpdateValidationRuleCommand command) {
        String commandId = command.getId();

        try {
            logger.info("Processing UpdateValidationRuleCommand: commandId={}", commandId);

            if (idempotencyService.isProcessed(commandId)) {
                logger.info("Update command already processed: commandId={}", commandId);
                return true;
            }

            validateCommand(command);

            UpdateValidationRuleCommandPayload payload = command.getPayload();
            if (payload == null) {
                logger.error("Update command payload is null: commandId={}", commandId);
                publishErrorEvent(commandId, null, "INVALID_PAYLOAD", "Command payload is missing");
                return false;
            }

            String objectType = payload.getObjectType();
            String objectId = payload.getObjectId();
            String assignmentId = payload.getAssignmentId();

            logger.info("Update request: objectType={}, objectId={}, assignmentId={}", objectType, objectId, assignmentId);

            // Find existing binding
            RuleBinding existingBinding = findExistingBinding(assignmentId, objectType, objectId);
            if (existingBinding == null) {
                // issue #4: a campaign created WITHOUT validation criteria has no RuleBinding
                // (create saga gates on hasValidationCriteria — PROM-972). A first-time
                // timeframe update must therefore CREATE the rule+binding (upsert) instead of
                // failing — otherwise the update saga rolls back and the timeframe is lost.
                logger.info("No existing binding for objectType={}, objectId={} — creating one (first-time update backfill)",
                        objectType, objectId);
                return createBindingForFirstTimeUpdate(payload, commandId);
            }

            // Capture BEFORE_UPDATE snapshot so saga compensation can call
            // RevertValidationRuleCommand to restore the prior rule state. The
            // snapshot is keyed by the rule's current optimistic-lock version,
            // which becomes targetVersion for revert. Also captures the binding
            // row so revert can restore validFrom/validTo/timezone/rrule/
            // timeWindows/applicability — fields the update path actually
            // modifies on the binding (separate entity from validation_rule).
            // Skip silently when the binding has no ruleId (legacy/incomplete rows).
            Long snapshotVersion = captureBeforeUpdateSnapshot(
                    existingBinding.getRuleId(), existingBinding.getId(),
                    command.getMetadata(), commandId);

            // Update binding
            RuleBinding updatedBinding = updateBinding(existingBinding, payload);

            // Deploy to validation-engine
            deployRuleToEngine(updatedBinding, payload.getApplicableTo());

            // Mark as processed
            idempotencyService.markAsProcessed(commandId, "Update completed successfully");

            // Publish success event — include snapshotVersion as previousVersion
            // so saga can later send RevertValidationRuleCommand with the right target.
            publishSuccessEvent(commandId, objectId, updatedBinding, snapshotVersion);

            logger.info("Successfully processed UpdateValidationRuleCommand: commandId={}", commandId);
            return true;

        } catch (BusinessRuleException e) {
            logger.error("Validation failed: commandId={}, error={}", commandId, e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error: commandId={}", commandId, e);
            String objectId = command.getPayload() != null ? command.getPayload().getObjectId() : null;
            publishErrorEvent(commandId, objectId, "PROCESSING_ERROR", "Unexpected error: " + e.getMessage());
            return false;
        }
    }

    private void validateCommand(UpdateValidationRuleCommand command) {
        if (command == null || command.getPayload() == null) {
            logger.error("Received null command or null payload");
            throw new InvalidCommandDataException("INVALID_COMMAND", "Command or payload is null");
        }

        UpdateValidationRuleCommandDTO dto = dtoMapper.toDTO(command);
        if (dto == null) {
            logger.error("Failed to convert command to DTO");
            throw new InvalidCommandDataException("INVALID_COMMAND", "Failed to convert command to DTO");
        }

        Set<ConstraintViolation<UpdateValidationRuleCommandDTO>> violations = validator.validate(dto);

        if (!violations.isEmpty()) {
            String errorDetails = violations.stream()
                    .map(violation -> String.format("%s: %s (value: %s)",
                            violation.getPropertyPath().toString(),
                            violation.getMessage(),
                            violation.getInvalidValue()))
                    .collect(Collectors.joining("; "));

            String errorMessage = String.format("Validation failed with %d error(s): %s", violations.size(), errorDetails);

            logger.error("UpdateValidationRuleCommand validation failed: commandId={}, errors={}",
                    command.getId(), errorDetails);

            throw new InvalidCommandDataException("METHOD_ARGUMENT_NOT_VALID", errorMessage);
        }

        logger.debug("UpdateValidationRuleCommand validation passed: commandId={}", command.getId());
    }

    private RuleBinding findExistingBinding(String assignmentId, String objectType, String objectId) {
        // Try to find by ID first
        if (assignmentId != null && !assignmentId.isBlank()) {
            Optional<RuleBinding> binding = ruleBindingPort.findById(assignmentId);
            if (binding.isPresent()) {
                return binding.get();
            }
        }

        // Search by object
        List<RuleBinding> bindings = ruleBindingPort.findByObject(objectType, objectId);
        return bindings.isEmpty() ? null : bindings.get(0);
    }

    private RuleBinding updateBinding(RuleBinding existing, UpdateValidationRuleCommandPayload payload) {
        RuleBinding.RuleBindingBuilder builder = existing.toBuilder()
                .updatedAt(Instant.now())
                .updatedBy(payload.getUpdatedBy() != null ? payload.getUpdatedBy() : "system");

        if (payload.getRuleId() != null && !payload.getRuleId().isBlank()) {
            builder.ruleId(payload.getRuleId());
        }
        if (payload.getActive() != null) {
            builder.active(payload.getActive());
        }
        if (payload.getTrafficPercent() != null) {
            builder.trafficPercent(payload.getTrafficPercent());
        }
        if (payload.getPriority() != null) {
            builder.priority(payload.getPriority());
        }

        applyApplicabilityUpdate(builder, payload.getApplicableTo());
        applyTimeframeUpdate(builder, payload.getTimeframe());

        return ruleBindingPort.save(builder.build());
    }

    /**
     * issue #4 — handle an UpdateValidationRuleCommand for a campaign that has no
     * RuleBinding yet (created without validation criteria). Builds a fresh binding
     * from the update payload using the same field-mapping as a normal update, then
     * delegates rule creation + deployment to
     * {@link SettingValidationRuleCommandHandler#backfillRuleAndBinding} so create and
     * backfill stay single-sourced. Publishes the same
     * {@link ValidationSettingUpdateResultEvent} success shape as a normal update but
     * with previousVersion=null, so the update saga treats compensation as a delete
     * (CREATE-style rollback) rather than a snapshot revert — leaving no orphan binding.
     */
    private boolean createBindingForFirstTimeUpdate(UpdateValidationRuleCommandPayload payload, String commandId) {
        RuleBinding.RuleBindingBuilder builder = RuleBinding.builder()
                .id(IdGenerator.generateId())
                .objectType(payload.getObjectType())
                .objectId(payload.getObjectId())
                .active(payload.getActive() == null || payload.getActive())
                .trafficPercent(payload.getTrafficPercent() != null ? payload.getTrafficPercent() : 100)
                .priority(payload.getPriority() != null ? payload.getPriority() : 1)
                .stickyKeyStrategy(RuleBinding.StickyKeyStrategy.CUSTOMER_ID)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .createdBy("system")
                .updatedBy(payload.getUpdatedBy() != null ? payload.getUpdatedBy() : "system")
                .version(0L);

        applyApplicabilityUpdate(builder, payload.getApplicableTo());
        applyTimeframeUpdate(builder, payload.getTimeframe());

        Instant startDate = null;
        Instant endDate = null;
        String timezone = null;
        TimeFrame tf = payload.getTimeframe();
        if (tf != null) {
            timezone = tf.getTimezone();
            if (tf.getValidityTimeframe() != null) {
                startDate = tf.getValidityTimeframe().getStartDate();
                endDate = tf.getValidityTimeframe().getExpirationDate();
            }
        }

        RuleBinding created = settingHandler.backfillRuleAndBinding(
                builder.build(), payload.getRuleId(), startDate, endDate, timezone);

        idempotencyService.markAsProcessed(commandId, "First-time update created binding");

        // previousVersion=null → saga compensates this as a CREATE (delete binding) on rollback.
        publishSuccessEvent(commandId, payload.getObjectId(), created, null);

        logger.info("Successfully created binding for first-time update: commandId={}, bindingId={}, ruleId={}",
                commandId, created.getId(), created.getRuleId());
        return true;
    }

    private void applyApplicabilityUpdate(RuleBinding.RuleBindingBuilder builder, ApplicabilityScope scope) {
        if (scope == null) {
            return;
        }
        builder.includedAll(Boolean.TRUE.equals(scope.getIncludedAll()));
        if (scope.getIncluded() != null) {
            List<String> includedIds = scope.getIncluded().stream()
                    .map(UpdateValidationRuleCommand.ApplicabilityRule::getId)
                    .toList();
            builder.includedProducts(includedIds);
        }
        if (scope.getExcluded() != null) {
            List<String> excludedIds = scope.getExcluded().stream()
                    .map(UpdateValidationRuleCommand.ApplicabilityRule::getId)
                    .toList();
            builder.excludedProducts(excludedIds);
        }
    }

    private void applyTimeframeUpdate(RuleBinding.RuleBindingBuilder builder, TimeFrame timeframe) {
        if (timeframe == null) {
            return;
        }
        if (timeframe.getTimezone() != null) {
            builder.timezone(timeframe.getTimezone());
        }

        String interval = null;
        String duration = null;
        String activityDurationAfterPublishing = null;
        if (timeframe.getValidityTimeframe() != null) {
            var validity = timeframe.getValidityTimeframe();
            builder.validFrom(validity.getStartDate());
            builder.validTo(validity.getExpirationDate());
            interval = validity.getInterval();
            duration = validity.getDuration();
            activityDurationAfterPublishing = validity.getActivityDurationAfterPublishing();
        }

        // Build rrule using interval (FREQ/INTERVAL derived from ISO 8601 period
        // when present) alongside daysOfWeek (BYDAY) — same shape as the create
        // path so edits preserve "lặp lại sau N ngày/tuần/tháng" semantics.
        List<Integer> daysOfWeek = timeframe.getValidityDaysOfWeek();
        if ((daysOfWeek != null && !daysOfWeek.isEmpty()) || (interval != null && !interval.isBlank())) {
            builder.rrule(buildRRuleFromTimeframe(daysOfWeek, interval));
        }

        // Persist duration in dedicated column AND in scope_time_windows JSON
        // alongside the timezone — mirrors create path so subsequent reads of
        // scope_time_windows see consistent (duration, timezone) metadata.
        if (duration != null && !duration.isBlank()) {
            builder.duration(duration);
            Map<String, Object> stw = new HashMap<>();
            stw.put("duration", duration);
            if (timeframe.getTimezone() != null) {
                stw.put("timezone", timeframe.getTimezone());
            }
            builder.scopeTimeWindows(stw);
        }

        if (activityDurationAfterPublishing != null && !activityDurationAfterPublishing.isBlank()) {
            builder.activityDurationAfterPublishing(activityDurationAfterPublishing);
        }

        if (timeframe.getValidityHoursPerDay() != null && !timeframe.getValidityHoursPerDay().isEmpty()) {
            builder.timeWindows(buildTimeWindowsGroupedByRange(timeframe.getValidityHoursPerDay()));
        }
    }

    /**
     * Group hours-per-day entries that share the same (start, end) into a
     * single {@link RuleBinding.TimeWindow} carrying the union of their days.
     * Mirrors {@link SettingValidationRuleCommandHandler#buildTimeWindowsGroupedByRange};
     * kept here so the update path also persists per-window daysOfWeek
     * (otherwise edits would lose the day filter even when create stored it).
     */
    private List<RuleBinding.TimeWindow> buildTimeWindowsGroupedByRange(
            List<UpdateValidationRuleCommand.ValidityHoursPerDay> hours) {
        LinkedHashMap<String, RuleBinding.TimeWindow> byRange = new LinkedHashMap<>();
        for (UpdateValidationRuleCommand.ValidityHoursPerDay h : hours) {
            String start = extractTimeOnly(h.getStartTime());
            String end = extractTimeOnly(h.getExpirationTime());
            String key = start + "-" + end;
            RuleBinding.TimeWindow window = byRange.computeIfAbsent(key, k -> RuleBinding.TimeWindow.builder()
                    .start(start)
                    .end(end)
                    .daysOfWeek(new ArrayList<>())
                    .build());
            Integer day = h.getDayOfWeek();
            if (day != null && !window.getDaysOfWeek().contains(day)) {
                window.getDaysOfWeek().add(day);
            }
        }
        for (RuleBinding.TimeWindow window : byRange.values()) {
            if (window.getDaysOfWeek().isEmpty()) {
                window.setDaysOfWeek(null);
            } else {
                Collections.sort(window.getDaysOfWeek());
            }
        }
        return new ArrayList<>(byRange.values());
    }

    private static final String DEFAULT_FREQ = "FREQ=DAILY;INTERVAL=1";

    /**
     * Build RFC 5545 RRULE from daysOfWeek (BYDAY) and ISO 8601 interval period
     * (FREQ/INTERVAL). Mirrors {@code SettingValidationRuleCommandHandler.buildRRuleFromTimeframe}
     * so create + update produce identical rrule shapes for the same inputs.
     */
    private String buildRRuleFromTimeframe(List<Integer> daysOfWeek, String interval) {
        StringBuilder rrule = new StringBuilder();
        if (interval != null && !interval.isBlank()) {
            rrule.append(parseFreqAndInterval(interval));
        } else if (daysOfWeek != null && !daysOfWeek.isEmpty()) {
            rrule.append("FREQ=WEEKLY");
        } else {
            rrule.append("FREQ=DAILY");
        }
        if (daysOfWeek != null && !daysOfWeek.isEmpty()) {
            String[] dayCodes = {"MO", "TU", "WE", "TH", "FR", "SA", "SU"};
            String byDay = daysOfWeek.stream()
                    .filter(day -> day >= 1 && day <= 7)
                    .map(day -> dayCodes[day - 1])
                    .collect(Collectors.joining(","));
            if (!byDay.isEmpty()) {
                rrule.append(";BYDAY=").append(byDay);
            }
        }
        return rrule.toString();
    }

    /**
     * Parse ISO 8601 period (PnD/PnW/PnM/PnY/PTnH) into RFC 5545 FREQ;INTERVAL.
     * Falls back to {@link #DEFAULT_FREQ} on parse failure. Mirrors
     * {@code SettingValidationRuleCommandHandler.parseFreqAndInterval}.
     */
    private String parseFreqAndInterval(String isoPeriod) {
        if (isoPeriod == null || isoPeriod.isBlank()) {
            return DEFAULT_FREQ;
        }
        if (isoPeriod.startsWith("PT")) {
            try {
                java.time.Duration d = java.time.Duration.parse(isoPeriod);
                long hours = d.toHours();
                if (hours > 0) {
                    return "FREQ=HOURLY;INTERVAL=" + hours;
                }
            } catch (java.time.format.DateTimeParseException ignored) {
                // fall through
            }
            return DEFAULT_FREQ;
        }
        try {
            java.time.Period period = java.time.Period.parse(isoPeriod);
            if (period.getYears() > 0) return "FREQ=YEARLY;INTERVAL=" + period.getYears();
            if (period.getMonths() > 0) return "FREQ=MONTHLY;INTERVAL=" + period.getMonths();
            int days = period.getDays();
            if (days > 0) {
                if (days % 7 == 0) return "FREQ=WEEKLY;INTERVAL=" + (days / 7);
                return "FREQ=DAILY;INTERVAL=" + days;
            }
            return DEFAULT_FREQ;
        } catch (java.time.format.DateTimeParseException e) {
            logger.warn("Failed to parse ISO 8601 period '{}', defaulting to FREQ=DAILY;INTERVAL=1", isoPeriod);
            return DEFAULT_FREQ;
        }
    }

    private String extractTimeOnly(String timeString) {
        if (timeString == null) {
            return null;
        }
        String time = timeString.split("\\+")[0].split("-")[0];
        String[] parts = time.split(":");
        if (parts.length >= 2) {
            return parts[0] + ":" + parts[1];
        }
        return time;
    }

    private void deployRuleToEngine(RuleBinding binding, ApplicabilityScope applicableToData) {
        try {
            if (!Boolean.TRUE.equals(binding.getActive())) {
                logger.info("Skipping deployment - binding is not active: bindingId={}", binding.getId());
                return;
            }

            String ruleId = binding.getRuleId();
            if (ruleId == null || ruleId.isBlank()) {
                logger.info("Skipping deployment - no ruleId: bindingId={}", binding.getId());
                return;
            }

            if (!validationRulePort.existsById(ruleId)) {
                logger.warn("Rule not found for deployment: ruleId={}", ruleId);
                return;
            }

            // Convert applicability scope for publishing
            var publishResult = rulePublishingService.publishRule(ruleId, binding.getId(),
                    convertApplicabilityScope(applicableToData));

            if (publishResult.isSuccess()) {
                RuleBinding updated = binding.toBuilder()
                        .bundleHash(publishResult.getBundleHash())
                        .build();
                ruleBindingPort.save(updated);
                logger.info("Rule deployed successfully: ruleId={}, bundleHash={}", ruleId, publishResult.getBundleHash());
            } else {
                logger.error("Failed to deploy rule: ruleId={}, error={}", ruleId, publishResult.getErrorMessage());
            }

        } catch (Exception e) {
            logger.error("Error deploying rule to engine: bindingId={}", binding.getId(), e);
        }
    }

    private SettingValidationRuleCommand.ApplicabilityScope convertApplicabilityScope(ApplicabilityScope source) {
        if (source == null) {
            return null;
        }

        return SettingValidationRuleCommand.ApplicabilityScope.builder()
                .includedAll(source.getIncludedAll())
                .included(source.getIncluded() != null
                        ? source.getIncluded().stream().map(this::convertApplicabilityRule).toList()
                        : null)
                .excluded(source.getExcluded() != null
                        ? source.getExcluded().stream().map(this::convertApplicabilityRule).toList()
                        : null)
                .build();
    }

    private SettingValidationRuleCommand.ApplicabilityRule convertApplicabilityRule(
            UpdateValidationRuleCommand.ApplicabilityRule r) {
        SettingValidationRuleCommand.ObjectType objectType = r.getObject() != null
                ? SettingValidationRuleCommand.ObjectType.valueOf(r.getObject().name())
                : null;
        return SettingValidationRuleCommand.ApplicabilityRule.builder()
                .id(r.getId())
                .object(objectType)
                .build();
    }

    /**
     * Persist a BEFORE_UPDATE snapshot of the rule's current state so the saga
     * orchestrator can revert via {@code RevertValidationRuleCommand} on
     * compensation. Returns the snapshot version (= rule's pre-update
     * optimistic-lock version) for inclusion in the success event, or null when
     * the binding has no ruleId or snapshot creation fails (best-effort — do not
     * abort the update for a snapshot error).
     */
    private Long captureBeforeUpdateSnapshot(String ruleId, String bindingId,
                                              java.util.Map<String, String> commandMetadata,
                                              String commandId) {
        if (ruleId == null || ruleId.isBlank()) {
            logger.warn("Skipping BEFORE_UPDATE snapshot — binding has no ruleId (commandId={})", commandId);
            return null;
        }
        String sagaId = commandMetadata != null ? commandMetadata.get("sagaId") : null;
        if (sagaId == null || sagaId.isBlank()) {
            sagaId = commandMetadata != null ? commandMetadata.get("correlationId") : null;
        }
        try {
            var snapshot = snapshotService.createSnapshot(
                    ruleId, bindingId, sagaId, commandId,
                    ValidationRuleSnapshotEntity.SnapshotReason.BEFORE_UPDATE);
            logger.info("Created BEFORE_UPDATE snapshot: ruleId={}, bindingId={}, version={}, sagaId={}",
                    ruleId, bindingId, snapshot.getVersion(), sagaId);
            return snapshot.getVersion();
        } catch (Exception e) {
            // PROM-942 round 3: createSnapshot now runs in REQUIRES_NEW so the
            // outer transaction (updateBinding's save) is not poisoned even if
            // we land here. Keep the explicit class+message log so the original
            // serialization / constraint failure is easy to identify next time.
            logger.error("Failed to create BEFORE_UPDATE snapshot for ruleId={}, bindingId={}, sagaId={}, errorClass={}, error={}",
                    ruleId, bindingId, sagaId, e.getClass().getName(), e.getMessage(), e);
            return null;
        }
    }

    private void publishSuccessEvent(String commandId, String objectId, RuleBinding binding, Long snapshotVersion) {
        try {
            Long currentVersion = snapshotVersion != null ? snapshotVersion + 1 : null;
            ValidationSettingUpdateResultEvent event = ValidationSettingUpdateResultEvent.builder()
                    .id(IdGenerator.generateId())
                    .aggregate("Validation")
                    .type("ValidationSettingUpdateResultEvent")
                    .source(SOURCE)
                    .subject(objectId)
                    .occurredAt(Instant.now())
                    .version(1)
                    .assignmentId(binding.getId())
                    .ruleId(binding.getRuleId())
                    .payload(ValidationSettingUpdateResultEvent.ValidationSettingUpdatePayload.builder()
                            .commandId(commandId)
                            .isSuccess(true)
                            .processedAt(Instant.now())
                            .updateResult(ValidationSettingUpdateResultEvent.UpdateResult.builder()
                                    .assignmentId(binding.getId())
                                    .ruleId(binding.getRuleId())
                                    .active(binding.getActive())
                                    .previousVersion(snapshotVersion)
                                    .currentVersion(currentVersion)
                                    .build())
                            .build())
                    .metadata(new HashMap<>())
                    .build();

            kafkaTemplate.send(validationEventTopic, objectId, event);

            logger.info("Published ValidationSettingUpdateResultEvent: commandId={}, previousVersion={}, currentVersion={}",
                    commandId, snapshotVersion, currentVersion);

        } catch (Exception e) {
            logger.error("Failed to publish success event: commandId={}", commandId, e);
        }
    }

    private void publishErrorEvent(String commandId, String objectId, String errorCode, String errorMessage) {
        try {
            ValidationSettingUpdateResultEvent event = ValidationSettingUpdateResultEvent.builder()
                    .id(IdGenerator.generateId())
                    .aggregate("Validation")
                    .type("ValidationSettingUpdateResultEvent")
                    .source(SOURCE)
                    .subject(objectId != null ? objectId : commandId)
                    .occurredAt(Instant.now())
                    .version(1)
                    .errorMessage(errorMessage)
                    .error(errorCode)
                    .payload(ValidationSettingUpdateResultEvent.ValidationSettingUpdatePayload.builder()
                            .commandId(commandId)
                            .isSuccess(false)
                            .errorMessage(errorMessage)
                            .processedAt(Instant.now())
                            .build())
                    .metadata(new HashMap<>())
                    .build();

            kafkaTemplate.send(validationEventTopic, objectId != null ? objectId : commandId, event);

            logger.info("Published error event: commandId={}, errorCode={}", commandId, errorCode);

        } catch (Exception e) {
            logger.error("Failed to publish error event: commandId={}", commandId, e);
        }
    }

    public void handleDeadLetterCommand(UpdateValidationRuleCommand command) {
        String commandId = command.getId();
        logger.error("Processing dead letter UpdateValidationRuleCommand: commandId={}", commandId);

        // If already successfully processed, skip failure event to prevent duplicate SUCCESS+FAILURE
        if (idempotencyService.isProcessed(commandId)) {
            logger.info("Update command already processed successfully, skipping DLQ failure event: commandId={}", commandId);
            return;
        }

        publishErrorEvent(
                commandId,
                command.getPayload() != null ? command.getPayload().getObjectId() : null,
                "DLQ_PROCESSING",
                "Command moved to dead letter queue"
        );
    }
}
