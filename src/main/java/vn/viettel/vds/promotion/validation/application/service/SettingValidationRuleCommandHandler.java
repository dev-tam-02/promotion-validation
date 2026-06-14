package vn.viettel.vds.promotion.validation.application.service;

import com.promix.platform.core.exception.BusinessRuleException;
import com.promix.platform.core.util.IdGenerator;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import vn.viettel.vds.promotion.validation.adapter.in.messaging.dto.SettingValidationRuleCommandDTO;
import vn.viettel.vds.promotion.validation.adapter.in.messaging.mapper.SettingValidationRuleCommandDTOMapper;
import vn.viettel.vds.promotion.validation.application.port.out.*;
import vn.viettel.vds.promotion.validation.command.SettingValidationRuleCommand;
import vn.viettel.vds.promotion.validation.command.SettingValidationRuleCommand.ApplicabilityScope;
import vn.viettel.vds.promotion.validation.command.SettingValidationRuleCommand.SettingValidationRuleCommandPayload;
import vn.viettel.vds.promotion.validation.command.SettingValidationRuleCommand.TimeFrame;
import vn.viettel.vds.promotion.validation.domain.common.ErrorCode;
import vn.viettel.vds.promotion.validation.domain.common.Result;
import vn.viettel.vds.promotion.validation.domain.exception.InvalidCommandDataException;
import vn.viettel.vds.promotion.validation.domain.exception.RuleNotFoundException;
import vn.viettel.vds.promotion.validation.domain.model.*;

import java.time.Instant;
import java.time.Period;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service to handle SettingValidationRuleCommand processing.
 * <p>
 * Refactored to use unified RuleBinding model instead of legacy Assignment + TemporalPolicy + RuleTemporalLink.
 */
@ConditionalOnProperty(prefix = "promix.messaging", name = "enabled", havingValue = "true")
@Service
public class SettingValidationRuleCommandHandler {

    private static final Logger logger = LoggerFactory.getLogger(SettingValidationRuleCommandHandler.class);

    // TEMP: tắt bước compile/deploy rule sang pp-rule-engine khi tính năng validation chưa hoàn thiện.
    // Khi false: chỉ cần lưu rule binding thành công là coi như pass, KHÔNG gọi rule-engine.
    // Đổi lại true sau khi validation hoàn thành (hoặc fix key mismatch values->segments ở rule-engine).
    private static final boolean RULE_ENGINE_DEPLOY_ENABLED = false;

    private static final String PROCESSING_ERROR = "PROCESSING_ERROR";
    private static final String SYSTEM_USER = "system";
    private static final String DEFAULT_FREQ = "FREQ=DAILY;INTERVAL=1";

    private final RuleBindingPersistencePort ruleBindingPort;
    private final ValidationRuleRepositoryPort validationRulePort;
    private final SettingValidationRuleEventPublisher eventPublisher;
    private final IdempotencyService idempotencyService;
    private final RulePublishingService rulePublishingService;
    private final Validator validator;
    private final SettingValidationRuleCommandDTOMapper dtoMapper;
    private final TransactionTemplate transactionTemplate;
    private final RuleEngineClient ruleEngineClient;
    private final DrlCompiler drlCompiler;
    private final OperatorPersistencePort operatorPort;

    public SettingValidationRuleCommandHandler(
            RuleBindingPersistencePort ruleBindingPort,
            ValidationRuleRepositoryPort validationRulePort,
            SettingValidationRuleEventPublisher eventPublisher,
            IdempotencyService idempotencyService,
            RulePublishingService rulePublishingService,
            Validator validator,
            SettingValidationRuleCommandDTOMapper dtoMapper,
            PlatformTransactionManager transactionManager,
            RuleEngineClient ruleEngineClient,
            DrlCompiler drlCompiler,
            OperatorPersistencePort operatorPort) {
        this.ruleBindingPort = ruleBindingPort;
        this.validationRulePort = validationRulePort;
        this.eventPublisher = eventPublisher;
        this.idempotencyService = idempotencyService;
        this.rulePublishingService = rulePublishingService;
        this.validator = validator;
        this.dtoMapper = dtoMapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.ruleEngineClient = ruleEngineClient;
        this.drlCompiler = drlCompiler;
        this.operatorPort = operatorPort;
    }

    /**
     * Handle SettingValidationRuleCommand.
     * <p>
     * Uses TransactionTemplate for explicit transaction control to ensure that:
     * - SUCCESS event is published ONLY AFTER the DB transaction commits
     * - If transaction commit fails, no SUCCESS event is sent (prevents SUCCESS+FAILURE race)
     * - Idempotency is marked BEFORE publishing SUCCESS (protects DLQ handler from race)
     */
    @SuppressWarnings({"java:S2139", "java:S3776"})
    public boolean handleCommand(SettingValidationRuleCommand command) {
        String commandId = command.getId();

        logger.info("[SAGA-DEBUG] ENTRY handleCommand: commandId={}, thread={}", commandId, Thread.currentThread().getName());

        try {
            logger.info("Processing SettingValidationRuleCommand: commandId={}", commandId);

            // Check idempotency
            logger.info("[SAGA-DEBUG] BEFORE idempotencyService.isProcessed: commandId={}", commandId);
            boolean alreadyProcessed = idempotencyService.isProcessed(commandId);
            logger.info("[SAGA-DEBUG] AFTER idempotencyService.isProcessed: commandId={}, result={}", commandId, alreadyProcessed);
            if (alreadyProcessed) {
                logger.info("Command already processed (idempotent check): commandId={}", commandId);
                logger.info("[SAGA-DEBUG] EXIT handleCommand (idempotent skip): commandId={}", commandId);
                return true;
            }

            // Validate command
            validateCommand(command);

            SettingValidationRuleCommandPayload payload = command.getPayload();
            if (payload == null) {
                logger.error("Command payload is null: commandId={}", commandId);
                publishErrorEvent(commandId, null, "INVALID_PAYLOAD", "Command payload is missing");
                logger.info("[SAGA-DEBUG] EXIT handleCommand (null payload): commandId={}, success=false", commandId);
                return false;
            }

            String campaignId = payload.getObjectId();

            // Process command within explicit transaction boundary.
            // Transaction commits when execute() returns — BEFORE we publish events.
            logger.info("[SAGA-DEBUG] BEFORE transactionTemplate.execute: commandId={}", commandId);
            CommandProcessingResult result = transactionTemplate.execute(status -> {
                logger.info("[SAGA-DEBUG] INSIDE TX execute callback: commandId={}, txStatus={}", commandId, status);
                CommandProcessingResult txResult = processCommand(commandId, payload);
                logger.info("[SAGA-DEBUG] INSIDE TX after processCommand: commandId={}, success={}", commandId, txResult != null ? txResult.isSuccess() : "null");
                return txResult;
            });
            logger.info("[SAGA-DEBUG] AFTER transactionTemplate.execute (TX COMMITTED): commandId={}, resultSuccess={}",
                    commandId, result != null ? result.isSuccess() : "null");

            // --- Transaction has committed here ---

            if (result == null || !result.isSuccess()) {
                String errorCode = result != null ? result.getErrorCode() : PROCESSING_ERROR;
                String errorMessage = result != null ? result.getErrorMessage() : "Processing returned null result";
                logger.info("[SAGA-DEBUG] BEFORE publishErrorEvent (tx result failed): commandId={}, errorCode={}", commandId, errorCode);
                publishErrorEvent(commandId, campaignId, errorCode, errorMessage);
                logger.error("Failed to process SettingValidationRuleCommand: commandId={}, errorCode={}, error={}",
                        commandId, errorCode, errorMessage);
                logger.info("[SAGA-DEBUG] EXIT handleCommand (tx result failed): commandId={}, success=false", commandId);
                throw new InvalidCommandDataException(errorCode, errorMessage);
            }

            // Mark as processed FIRST — so DLQ handler sees it even if SUCCESS publish is slow
            IdempotencyResultDto idempotencyDto = result.toIdempotencyDto();
            logger.info("[SAGA-DEBUG] BEFORE markAsProcessed: commandId={}, bindingId={}", commandId, idempotencyDto.getBindingId());
            idempotencyService.markAsProcessed(commandId, idempotencyDto);
            logger.info("[SAGA-DEBUG] AFTER markAsProcessed: commandId={}", commandId);

            // Publish success event AFTER transaction committed and idempotency marked
            logger.info("[SAGA-DEBUG] BEFORE publishSuccessEvent: commandId={}", commandId);
            publishSuccessEvent(commandId, result);
            logger.info("[SAGA-DEBUG] AFTER publishSuccessEvent: commandId={}", commandId);

            logger.info("Successfully processed SettingValidationRuleCommand: commandId={}", commandId);
            logger.info("[SAGA-DEBUG] EXIT handleCommand: commandId={}, success=true", commandId);
            return true;

        } catch (BusinessRuleException e) {
            logger.info("[SAGA-DEBUG] CATCH BusinessRuleException: commandId={}, exceptionClass={}, message={}",
                    commandId, e.getClass().getName(), e.getMessage());
            logger.error("Validation failed for SettingValidationRuleCommand: commandId={}, error={}",
                    commandId, e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.info("[SAGA-DEBUG] CATCH Exception: commandId={}, exceptionClass={}, message={}",
                    commandId, e.getClass().getName(), e.getMessage());
            logger.error("Unexpected error processing SettingValidationRuleCommand: commandId={}", commandId, e);
            // Only publish error if this command was NOT already successfully processed.
            // Prevents the race: SUCCESS event published -> post-processing fails -> FAILURE event.
            logger.info("[SAGA-DEBUG] CATCH checking isProcessed before error event: commandId={}", commandId);
            boolean processedInCatch = idempotencyService.isProcessed(commandId);
            logger.info("[SAGA-DEBUG] CATCH isProcessed result: commandId={}, processed={}", commandId, processedInCatch);
            if (!processedInCatch) {
                String campaignId = command.getPayload() != null ? command.getPayload().getObjectId() : null;
                logger.info("[SAGA-DEBUG] CATCH BEFORE publishErrorEvent: commandId={}", commandId);
                publishErrorEvent(commandId, campaignId, PROCESSING_ERROR, "Unexpected error: " + e.getMessage());
                logger.info("[SAGA-DEBUG] CATCH AFTER publishErrorEvent: commandId={}", commandId);
            } else {
                logger.warn("Command already marked as processed, suppressing error event: commandId={}", commandId);
                logger.info("[SAGA-DEBUG] CATCH suppressed error event (already processed): commandId={}", commandId);
            }
            logger.info("[SAGA-DEBUG] EXIT handleCommand (exception): commandId={}, success=false", commandId);
            return false;
        }
    }

    private void validateCommand(SettingValidationRuleCommand command) {
        if (command == null || command.getPayload() == null) {
            logger.error("Received null command or null payload");
            throw new InvalidCommandDataException("INVALID_COMMAND", "Command or payload is null");
        }

        SettingValidationRuleCommandDTO dto = dtoMapper.toDTO(command);
        if (dto == null) {
            logger.error("Failed to convert command to DTO: commandId={}", command.getId());
            throw new InvalidCommandDataException("INVALID_COMMAND", "Failed to convert command to DTO");
        }

        Set<ConstraintViolation<SettingValidationRuleCommandDTO>> violations = validator.validate(dto);

        if (!violations.isEmpty()) {
            String errorDetails = violations.stream()
                    .map(violation -> String.format("%s: %s (value: %s)",
                            violation.getPropertyPath().toString(),
                            violation.getMessage(),
                            violation.getInvalidValue()))
                    .collect(Collectors.joining("; "));

            String errorMessage = String.format("Validation failed with %d error(s): %s", violations.size(), errorDetails);

            logger.error("SettingValidationRuleCommand validation failed: commandId={}, errorCount={}, errors={}",
                    command.getId(), violations.size(), errorDetails);

            throw new InvalidCommandDataException("METHOD_ARGUMENT_NOT_VALID", errorMessage);
        }

        logger.debug("SettingValidationRuleCommand validation passed: commandId={}", command.getId());
    }

    /**
     * Process command by creating a unified RuleBinding
     */
    private CommandProcessingResult processCommand(String commandId, SettingValidationRuleCommandPayload payload) {
        try {
            logger.info("[SAGA-DEBUG] processCommand ENTRY: commandId={}", commandId);

            // Validate components
            Result<ComponentsData> componentsResult = validateCommandComponents(payload);
            if (componentsResult.isFailure()) {
                logger.info("[SAGA-DEBUG] processCommand components validation FAILED: commandId={}", commandId);
                return CommandProcessingResult.failure(
                        componentsResult.getFirstErrorCode().orElse(ErrorCode.COMMAND_VALIDATION_ERROR).name(),
                        componentsResult.getFirstErrorMessage().orElse("Command validation failed")
                );
            }

            ComponentsData components = componentsResult.getValue();

            // Two-path rule resolution:
            //   Path A (ruleId != null): load the user-selected rule.
            //   Path B (ruleId == null): bind WITHOUT a rule — the binding alone carries
            //   the timeframe and deploys as an assignment bundle
            //   (RulePublishingService.publishAssignmentBundle). No skeleton
            //   "Campaign Rule - {campaignId}" is created anymore; it was an inert
            //   artifact (no nodes when start/end dates are null, never registered in
            //   the engine) that leaked into FE Step 3 as a user-visible rule.
            Rule resolvedRule = resolveExistingRule(payload);
            if (resolvedRule != null) {
                // VRUL001: rules have no DRAFT state and the pre-publish DRAFT guard
                // is gone — the former Path A auto-activate (issue #5) is no longer
                // needed; a resolved rule is always publishable.
                logger.debug("[Path A] Resolved rule {} ready for deploy", resolvedRule.getId());
            } else {
                logger.info("[Path B] No ruleId in payload — creating rule-less binding for campaign {} " +
                        "(timeframe lives on the binding)", payload.getObjectId());
            }

            // Create unified RuleBinding
            RuleBinding ruleBinding = createRuleBinding(components);

            logger.info("[SAGA-DEBUG] processCommand ruleBinding created: commandId={}, bindingId={}", commandId, ruleBinding.getId());
            logger.info("Created rule binding: objectType={}, objectId={}, ruleId={}, bindingId={}",
                    components.objectType(), components.objectId(), components.ruleId(), ruleBinding.getId());

            // Deploy to validation-engine (does NOT save the binding).
            // TEMP: bỏ qua khi rule-engine deploy bị tắt — lưu binding là đủ để pass.
            DeployResult deployResult;
            if (RULE_ENGINE_DEPLOY_ENABLED) {
                deployResult = deployRuleToEngine(ruleBinding, components.ruleId(), components.applicableToData());
                ruleBinding = deployResult.binding();
            } else {
                logger.warn("[RULE-ENGINE DEPLOY DISABLED] Bỏ qua compile/deploy sang rule-engine; coi binding đã lưu là success. bindingId={}, ruleId={}",
                        ruleBinding.getId(), components.ruleId());
                deployResult = DeployResult.skipped(ruleBinding);
            }

            // Save binding once (avoids double-save OptimisticLockException)
            logger.info("[SAGA-DEBUG] BEFORE ruleBindingPort.save: commandId={}, bindingId={}", commandId, ruleBinding.getId());
            ruleBinding = ruleBindingPort.save(ruleBinding);
            logger.info("[SAGA-DEBUG] AFTER ruleBindingPort.save: commandId={}, bindingId={}", commandId, ruleBinding.getId());

            // Only a genuine deploy FAILURE blocks the saga. A SKIPPED deploy (rule has no
            // conditions/constraints — nothing to compile) is a valid success path and must NOT
            // be reported as COMPILE_DEPLOY_ERROR.
            if (deployResult.outcome() == DeployOutcome.FAILED) {
                logger.warn("Rule binding created but compilation to rule-engine failed: bindingId={}, ruleId={}",
                        ruleBinding.getId(), components.ruleId());
                return CommandProcessingResult.failure("COMPILE_DEPLOY_ERROR",
                        "Rule binding created but compilation to rule-engine failed. RuleId: " + components.ruleId());
            }

            // Step 4 (T4): Register DRL into KieBase (pp-rule-engine) for live evaluation.
            // This is a best-effort step — failures are logged but do not block the saga.
            // The T0 bootstrap loader will re-register rules on next service restart if needed.
            if (RULE_ENGINE_DEPLOY_ENABLED && resolvedRule != null
                    && resolvedRule.getNodes() != null && !resolvedRule.getNodes().isEmpty()) {
                compileDrlAndRegisterInEngine(resolvedRule);
            }

            // Create result
            logger.info("[SAGA-DEBUG] processCommand EXIT success: commandId={}, bindingId={}", commandId, ruleBinding.getId());
            return CommandProcessingResult.success(ruleBinding, components.applicableToData(), components.timeframeData());

        } catch (Exception e) {
            logger.info("[SAGA-DEBUG] processCommand EXCEPTION: commandId={}, exceptionClass={}, message={}",
                    commandId, e.getClass().getName(), e.getMessage());
            logger.error("Error processing command: commandId={}", commandId, e);
            return CommandProcessingResult.failure(PROCESSING_ERROR, e.getMessage());
        }
    }

    private Result<ComponentsData> validateCommandComponents(SettingValidationRuleCommandPayload payload) {
        String ruleId = payload.getRuleId();
        String objectType = payload.getObjectType();
        String objectId = payload.getObjectId();
        Boolean active = payload.getActive();
        Integer trafficPercent = payload.getTrafficPercent();
        ApplicabilityScope applicableToData = payload.getApplicableTo();
        TimeFrame timeframeData = payload.getTimeframe();
        Integer priority = payload.getPriority();

        if (objectType == null || objectType.isEmpty()) {
            return Result.failure(ErrorCode.COMMAND_VALIDATION_ERROR, "objectType is required in payload");
        }

        if (objectId == null || objectId.isEmpty()) {
            return Result.failure(ErrorCode.COMMAND_VALIDATION_ERROR, "objectId is required in payload");
        }

        // Check duplicate binding
        if (ruleBindingPort.existsByObjectAndRule(objectType, objectId, ruleId != null ? ruleId : "")) {
            return Result.failure(ErrorCode.DUPLICATE_ASSIGNMENT_VALIDATION_RULE,
                    "Binding already exists for objectType=" + objectType + ", objectId=" + objectId);
        }

        return Result.success(new ComponentsData(
                ruleId,
                objectType,
                objectId,
                active,
                trafficPercent,
                applicableToData,
                timeframeData,
                priority
        ));
    }

    /**
     * Create RuleBinding from command components
     */
    private RuleBinding createRuleBinding(ComponentsData components) {
        RuleBinding.RuleBindingBuilder builder = RuleBinding.builder()
                .id(IdGenerator.generateId())
                .ruleId(components.ruleId())
                .objectType(components.objectType())
                .objectId(components.objectId())
                .active(components.active() == null || components.active())
                .trafficPercent(components.trafficPercent() != null ? components.trafficPercent() : 100)
                .priority(components.priority() != null ? components.priority() : 0)
                .stickyKeyStrategy(RuleBinding.StickyKeyStrategy.CUSTOMER_ID)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .createdBy(SYSTEM_USER)
                .updatedBy(SYSTEM_USER)
                .version(0L);

        applyApplicabilityData(builder, components.applicableToData());
        applyTimeframeData(builder, components.timeframeData());

        return builder.build();
    }

    /**
     * Path A resolution: load the user-selected rule when {@code payload.ruleId} is
     * set, throwing {@link RuleNotFoundException} if it does not exist (saga
     * compensation). Returns {@code null} for Path B ({@code ruleId == null}) — the
     * binding is created WITHOUT a rule and the timeframe lives on the binding;
     * no skeleton rule is auto-generated anymore.
     */
    Rule resolveExistingRule(SettingValidationRuleCommandPayload payload) {
        if (payload.getRuleId() == null) {
            return null;
        }
        Rule existing = validationRulePort.findById(payload.getRuleId())
                .orElseThrow(() -> new RuleNotFoundException(payload.getRuleId()));
        logger.info("[Path A] Reusing existing rule {} for campaign {}",
                payload.getRuleId(), payload.getObjectId());
        return existing;
    }

    /**
     * Backfill a binding for a campaign that has no validation binding yet, then deploy
     * the bundle and return the persisted binding. Reused by
     * {@link UpdateValidationRuleCommandHandler} (issue #4) so a first-time timeframe
     * update creates the missing binding instead of failing with
     * {@code BindingNotFoundException}. When {@code existingRuleId} is provided the
     * rule is loaded (Path A); otherwise the binding is created WITHOUT a rule
     * (Path B) — the timeframe lives on the binding and deploys as an assignment
     * bundle, mirroring the create saga. The caller owns success-event publishing and
     * idempotency for its own command type. On saga rollback the binding is removed by
     * delete-style compensation (the caller publishes previousVersion=null), so no
     * orphan is left.
     *
     * @param binding        binding pre-built by the caller from its payload, WITHOUT ruleId
     * @param existingRuleId user-selected rule id, or null/blank for a rule-less binding
     * @return the persisted binding (and bundleHash when deployment succeeds)
     */
    public RuleBinding backfillRuleAndBinding(RuleBinding binding, String existingRuleId) {
        Rule rule = null;
        if (existingRuleId != null && !existingRuleId.isBlank()) {
            rule = validationRulePort.findById(existingRuleId)
                    .orElseThrow(() -> new RuleNotFoundException(existingRuleId));
            logger.info("[Backfill] Reusing existing rule {} for campaign {}", existingRuleId, binding.getObjectId());
        } else {
            logger.info("[Backfill] No ruleId — creating rule-less binding for campaign {}", binding.getObjectId());
        }

        RuleBinding toDeploy = rule != null ? binding.toBuilder().ruleId(rule.getId()).build() : binding;
        toDeploy = deployRuleToEngine(toDeploy, rule != null ? rule.getId() : null, null).binding();
        toDeploy = ruleBindingPort.save(toDeploy);

        if (rule != null && rule.getNodes() != null && !rule.getNodes().isEmpty()) {
            compileDrlAndRegisterInEngine(rule);
        }

        logger.info("[Backfill] Created rule binding for first-time update: objectId={}, ruleId={}, bindingId={}",
                binding.getObjectId(), toDeploy.getRuleId(), toDeploy.getId());
        return toDeploy;
    }

    private void applyApplicabilityData(RuleBinding.RuleBindingBuilder builder, ApplicabilityScope scope) {
        if (scope == null) {
            return;
        }
        builder.includedAll(Boolean.TRUE.equals(scope.getIncludedAll()));
        if (scope.getIncluded() != null && !scope.getIncluded().isEmpty()) {
            List<String> includedIds = scope.getIncluded().stream()
                    .map(SettingValidationRuleCommand.ApplicabilityRule::getId)
                    .toList();
            builder.includedProducts(includedIds);
        }
        if (scope.getExcluded() != null && !scope.getExcluded().isEmpty()) {
            List<String> excludedIds = scope.getExcluded().stream()
                    .map(SettingValidationRuleCommand.ApplicabilityRule::getId)
                    .toList();
            builder.excludedProducts(excludedIds);
        }
    }

    private void applyTimeframeData(RuleBinding.RuleBindingBuilder builder, TimeFrame timeframe) {
        if (timeframe == null) {
            return;
        }
        builder.timezone(timeframe.getTimezone() != null ? timeframe.getTimezone() : "Asia/Ho_Chi_Minh");

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

        List<Integer> daysOfWeek = timeframe.getValidityDaysOfWeek();
        if ((daysOfWeek != null && !daysOfWeek.isEmpty()) || interval != null) {
            builder.rrule(buildRRuleFromTimeframe(daysOfWeek, interval));
        }

        // F2: persist DURATION in dedicated column (not inside RRULE — RFC 5545 §3.3.10
        // RECUR rule parts do not include DURATION; it is a separate iCal property)
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
     * <p>
     * Example input (4 entries):
     * <pre>
     *   (1, 00:00, 01:30), (7, 00:00, 01:30),
     *   (3, 01:30, 04:00), (4, 01:30, 04:00)
     * </pre>
     * Output (2 windows):
     * <pre>
     *   TimeWindow(00:00, 01:30, [1, 7])
     *   TimeWindow(01:30, 04:00, [3, 4])
     * </pre>
     * Preserves input order of distinct ranges; days within a window are
     * sorted ascending for deterministic JSON output.
     */
    private List<RuleBinding.TimeWindow> buildTimeWindowsGroupedByRange(
            List<SettingValidationRuleCommand.ValidityHoursPerDay> hours) {
        LinkedHashMap<String, RuleBinding.TimeWindow> byRange = new LinkedHashMap<>();
        for (SettingValidationRuleCommand.ValidityHoursPerDay h : hours) {
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
                // No dayOfWeek provided → null marks "every day" (back-compat).
                window.setDaysOfWeek(null);
            } else {
                Collections.sort(window.getDaysOfWeek());
            }
        }
        return new ArrayList<>(byRange.values());
    }

    /**
     * Build RFC 5545 RRULE string from timeframe fields.
     * <p>
     * Format: FREQ=DAILY;INTERVAL=X;BYDAY=MO,TU,...
     * - If interval is null, defaults to FREQ=WEEKLY when BYDAY is specified, else FREQ=DAILY
     * - BYDAY is omitted if daysOfWeek is null or empty
     * - INTERVAL and FREQ are derived from ISO 8601 period (P1D/P1W/P1M/P1Y) via
     * {@link #parseFreqAndInterval(String)}
     * - DURATION is NOT part of RRULE (RFC 5545 §3.3.10); persist it separately in scopeTimeWindows
     */
    private String buildRRuleFromTimeframe(List<Integer> daysOfWeek, String interval) {
        StringBuilder rrule = new StringBuilder();

        // Determine FREQ and INTERVAL from ISO 8601 period string
        if (interval != null && !interval.isBlank()) {
            rrule.append(parseFreqAndInterval(interval));
        } else if (daysOfWeek != null && !daysOfWeek.isEmpty()) {
            rrule.append("FREQ=WEEKLY");
        } else {
            rrule.append("FREQ=DAILY");
        }

        // Add BYDAY
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
     * Parse ISO 8601 period/duration string into an RFC 5545 RRULE FREQ;INTERVAL fragment.
     * <p>
     * Supported mappings:
     * <ul>
     *   <li>P1D, P7D, PnD → FREQ=DAILY;INTERVAL=n</li>
     *   <li>P1W, PnW     → FREQ=WEEKLY;INTERVAL=n  (PnW stored as 7n days by java.time.Period)</li>
     *   <li>P1M, PnM     → FREQ=MONTHLY;INTERVAL=n</li>
     *   <li>P1Y, PnY     → FREQ=YEARLY;INTERVAL=n</li>
     *   <li>PT1H, PTnH   → FREQ=HOURLY;INTERVAL=n  (warn: use period, not duration, when possible)</li>
     * </ul>
     * Falls back to FREQ=DAILY;INTERVAL=1 on parse failure.
     */
    String parseFreqAndInterval(String isoPeriod) {
        if (isoPeriod == null || isoPeriod.isBlank()) {
            return DEFAULT_FREQ;
        }

        // Duration (PTnH / PTnM / PTnS) — warn and map to HOURLY for PT*H, else reject to DAILY
        if (isoPeriod.startsWith("PT")) {
            logger.warn("ISO 8601 duration '{}' supplied as interval; expected a period (P1D/P1W/P1M/P1Y). Mapping to HOURLY.", isoPeriod);
            try {
                java.time.Duration d = java.time.Duration.parse(isoPeriod);
                long hours = d.toHours();
                if (hours > 0) {
                    return "FREQ=HOURLY;INTERVAL=" + hours;
                }
            } catch (DateTimeParseException ignored) {
                // fall through
            }
            return DEFAULT_FREQ;
        }

        // Period (PnD / PnW / PnM / PnY)
        try {
            Period period = Period.parse(isoPeriod);
            if (period.getYears() > 0) {
                return "FREQ=YEARLY;INTERVAL=" + period.getYears();
            }
            if (period.getMonths() > 0) {
                return "FREQ=MONTHLY;INTERVAL=" + period.getMonths();
            }
            int days = period.getDays();
            if (days > 0) {
                if (days % 7 == 0) {
                    return "FREQ=WEEKLY;INTERVAL=" + (days / 7);
                }
                return "FREQ=DAILY;INTERVAL=" + days;
            }
            // Zero period — default
            logger.warn("ISO 8601 period '{}' resolved to zero — defaulting to FREQ=DAILY;INTERVAL=1", isoPeriod);
            return DEFAULT_FREQ;
        } catch (DateTimeParseException e) {
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

    /**
     * Compile DRL for the given rule and register it in pp-rule-engine's KieBase.
     *
     * <p>This is step 4 in the saga: after bundle compilation and binding persistence,
     * the DRL is registered via {@code POST /v1/rules} so that the rule engine can
     * evaluate it immediately — without waiting for the T0 bootstrap loader to run on restart.
     *
     * <p>Failures are logged but do NOT propagate. The saga binding and bundle remain
     * valid; the T0 bootstrap loader will re-register the rule on the next restart.
     *
     * @param rule the resolved or auto-generated rule with at least one node
     */
    private void compileDrlAndRegisterInEngine(Rule rule) {
        try {
            List<Operator> operators = operatorPort.findGlobalOperatorsByStatus(Operator.OperatorStatus.ACTIVE);
            Map<String, Operator> operatorMap = operators.stream()
                    .collect(Collectors.toMap(Operator::getName, op -> op, (a, b) -> a));
            String drl = drlCompiler.compile(rule, rule.getNodes(), operatorMap);
            ruleEngineClient.register(rule.getId(), drl);
            logger.info("[SagaRegister] Registered rule {} into KieBase ({} operators loaded)",
                    rule.getId(), operators.size());
        } catch (Exception e) {
            logger.error("[SagaRegister] Failed to register rule {} into KieBase — T0 bootstrap will recover on restart",
                    rule.getId(), e);
        }
    }

    /**
     * Deploy rule to validation-engine.
     *
     * <p>Returns a {@link DeployResult} carrying the (possibly updated) binding plus an explicit
     * outcome, so the caller can tell a legitimate SKIP (nothing to compile) apart from a real
     * FAILURE. Previously success was inferred from {@code bundleHash != null}, which wrongly
     * flagged a skipped deployment (rule with no conditions/constraints) as a compile failure.
     */
    @SuppressWarnings("java:S3776")
    private DeployResult deployRuleToEngine(RuleBinding binding, String ruleId, ApplicabilityScope applicableToData) {
        try {
            if (!Boolean.TRUE.equals(binding.getActive())) {
                logger.info("Skipping deployment - binding is not active: bindingId={}", binding.getId());
                return DeployResult.skipped(binding);
            }

            boolean hasTemporalPolicy = binding.hasTemporalConstraints();
            boolean hasApplicability = applicableToData != null;

            if (ruleId == null || ruleId.isEmpty()) {
                // Deploy assignment bundle without business rule
                if (!hasApplicability && !hasTemporalPolicy) {
                    logger.info("Skipping deployment - no ruleId and no constraints: bindingId={}", binding.getId());
                    return DeployResult.skipped(binding);
                }

                logger.info("Deploying binding bundle (no ruleId): bindingId={}, hasApplicability={}, hasTemporalPolicy={}",
                        binding.getId(), hasApplicability, hasTemporalPolicy);

                var publishResult = rulePublishingService.publishAssignmentBundle(
                        binding.getId(), applicableToData, hasTemporalPolicy);

                return handlePublishResult(binding, publishResult);
            } else {
                // Deploy business rule bundle
                var validationRuleOpt = validationRulePort.findById(ruleId);
                if (validationRuleOpt.isEmpty()) {
                    logger.warn("Validation rule not found for deployment: ruleId={}", ruleId);
                    return DeployResult.failed(binding);
                }

                var validationRule = validationRuleOpt.get();

                // If rule has no business conditions (e.g., auto-created campaign rule),
                // deploy temporal-only bundle instead of full rule compilation
                if (validationRule.getNodes() == null || validationRule.getNodes().isEmpty()) {
                    if (!hasApplicability && !hasTemporalPolicy) {
                        logger.info("Rule has no conditions and no constraints, skipping deployment: bindingId={}", binding.getId());
                        return DeployResult.skipped(binding);
                    }
                    logger.info("Rule has no business conditions, deploying temporal-only bundle: bindingId={}, ruleId={}",
                            binding.getId(), ruleId);
                    var publishResult = rulePublishingService.publishAssignmentBundle(
                            binding.getId(), applicableToData, hasTemporalPolicy);
                    return handlePublishResult(binding, publishResult);
                }

                logger.info("Deploying rule bundle: bindingId={}, ruleId={}", binding.getId(), ruleId);

                var publishResult = rulePublishingService.publishRule(ruleId, binding.getId(), applicableToData);

                return handlePublishResult(binding, publishResult);
            }
        } catch (Exception e) {
            logger.error("Error deploying rule to engine: bindingId={}, ruleId={}", binding.getId(), ruleId, e);
            return DeployResult.failed(binding);
        }
    }

    private DeployResult handlePublishResult(RuleBinding binding, RulePublishingService.RulePublishResult publishResult) {
        if (publishResult.isSuccess()) {
            // Re-fetch the binding from DB to get the managed entity and avoid OptimisticLockException
            RuleBinding current = ruleBindingPort.findById(binding.getId()).orElse(binding);
            RuleBinding updated = current.toBuilder()
                    .bundleHash(publishResult.getBundleHash())
                    .updatedAt(Instant.now())
                    .build();
            logger.info("Rule deployed successfully: bindingId={}, bundleHash={}", updated.getId(), publishResult.getBundleHash());
            return DeployResult.deployed(updated);
        } else {
            logger.error("Failed to deploy rule: bindingId={}, error={}", binding.getId(), publishResult.getErrorMessage());
            return DeployResult.failed(binding);
        }
    }

    /** Outcome of a deploy attempt — separates a legitimate skip from a real failure. */
    private enum DeployOutcome { DEPLOYED, SKIPPED, FAILED }

    /** Carries the (possibly updated) binding plus the deploy outcome. */
    private record DeployResult(RuleBinding binding, DeployOutcome outcome) {
        static DeployResult deployed(RuleBinding b) {
            return new DeployResult(b, DeployOutcome.DEPLOYED);
        }

        static DeployResult skipped(RuleBinding b) {
            return new DeployResult(b, DeployOutcome.SKIPPED);
        }

        static DeployResult failed(RuleBinding b) {
            return new DeployResult(b, DeployOutcome.FAILED);
        }
    }

    private void publishSuccessEvent(String commandId, CommandProcessingResult result) {
        try {
            eventPublisher.publishSuccessEvent(commandId, result);
        } catch (Exception e) {
            logger.error("Failed to publish success event: commandId={}", commandId, e);
        }
    }

    private void publishErrorEvent(String commandId, String campaignId, String errorCode, String errorMessage) {
        try {
            eventPublisher.publishErrorEvent(commandId, campaignId, errorCode, errorMessage);
        } catch (Exception e) {
            logger.error("Failed to publish error event: commandId={}", commandId, e);
        }
    }

    public void handleDeadLetterCommand(SettingValidationRuleCommand command) {
        String commandId = command.getId();
        logger.info("[SAGA-DEBUG] DLQ handleDeadLetterCommand ENTRY: commandId={}", commandId);
        logger.error("Processing dead letter command: commandId={}", commandId);

        // If already successfully processed, skip failure event to prevent duplicate SUCCESS+FAILURE
        logger.info("[SAGA-DEBUG] DLQ BEFORE idempotency check: commandId={}", commandId);
        boolean idempotencyResult = idempotencyService.isProcessed(commandId);
        logger.info("[SAGA-DEBUG] DLQ idempotency check result: commandId={}, processed={}", commandId, idempotencyResult);
        if (idempotencyResult) {
            logger.info("[SAGA-DEBUG] DLQ decision: SKIP (already processed in Redis): commandId={}", commandId);
            logger.info("Command already processed successfully, skipping DLQ failure event: commandId={}", commandId);
            return;
        }

        // Fallback: check DB for the binding in case Redis was unavailable during processing.
        // Redis idempotency uses fail-open strategy, so this DB check prevents false negatives.
        if (command.getPayload() != null) {
            String objectType = command.getPayload().getObjectType();
            String objectId = command.getPayload().getObjectId();
            String ruleId = command.getPayload().getRuleId();
            try {
                logger.info("[SAGA-DEBUG] DLQ BEFORE DB fallback check: commandId={}, objectType={}, objectId={}, ruleId={}",
                        commandId, objectType, objectId, ruleId);
                boolean existsInDb = ruleBindingPort.existsByObjectAndRule(objectType, objectId, ruleId != null ? ruleId : "");
                logger.info("[SAGA-DEBUG] DLQ DB fallback check result: commandId={}, existsInDb={}", commandId, existsInDb);
                if (existsInDb) {
                    logger.info("[SAGA-DEBUG] DLQ decision: SKIP (exists in DB): commandId={}", commandId);
                    logger.info("Binding exists in DB, command was processed successfully, skipping DLQ failure event: commandId={}", commandId);
                    return;
                }
            } catch (Exception e) {
                logger.info("[SAGA-DEBUG] DLQ DB fallback check EXCEPTION: commandId={}, error={}", commandId, e.getMessage());
                logger.warn("Failed to check DB for binding existence, proceeding with DLQ event: commandId={}", commandId, e);
            }
        }

        logger.info("[SAGA-DEBUG] DLQ decision: PROCESS (publishing dead letter event): commandId={}", commandId);
        try {
            eventPublisher.publishDeadLetterEvent(commandId);
            logger.info("[SAGA-DEBUG] DLQ publishDeadLetterEvent OK: commandId={}", commandId);
        } catch (Exception e) {
            logger.info("[SAGA-DEBUG] DLQ publishDeadLetterEvent FAILED: commandId={}, error={}", commandId, e.getMessage());
            logger.error("Failed to publish dead letter event: commandId={}", commandId, e);
        }
    }

    // Sonar rules S100/S107/S1172/S1186 are false positives on Java records (older sonar-java plugins
    // analyze record components/canonical constructor as regular methods with too many/unused params).
    @SuppressWarnings({"java:S100", "java:S107", "java:S1172", "java:S1186"})
    private record ComponentsData( // NOSONAR
            String ruleId,
            String objectType,
            String objectId,
            Boolean active,
            Integer trafficPercent,
            ApplicabilityScope applicableToData,
            TimeFrame timeframeData,
            Integer priority
    ) {
        // Canonical record; no extra members.
    }

    public static class IdempotencyResultDto {
        private final boolean success;
        private final String bindingId;
        private final String ruleId;

        public IdempotencyResultDto(boolean success, String bindingId, String ruleId) {
            this.success = success;
            this.bindingId = bindingId;
            this.ruleId = ruleId;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getBindingId() {
            return bindingId;
        }

        public String getRuleId() {
            return ruleId;
        }
    }

    public static class CommandProcessingResult {
        private final boolean success;
        private final String errorCode;
        private final String errorMessage;
        private final RuleBinding ruleBinding;
        private final ApplicabilityScope applicabilityData;
        private final TimeFrame timeframeData;

        private CommandProcessingResult(Builder builder) {
            this.success = builder.success;
            this.errorCode = builder.errorCode;
            this.errorMessage = builder.errorMessage;
            this.ruleBinding = builder.ruleBinding;
            this.applicabilityData = builder.applicabilityData;
            this.timeframeData = builder.timeframeData;
        }

        public static CommandProcessingResult success(RuleBinding binding,
                                                      ApplicabilityScope applicabilityData,
                                                      TimeFrame timeframeData) {
            return new Builder()
                    .success(true)
                    .ruleBinding(binding)
                    .applicabilityData(applicabilityData)
                    .timeframeData(timeframeData)
                    .build();
        }

        public static CommandProcessingResult failure(String errorCode, String errorMessage) {
            return new Builder()
                    .success(false)
                    .errorCode(errorCode)
                    .errorMessage(errorMessage)
                    .build();
        }

        public boolean isSuccess() {
            return success;
        }

        public String getErrorCode() {
            return errorCode;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public RuleBinding getRuleBinding() {
            return ruleBinding;
        }

        public ApplicabilityScope getApplicabilityData() {
            return applicabilityData;
        }

        public TimeFrame getTimeframeData() {
            return timeframeData;
        }

        public IdempotencyResultDto toIdempotencyDto() {
            String bindingId = ruleBinding != null ? ruleBinding.getId() : null;
            String ruleId = ruleBinding != null ? ruleBinding.getRuleId() : null;
            return new IdempotencyResultDto(success, bindingId, ruleId);
        }

        static class Builder {
            private boolean success;
            private String errorCode;
            private String errorMessage;
            private RuleBinding ruleBinding;
            private ApplicabilityScope applicabilityData;
            private TimeFrame timeframeData;

            Builder success(boolean success) {
                this.success = success;
                return this;
            }

            Builder errorCode(String errorCode) {
                this.errorCode = errorCode;
                return this;
            }

            Builder errorMessage(String errorMessage) {
                this.errorMessage = errorMessage;
                return this;
            }

            Builder ruleBinding(RuleBinding ruleBinding) {
                this.ruleBinding = ruleBinding;
                return this;
            }

            Builder applicabilityData(ApplicabilityScope applicabilityData) {
                this.applicabilityData = applicabilityData;
                return this;
            }

            Builder timeframeData(TimeFrame timeframeData) {
                this.timeframeData = timeframeData;
                return this;
            }

            CommandProcessingResult build() {
                return new CommandProcessingResult(this);
            }
        }
    }
}
