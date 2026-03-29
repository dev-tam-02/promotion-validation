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
import vn.viettel.vds.promotion.validation.application.port.out.RuleBindingPersistencePort;
import vn.viettel.vds.promotion.validation.application.port.out.ValidationRuleRepositoryPort;
import vn.viettel.vds.promotion.validation.command.SettingValidationRuleCommand;
import vn.viettel.vds.promotion.validation.command.SettingValidationRuleCommand.ApplicabilityScope;
import vn.viettel.vds.promotion.validation.command.SettingValidationRuleCommand.SettingValidationRuleCommandPayload;
import vn.viettel.vds.promotion.validation.command.SettingValidationRuleCommand.TimeFrame;
import vn.viettel.vds.promotion.validation.domain.common.ErrorCode;
import vn.viettel.vds.promotion.validation.domain.common.Result;
import vn.viettel.vds.promotion.validation.domain.model.Rule;
import vn.viettel.vds.promotion.validation.domain.model.RuleBinding;
import vn.viettel.vds.promotion.validation.domain.model.RuleNode;

import java.time.Instant;
import java.util.ArrayList;
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

    public SettingValidationRuleCommandHandler(
            RuleBindingPersistencePort ruleBindingPort,
            ValidationRuleRepositoryPort validationRulePort,
            SettingValidationRuleEventPublisher eventPublisher,
            IdempotencyService idempotencyService,
            RulePublishingService rulePublishingService,
            Validator validator,
            SettingValidationRuleCommandDTOMapper dtoMapper,
            PlatformTransactionManager transactionManager) {
        this.ruleBindingPort = ruleBindingPort;
        this.validationRulePort = validationRulePort;
        this.eventPublisher = eventPublisher;
        this.idempotencyService = idempotencyService;
        this.rulePublishingService = rulePublishingService;
        this.validator = validator;
        this.dtoMapper = dtoMapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
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

        try {
            logger.info("Processing SettingValidationRuleCommand: commandId={}", commandId);

            // Check idempotency
            if (idempotencyService.isProcessed(commandId)) {
                logger.info("Command already processed (idempotent check): commandId={}", commandId);
                return true;
            }

            // Validate command
            validateCommand(command);

            SettingValidationRuleCommandPayload payload = command.getPayload();
            if (payload == null) {
                logger.error("Command payload is null: commandId={}", commandId);
                publishErrorEvent(commandId, null, "INVALID_PAYLOAD", "Command payload is missing");
                return false;
            }

            String campaignId = payload.getObjectId();

            // Process command within explicit transaction boundary.
            // Transaction commits when execute() returns — BEFORE we publish events.
            CommandProcessingResult result = transactionTemplate.execute(status -> {
                return processCommand(commandId, payload);
            });

            // --- Transaction has committed here ---

            if (result == null || !result.isSuccess()) {
                String errorCode = result != null ? result.getErrorCode() : "PROCESSING_ERROR";
                String errorMessage = result != null ? result.getErrorMessage() : "Processing returned null result";
                publishErrorEvent(commandId, campaignId, errorCode, errorMessage);
                logger.error("Failed to process SettingValidationRuleCommand: commandId={}, errorCode={}, error={}",
                        commandId, errorCode, errorMessage);
                throw new InvalidCommandDataException(errorCode, errorMessage);
            }

            // Mark as processed FIRST — so DLQ handler sees it even if SUCCESS publish is slow
            IdempotencyResultDto idempotencyDto = result.toIdempotencyDto();
            idempotencyService.markAsProcessed(commandId, idempotencyDto);

            // Publish success event AFTER transaction committed and idempotency marked
            publishSuccessEvent(commandId, result);

            logger.info("Successfully processed SettingValidationRuleCommand: commandId={}", commandId);
            return true;

        } catch (BusinessRuleException e) {
            logger.error("Validation failed for SettingValidationRuleCommand: commandId={}, error={}",
                    commandId, e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error processing SettingValidationRuleCommand: commandId={}", commandId, e);
            // Only publish error if this command was NOT already successfully processed.
            // Prevents the race: SUCCESS event published -> post-processing fails -> FAILURE event.
            if (!idempotencyService.isProcessed(commandId)) {
                String campaignId = command.getPayload() != null ? command.getPayload().getObjectId() : null;
                publishErrorEvent(commandId, campaignId, "PROCESSING_ERROR", "Unexpected error: " + e.getMessage());
            } else {
                logger.warn("Command already marked as processed, suppressing error event: commandId={}", commandId);
            }
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
            // Validate components
            Result<ComponentsData> componentsResult = validateCommandComponents(payload);
            if (componentsResult.isFailure()) {
                return CommandProcessingResult.failure(
                        componentsResult.getFirstErrorCode().orElse(ErrorCode.COMMAND_VALIDATION_ERROR).name(),
                        componentsResult.getFirstErrorMessage().orElse("Command validation failed")
                );
            }

            ComponentsData components = componentsResult.getValue();

            // Auto-create campaign validation rule when ruleId is not provided
            if ((components.ruleId() == null || components.ruleId().isEmpty())
                    && "DISCOUNT_COUPON".equals(components.objectType())) {

                Rule campaignRule = createCampaignValidationRule(components);
                campaignRule = validationRulePort.save(campaignRule);

                logger.info("Auto-created campaign validation rule: ruleId={}, objectId={}",
                        campaignRule.getId(), components.objectId());

                components = new ComponentsData(
                        campaignRule.getId(),
                        components.objectType(),
                        components.objectId(),
                        components.active(),
                        components.trafficPercent(),
                        components.applicableToData(),
                        components.timeframeData(),
                        components.priority()
                );
            }

            // Create unified RuleBinding
            RuleBinding ruleBinding = createRuleBinding(components);

            logger.info("Created rule binding: objectType={}, objectId={}, ruleId={}, bindingId={}",
                    components.objectType(), components.objectId(), components.ruleId(), ruleBinding.getId());

            // Deploy to validation-engine (does NOT save the binding)
            ruleBinding = deployRuleToEngine(ruleBinding, components.ruleId(), components.applicableToData());

            // Save binding once (avoids double-save OptimisticLockException)
            ruleBinding = ruleBindingPort.save(ruleBinding);

            // If a ruleId was present (compile required), verify deployment succeeded
            // Deploy failure is indicated by bundleHash being null after the attempt
            if (components.ruleId() != null && !components.ruleId().isEmpty()
                    && ruleBinding.getBundleHash() == null) {
                logger.warn("Rule binding created but compilation to rule-engine failed: bindingId={}, ruleId={}",
                        ruleBinding.getId(), components.ruleId());
                return CommandProcessingResult.failure("COMPILE_DEPLOY_ERROR",
                        "Rule binding created but compilation to rule-engine failed. RuleId: " + components.ruleId());
            }

            // Create result
            return CommandProcessingResult.success(ruleBinding, components.applicableToData(), components.timeframeData());

        } catch (Exception e) {
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
     * Auto-create a campaign-specific validation rule when no ruleId is provided.
     * Creates a minimal rule with a root GROUP node (ALL logic) and no business conditions.
     * The temporal validation (date range, days of week, hours per day) is handled by
     * the DRL compiler's temporal policy system based on the binding's temporal data.
     */
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
        if (timeframe.getValidityTimeframe() != null) {
            var validity = timeframe.getValidityTimeframe();
            builder.validFrom(validity.getStartDate());
            builder.validTo(validity.getExpirationDate());
        }
        if (timeframe.getValidityDaysOfWeek() != null && !timeframe.getValidityDaysOfWeek().isEmpty()) {
            builder.rrule(buildRRuleFromDaysOfWeek(timeframe.getValidityDaysOfWeek()));
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

    private String buildRRuleFromDaysOfWeek(List<Integer> daysOfWeek) {
        String[] dayCodes = {"MO", "TU", "WE", "TH", "FR", "SA", "SU"};
        String byDay = daysOfWeek.stream()
                .filter(day -> day >= 1 && day <= 7)
                .map(day -> dayCodes[day - 1])
                .collect(Collectors.joining(","));
        return "FREQ=WEEKLY;BYDAY=" + byDay;
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
        logger.error("Processing dead letter command: commandId={}", commandId);

        // If already successfully processed, skip failure event to prevent duplicate SUCCESS+FAILURE
        if (idempotencyService.isProcessed(commandId)) {
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
                if (ruleBindingPort.existsByObjectAndRule(objectType, objectId, ruleId != null ? ruleId : "")) {
                    logger.info("Binding exists in DB, command was processed successfully, skipping DLQ failure event: commandId={}", commandId);
                    return;
                }
            } catch (Exception e) {
                logger.warn("Failed to check DB for binding existence, proceeding with DLQ event: commandId={}", commandId, e);
            }
        }

        try {
            eventPublisher.publishDeadLetterEvent(commandId);
        } catch (Exception e) {
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
