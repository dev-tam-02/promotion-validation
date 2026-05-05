package vn.viettel.vds.promotion.validation.application.service;

import com.promix.platform.core.exception.BusinessRuleException;
import com.promix.platform.core.util.IdGenerator;
import vn.viettel.vds.promotion.validation.domain.exception.InvalidCommandDataException;
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
import vn.viettel.vds.promotion.validation.application.port.out.OperatorPersistencePort;
import vn.viettel.vds.promotion.validation.application.port.out.RuleBindingPersistencePort;
import vn.viettel.vds.promotion.validation.application.port.out.RuleEngineClient;
import vn.viettel.vds.promotion.validation.application.port.out.RuleHistoryPersistencePort;
import vn.viettel.vds.promotion.validation.application.port.out.ValidationRuleRepositoryPort;
import vn.viettel.vds.promotion.validation.domain.model.RuleHistoryEntry;
import vn.viettel.vds.promotion.validation.domain.model.Operator;
import vn.viettel.vds.promotion.validation.command.SettingValidationRuleCommand;
import vn.viettel.vds.promotion.validation.command.SettingValidationRuleCommand.ApplicabilityScope;
import vn.viettel.vds.promotion.validation.command.SettingValidationRuleCommand.SettingValidationRuleCommandPayload;
import vn.viettel.vds.promotion.validation.command.SettingValidationRuleCommand.TimeFrame;
import vn.viettel.vds.promotion.validation.domain.common.ErrorCode;
import vn.viettel.vds.promotion.validation.domain.common.Result;
import vn.viettel.vds.promotion.validation.domain.model.Rule;
import vn.viettel.vds.promotion.validation.domain.model.RuleBinding;
import vn.viettel.vds.promotion.validation.domain.model.RuleNode;

import vn.viettel.vds.promotion.validation.domain.exception.RuleNotFoundException;

import java.time.Instant;
import java.time.Period;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private final RuleHistoryPersistencePort historyPort;

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
            OperatorPersistencePort operatorPort,
            RuleHistoryPersistencePort historyPort) {
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
        this.historyPort = historyPort;
    }

    /**
     * Handle SettingValidationRuleCommand.
     * <p>
     * Uses TransactionTemplate for explicit transaction control to ensure that:
     * - SUCCESS event is published ONLY AFTER the DB transaction commits
     * - If transaction commit fails, no SUCCESS event is sent (prevents SUCCESS+FAILURE race)
     * - Idempotency is marked BEFORE publishing SUCCESS (protects DLQ handler from race)
     */
    @SuppressWarnings("java:S2139")
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
                String errorCode = result != null ? result.getErrorCode() : "PROCESSING_ERROR";
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
                publishErrorEvent(commandId, campaignId, "PROCESSING_ERROR", "Unexpected error: " + e.getMessage());
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

            // Resolve or auto-create validation rule (2-path: Path A = load, Path B = auto-gen)
            Rule resolvedRule = resolveOrCreateRule(payload);
            if (payload.getRuleId() == null) {
                // Path B: newly built rule — persist and update components with new ruleId
                resolvedRule = validationRulePort.save(resolvedRule);
                logger.info("[Path B] Saved auto-generated rule {} for campaign {}",
                        resolvedRule.getId(), payload.getObjectId());
                // V2: append CREATE audit snapshot to history
                historyPort.save(buildHistorySnapshot(resolvedRule, "Auto-generated by saga (Path B)"));
                components = new ComponentsData(
                        resolvedRule.getId(),
                        components.objectType(),
                        components.objectId(),
                        components.active(),
                        components.trafficPercent(),
                        components.applicableToData(),
                        components.timeframeData(),
                        components.priority()
                );
            }
            // Path A: rule already in DB; ruleId in components matches payload.ruleId

            // Create unified RuleBinding
            RuleBinding ruleBinding = createRuleBinding(components);

            logger.info("[SAGA-DEBUG] processCommand ruleBinding created: commandId={}, bindingId={}", commandId, ruleBinding.getId());
            logger.info("Created rule binding: objectType={}, objectId={}, ruleId={}, bindingId={}",
                    components.objectType(), components.objectId(), components.ruleId(), ruleBinding.getId());

            // Deploy to validation-engine (does NOT save the binding)
            ruleBinding = deployRuleToEngine(ruleBinding, components.ruleId(), components.applicableToData());

            // Save binding once (avoids double-save OptimisticLockException)
            logger.info("[SAGA-DEBUG] BEFORE ruleBindingPort.save: commandId={}, bindingId={}", commandId, ruleBinding.getId());
            ruleBinding = ruleBindingPort.save(ruleBinding);
            logger.info("[SAGA-DEBUG] AFTER ruleBindingPort.save: commandId={}, bindingId={}", commandId, ruleBinding.getId());

            // If a ruleId was present (compile required), verify deployment succeeded
            // Deploy failure is indicated by bundleHash being null after the attempt
            if (components.ruleId() != null && !components.ruleId().isEmpty()
                    && ruleBinding.getBundleHash() == null) {
                logger.warn("Rule binding created but compilation to rule-engine failed: bindingId={}, ruleId={}",
                        ruleBinding.getId(), components.ruleId());
                return CommandProcessingResult.failure("COMPILE_DEPLOY_ERROR",
                        "Rule binding created but compilation to rule-engine failed. RuleId: " + components.ruleId());
            }

            // Step 4 (T4): Register DRL into KieBase (pp-rule-engine) for live evaluation.
            // This is a best-effort step — failures are logged but do not block the saga.
            // The T0 bootstrap loader will re-register rules on next service restart if needed.
            if (resolvedRule.getNodes() != null && !resolvedRule.getNodes().isEmpty()) {
                compileDrlAndRegisterInEngine(resolvedRule);
            }

            // Create result
            logger.info("[SAGA-DEBUG] processCommand EXIT success: commandId={}, bindingId={}", commandId, ruleBinding.getId());
            return CommandProcessingResult.success(ruleBinding, components.applicableToData(), components.timeframeData());

        } catch (Exception e) {
            logger.info("[SAGA-DEBUG] processCommand EXCEPTION: commandId={}, exceptionClass={}, message={}",
                    commandId, e.getClass().getName(), e.getMessage());
            logger.error("Error processing command: commandId={}", commandId, e);
            return CommandProcessingResult.failure("PROCESSING_ERROR", e.getMessage());
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
                .createdBy("system")
                .updatedBy("system")
                .version(0L);

        applyApplicabilityData(builder, components.applicableToData());
        applyTimeframeData(builder, components.timeframeData());

        return builder.build();
    }

    /**
     * Two-path rule resolution:
     *
     * <ul>
     *   <li><b>Path A</b>: {@code payload.ruleId != null} — load the existing rule from DB.
     *       Throws {@link RuleNotFoundException} if the ID does not exist (saga compensation).</li>
     *   <li><b>Path B</b>: {@code payload.ruleId == null} — auto-generate a skeleton rule whose
     *       only condition is a {@code binding.validity_window} temporal gate built from the
     *       payload's timeframe. The caller is responsible for persisting the returned rule.</li>
     * </ul>
     */
    Rule resolveOrCreateRule(SettingValidationRuleCommandPayload payload) {
        if (payload.getRuleId() != null) {
            // ===== Path A: reuse existing rule =====
            Rule existing = validationRulePort.findById(payload.getRuleId())
                    .orElseThrow(() -> new RuleNotFoundException(payload.getRuleId()));
            logger.info("[Path A] Reusing existing rule {} for campaign {}",
                    payload.getRuleId(), payload.getObjectId());
            return existing;
        }

        // ===== Path B: auto-generate timeframe-gate rule =====
        String ruleId = IdGenerator.generateId();
        String objectId = payload.getObjectId();
        String code = "CAMPAIGN_" + objectId.replace("-", "").substring(
                0, Math.min(objectId.replace("-", "").length(), 20));

        List<RuleNode> nodes = buildTimeframeNodes(payload.getTimeframe());

        Instant now = Instant.now();
        Instant effectiveFrom = null;
        Instant effectiveTo = null;
        TimeFrame tf = payload.getTimeframe();
        if (tf != null && tf.getValidityTimeframe() != null) {
            effectiveFrom = tf.getValidityTimeframe().getStartDate();
            effectiveTo = tf.getValidityTimeframe().getExpirationDate();
        }

        Rule rule = Rule.builder()
                .id(ruleId)
                .code(code)
                .name("Campaign Rule - " + objectId)
                .description("Auto-generated validation rule for campaign " + objectId)
                .state(Rule.RuleState.PUBLISHED)
                .active(true)
                .ruleVersion(1L)
                .logic(Rule.LogicType.ALL)
                .nodes(nodes)
                .effectiveFrom(effectiveFrom)
                .effectiveTo(effectiveTo)
                .campaignId(objectId)
                .publishedAt(now)
                .publishedBy("system")
                .createdAt(now)
                .updatedAt(now)
                .createdBy("system")
                .updatedBy("system")
                // BUG-024: leave version null so RuleJpaEntity.isNew() returns true and
                // Spring Data uses persist() (INSERT) instead of merge() (UPDATE).
                // @PrePersist will assign version=0 inside the persistence layer.
                .version(null)
                .build();

        logger.info("[Path B] Auto-generated timeframe rule {} for campaign {} (nodes={})",
                rule.getId(), objectId, nodes.size());
        return rule;
    }

    /**
     * Build a rule-node list containing a single {@code binding.validity_window} COND node
     * wrapped in a root GROUP(ALL), derived from the payload's validity timeframe.
     *
     * <p>Returns an empty list when:
     * <ul>
     *   <li>{@code tf} is null</li>
     *   <li>{@code tf.validityTimeframe} is null</li>
     *   <li>Both {@code startDate} and {@code expirationDate} are null</li>
     * </ul>
     * In those cases the auto-generated DRL becomes an unconditional ALLOW skeleton.
     */
    private List<RuleNode> buildTimeframeNodes(TimeFrame tf) {
        if (tf == null || tf.getValidityTimeframe() == null) {
            return List.of();
        }

        Instant startDate = tf.getValidityTimeframe().getStartDate();
        Instant endDate = tf.getValidityTimeframe().getExpirationDate();
        String timezone = tf.getTimezone() != null ? tf.getTimezone() : "Asia/Ho_Chi_Minh";

        if (startDate == null && endDate == null) {
            return List.of();
        }

        Map<String, Object> params = new HashMap<>();
        if (startDate != null) {
            params.put("startDate", startDate.toString());
        }
        if (endDate != null) {
            params.put("endDate", endDate.toString());
        }
        params.put("timezone", timezone);

        RuleNode condNode = RuleNode.builder()
                .nodeId(IdGenerator.generateId())
                .type(RuleNode.NodeType.COND)
                .operatorName("binding.validity_window")
                .reasonCode("OUTSIDE_VALIDITY_WINDOW")
                .params(params)
                .build();

        RuleNode rootGroup = RuleNode.builder()
                .nodeId(IdGenerator.generateId())
                .type(RuleNode.NodeType.GROUP)
                .groupLogic(Rule.LogicType.ALL)
                .children(List.of(condNode))
                .build();

        return List.of(rootGroup);
    }

    /**
     * Build a CREATE history entry capturing the full rule snapshot at the moment of auto-generation (Path B).
     *
     * <p>The {@code dslSnapshot} stores essential fields so the rule can be reconstructed
     * from history. The snapshot is serialised to JSON by {@link RuleHistoryJpaAdapter}.
     */
    private RuleHistoryEntry buildHistorySnapshot(Rule rule, String changeReason) {
        Map<String, Object> snapshot = buildRuleSnapshot(rule);
        return new RuleHistoryEntry(
                IdGenerator.generateId(),
                rule.getId(),
                rule.getRuleVersion() != null ? rule.getRuleVersion() : 1L,
                RuleHistoryEntry.ChangeType.CREATE,
                "system",
                Instant.now(),
                snapshot,
                rule.getBundleHash(),
                rule.getState() != null ? rule.getState().name() : null,
                changeReason
        );
    }

    /**
     * Compose a Map snapshot of the rule — id, name, objectId, ruleVersion, state, nodes.
     * Children of each node are included recursively.
     */
    private Map<String, Object> buildRuleSnapshot(Rule rule) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("id", rule.getId());
        snapshot.put("code", rule.getCode());
        snapshot.put("name", rule.getName());
        snapshot.put("campaignId", rule.getCampaignId());
        snapshot.put("ruleVersion", rule.getRuleVersion());
        snapshot.put("state", rule.getState() != null ? rule.getState().name() : null);
        snapshot.put("logic", rule.getLogic() != null ? rule.getLogic().name() : null);
        List<RuleNode> nodes = rule.getNodes();
        if (nodes != null) {
            snapshot.put("nodes", nodes.stream()
                    .map(this::nodeToSnapshot)
                    .collect(Collectors.toList()));
        } else {
            snapshot.put("nodes", List.of());
        }
        return snapshot;
    }

    private Map<String, Object> nodeToSnapshot(RuleNode node) {
        Map<String, Object> m = new HashMap<>();
        m.put("nodeId", node.getNodeId());
        m.put("type", node.getType() != null ? node.getType().name() : null);
        m.put("operatorName", node.getOperatorName());
        m.put("reasonCode", node.getReasonCode());
        m.put("groupLogic", node.getGroupLogic() != null ? node.getGroupLogic().name() : null);
        m.put("params", node.getParams());
        List<RuleNode> children = node.getChildren();
        if (children != null && !children.isEmpty()) {
            m.put("children", children.stream()
                    .map(this::nodeToSnapshot)
                    .collect(Collectors.toList()));
        }
        return m;
    }

    /**
     * Auto-create a campaign-specific validation rule when no ruleId is provided.
     * Creates a minimal rule with a root GROUP node (ALL logic) and no business conditions.
     * The temporal validation (date range, days of week, hours per day) is handled by
     * the DRL compiler's temporal policy system based on the binding's temporal data.
     *
     * @deprecated Use {@link #resolveOrCreateRule(SettingValidationRuleCommandPayload)} instead.
     */
    @Deprecated
    private Rule createCampaignValidationRule(ComponentsData components) {
        String ruleId = IdGenerator.generateId();
        String objectId = components.objectId();
        String code = "CAMPAIGN_" + objectId.replace("-", "").substring(0, Math.min(objectId.replace("-", "").length(), 20));

        // No business condition nodes — temporal validation is handled by
        // the DRL compiler's temporal policy based on the binding's temporal data
        Instant now = Instant.now();
        Instant effectiveFrom = null;
        Instant effectiveTo = null;

        TimeFrame timeframe = components.timeframeData();
        if (timeframe != null && timeframe.getValidityTimeframe() != null) {
            effectiveFrom = timeframe.getValidityTimeframe().getStartDate();
            effectiveTo = timeframe.getValidityTimeframe().getExpirationDate();
        }

        return Rule.builder()
                .id(ruleId)
                .code(code)
                .name("Campaign Rule - " + objectId)
                .description("Auto-generated validation rule for campaign " + objectId)
                .state(Rule.RuleState.PUBLISHED)
                .active(true)
                .ruleVersion(1L)
                .logic(Rule.LogicType.ALL)
                .nodes(new ArrayList<>())
                .effectiveFrom(effectiveFrom)
                .effectiveTo(effectiveTo)
                .campaignId(objectId)
                .publishedAt(now)
                .publishedBy("system")
                .createdAt(now)
                .updatedAt(now)
                .createdBy("system")
                .updatedBy("system")
                .version(0L)
                .build();
    }

    private void applyApplicabilityData(RuleBinding.RuleBindingBuilder builder, ApplicabilityScope scope) {
        if (scope == null) {
            return;
        }
        builder.includedAll(Boolean.TRUE.equals(scope.getIncludedAll()));
        if (scope.getIncluded() != null && !scope.getIncluded().isEmpty()) {
            List<String> includedIds = scope.getIncluded().stream()
                    .map(SettingValidationRuleCommand.ApplicabilityRule::getId)
                    .collect(Collectors.toList());
            builder.includedProducts(includedIds);
        }
        if (scope.getExcluded() != null && !scope.getExcluded().isEmpty()) {
            List<String> excludedIds = scope.getExcluded().stream()
                    .map(SettingValidationRuleCommand.ApplicabilityRule::getId)
                    .collect(Collectors.toList());
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
        if (timeframe.getValidityTimeframe() != null) {
            var validity = timeframe.getValidityTimeframe();
            builder.validFrom(validity.getStartDate());
            builder.validTo(validity.getExpirationDate());
            interval = validity.getInterval();
            duration = validity.getDuration();
        }

        List<Integer> daysOfWeek = timeframe.getValidityDaysOfWeek();
        if ((daysOfWeek != null && !daysOfWeek.isEmpty()) || interval != null) {
            builder.rrule(buildRRuleFromTimeframe(daysOfWeek, interval));
        }

        // F2: persist DURATION in scopeTimeWindows (not inside RRULE — RFC 5545 §3.3.10
        // RECUR rule parts do not include DURATION; it is a separate iCal property)
        if (duration != null && !duration.isBlank()) {
            Map<String, Object> stw = new HashMap<>();
            stw.put("duration", duration);
            if (timeframe.getTimezone() != null) {
                stw.put("timezone", timeframe.getTimezone());
            }
            builder.scopeTimeWindows(stw);
        }

        if (timeframe.getValidityHoursPerDay() != null && !timeframe.getValidityHoursPerDay().isEmpty()) {
            List<RuleBinding.TimeWindow> windows = timeframe.getValidityHoursPerDay().stream()
                    .map(hours -> RuleBinding.TimeWindow.builder()
                            .start(extractTimeOnly(hours.getStartTime()))
                            .end(extractTimeOnly(hours.getExpirationTime()))
                            .build())
                    .collect(Collectors.toList());
            builder.timeWindows(windows);
        }
    }

    /**
     * Build RFC 5545 RRULE string from timeframe fields.
     * <p>
     * Format: FREQ=DAILY;INTERVAL=X;BYDAY=MO,TU,...
     * - If interval is null, defaults to FREQ=WEEKLY when BYDAY is specified, else FREQ=DAILY
     * - BYDAY is omitted if daysOfWeek is null or empty
     * - INTERVAL and FREQ are derived from ISO 8601 period (P1D/P1W/P1M/P1Y) via
     *   {@link #parseFreqAndInterval(String)}
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
            return "FREQ=DAILY;INTERVAL=1";
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
            return "FREQ=DAILY;INTERVAL=1";
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
            return "FREQ=DAILY;INTERVAL=1";
        } catch (DateTimeParseException e) {
            logger.warn("Failed to parse ISO 8601 period '{}', defaulting to FREQ=DAILY;INTERVAL=1", isoPeriod);
            return "FREQ=DAILY;INTERVAL=1";
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
     * Deploy rule to validation-engine
     */
    private RuleBinding deployRuleToEngine(RuleBinding binding, String ruleId, ApplicabilityScope applicableToData) {
        try {
            if (!Boolean.TRUE.equals(binding.getActive())) {
                logger.info("Skipping deployment - binding is not active: bindingId={}", binding.getId());
                return binding;
            }

            boolean hasTemporalPolicy = binding.hasTemporalConstraints();
            boolean hasApplicability = applicableToData != null;

            if (ruleId == null || ruleId.isEmpty()) {
                // Deploy assignment bundle without business rule
                if (!hasApplicability && !hasTemporalPolicy) {
                    logger.info("Skipping deployment - no ruleId and no constraints: bindingId={}", binding.getId());
                    return binding;
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
                    return binding;
                }

                var validationRule = validationRuleOpt.get();

                // If rule has no business conditions (e.g., auto-created campaign rule),
                // deploy temporal-only bundle instead of full rule compilation
                if (validationRule.getNodes() == null || validationRule.getNodes().isEmpty()) {
                    if (!hasApplicability && !hasTemporalPolicy) {
                        logger.info("Rule has no conditions and no constraints, skipping deployment: bindingId={}", binding.getId());
                        return binding;
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
            return binding;
        }
    }

    private RuleBinding handlePublishResult(RuleBinding binding, RulePublishingService.RulePublishResult publishResult) {
        if (publishResult.isSuccess()) {
            // Re-fetch the binding from DB to get the managed entity and avoid OptimisticLockException
            RuleBinding current = ruleBindingPort.findById(binding.getId()).orElse(binding);
            RuleBinding updated = current.toBuilder()
                    .bundleHash(publishResult.getBundleHash())
                    .updatedAt(Instant.now())
                    .build();
            logger.info("Rule deployed successfully: bindingId={}, bundleHash={}", updated.getId(), publishResult.getBundleHash());
            return updated;
        } else {
            logger.error("Failed to deploy rule: bindingId={}, error={}", binding.getId(), publishResult.getErrorMessage());
            return binding;
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

    private record ComponentsData(
            String ruleId,
            String objectType,
            String objectId,
            Boolean active,
            Integer trafficPercent,
            ApplicabilityScope applicableToData,
            TimeFrame timeframeData,
            Integer priority
    ) {
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
