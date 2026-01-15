package vn.viettel.vds.promotion.validation.application.service;

import com.promix.platform.core.error.ErrorDetail;
import com.promix.platform.core.exception.BusinessException;
import com.promix.platform.core.exception.factory.ExceptionFactory;
import com.promix.platform.core.util.IdGenerator;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.viettel.vds.promotion.validation.adapter.in.messaging.dto.UpdateValidationRuleCommandDTO;
import vn.viettel.vds.promotion.validation.adapter.in.messaging.mapper.UpdateValidationRuleCommandDTOMapper;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.AssignmentSnapshotEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.ValidationRuleSnapshotEntity;
import vn.viettel.vds.promotion.validation.application.port.out.*;
import vn.viettel.vds.promotion.validation.command.UpdateValidationRuleCommand;
import vn.viettel.vds.promotion.validation.command.UpdateValidationRuleCommand.ApplicabilityScope;
import vn.viettel.vds.promotion.validation.command.UpdateValidationRuleCommand.TimeFrame;
import vn.viettel.vds.promotion.validation.command.UpdateValidationRuleCommand.UpdateValidationRuleCommandPayload;
import vn.viettel.vds.promotion.validation.domain.common.ErrorCode;
import vn.viettel.vds.promotion.validation.domain.exception.TimeframeProcessingException;
import vn.viettel.vds.promotion.validation.domain.model.*;
import vn.viettel.vds.promotion.validation.event.ValidationSettingUpdateResultEvent;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service to handle UpdateValidationRuleCommand processing.
 * Updates existing validation rule assignments with new configuration.
 */
@Service
@Transactional
public class UpdateValidationRuleCommandHandler {

    private static final Logger logger = LoggerFactory.getLogger(UpdateValidationRuleCommandHandler.class);
    private static final String SOURCE = "validation-service";

    private final AssignmentPersistencePort assignmentPort;
    private final ApplicabilityRulePersistencePort applicabilityRulePort;
    private final RuleTimeFramePersistencePort ruleTimeFramePort;
    private final TemporalPolicyPersistencePort temporalPolicyPort;
    private final RuleTemporalLinkPersistencePort ruleTemporalLinkPort;
    private final ValidationRuleRepositoryPort validationRulePort;
    private final IdempotencyService idempotencyService;
    private final RulePublishingService rulePublishingService;
    private final Validator validator;
    private final UpdateValidationRuleCommandDTOMapper dtoMapper;
    private final AssignmentSnapshotService assignmentSnapshotService;
    private final ValidationRuleSnapshotService validationRuleSnapshotService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.validation-event:promotion_validation_event}")
    private String validationEventTopic;

    public UpdateValidationRuleCommandHandler(
            AssignmentPersistencePort assignmentPort,
            ApplicabilityRulePersistencePort applicabilityRulePort,
            RuleTimeFramePersistencePort ruleTimeFramePort,
            TemporalPolicyPersistencePort temporalPolicyPort,
            RuleTemporalLinkPersistencePort ruleTemporalLinkPort,
            ValidationRuleRepositoryPort validationRulePort,
            IdempotencyService idempotencyService,
            RulePublishingService rulePublishingService,
            Validator validator,
            UpdateValidationRuleCommandDTOMapper dtoMapper,
            AssignmentSnapshotService assignmentSnapshotService,
            ValidationRuleSnapshotService validationRuleSnapshotService,
            KafkaTemplate<String, Object> kafkaTemplate) {
        this.assignmentPort = assignmentPort;
        this.applicabilityRulePort = applicabilityRulePort;
        this.ruleTimeFramePort = ruleTimeFramePort;
        this.temporalPolicyPort = temporalPolicyPort;
        this.ruleTemporalLinkPort = ruleTemporalLinkPort;
        this.validationRulePort = validationRulePort;
        this.idempotencyService = idempotencyService;
        this.rulePublishingService = rulePublishingService;
        this.validator = validator;
        this.dtoMapper = dtoMapper;
        this.assignmentSnapshotService = assignmentSnapshotService;
        this.validationRuleSnapshotService = validationRuleSnapshotService;
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Handle UpdateValidationRuleCommand.
     * Returns true if successfully processed, false if already processed (idempotent).
     * Throws exception on validation or processing errors.
     */
    @SuppressWarnings("java:S2139")
    public boolean handleCommand(UpdateValidationRuleCommand command) {
        String commandId = command.getId();

        try {
            logger.info("Processing UpdateValidationRuleCommand: commandId={}", commandId);

            // Check idempotency - return false to indicate already processed
            if (idempotencyService.isProcessed(commandId)) {
                logger.info("Command already processed (idempotent check): commandId={}", commandId);
                return false;
            }

            // Step 1: Validate command
            validateCommand(command);

            // Extract payload
            UpdateValidationRuleCommandPayload payload = command.getPayload();
            if (payload == null) {
                logger.error("Command payload is null: commandId={}", commandId);
                throw ExceptionFactory.createValidationException("INVALID_PAYLOAD", "Command payload is missing");
            }

            // Step 2: Find existing assignment
            // Priority: assignmentId > (objectType + objectId)
            Assignment assignment;
            String assignmentId;
            String payloadAssignmentId = payload.getAssignmentId();

            if (payloadAssignmentId != null && !payloadAssignmentId.isBlank()) {
                // Find by assignmentId
                assignment = assignmentPort.findById(payloadAssignmentId)
                        .orElseThrow(() -> {
                            logger.error("Assignment not found: assignmentId={}", payloadAssignmentId);
                            return ExceptionFactory.createValidationException(
                                    ErrorCode.ASSIGNMENT_VALIDATION_NOT_FOUND.name(),
                                    "Assignment not found: " + payloadAssignmentId
                            );
                        });
                assignmentId = payloadAssignmentId;
            } else {
                // Find by objectType and objectId
                String objectType = payload.getObjectType();
                String objectId = payload.getObjectId();

                List<Assignment> assignments = assignmentPort.findAllBySubjectTypeAndSubjectKey(objectType, objectId);
                if (assignments.isEmpty()) {
                    logger.error("Assignment not found: objectType={}, objectId={}", objectType, objectId);
                    throw ExceptionFactory.createValidationException(
                            ErrorCode.ASSIGNMENT_VALIDATION_NOT_FOUND.name(),
                            "Assignment not found: objectType=" + objectType + ", objectId=" + objectId
                    );
                }
                // Take the first one (latest by default query ordering)
                assignment = assignments.get(0);
                assignmentId = assignment.getId();
                logger.info("Found assignment by objectReference: objectType={}, objectId={}, assignmentId={}",
                        objectType, objectId, assignmentId);
            }

            // Step 3: Create snapshots BEFORE update for compensation
            String sagaId = command.getMetadata() != null ? command.getMetadata().get("sagaId") : null;

            // 3a. Create Assignment snapshot
            AssignmentSnapshotEntity assignmentSnapshot = assignmentSnapshotService.createSnapshot(
                    assignmentId, sagaId, commandId, AssignmentSnapshotEntity.SnapshotReason.BEFORE_UPDATE);

            // 3b. Create ValidationRule snapshot if ruleId exists (for RevertValidationRuleCommand support)
            String existingRuleId = assignment.getRuleId();
            Long ruleVersionBeforeUpdate = null;
            if (existingRuleId != null && !existingRuleId.isEmpty()) {
                var ruleOpt = validationRulePort.findById(existingRuleId);
                if (ruleOpt.isPresent()) {
                    Rule rule = ruleOpt.get();
                    ruleVersionBeforeUpdate = rule.getRuleVersion() != null ? rule.getRuleVersion().longValue() : null;
                    validationRuleSnapshotService.createSnapshot(
                            existingRuleId, sagaId, commandId,
                            ValidationRuleSnapshotEntity.SnapshotReason.BEFORE_UPDATE);
                    logger.info("Created ValidationRule snapshot: ruleId={}, version={}",
                            existingRuleId, ruleVersionBeforeUpdate);
                }
            }

            // Calculate versions for event publishing
            // snapshotVersion = version of the snapshot we just created (state BEFORE update)
            // This is the version to revert to if saga compensation is needed
            Long snapshotVersionToRevertTo = assignmentSnapshot.getSnapshotVersion();

            // Step 4: Process update
            UpdateProcessingResult result = processUpdate(commandId, assignment, payload);

            if (!result.isSuccess()) {
                logger.error("Failed to process UpdateValidationRuleCommand: commandId={}, errorCode={}, error={}",
                        commandId, result.getErrorCode(), result.getErrorMessage());
                throw ExceptionFactory.createValidationException(result.getErrorCode(), result.getErrorMessage());
            }

            // Mark as processed
            idempotencyService.markAsProcessed(commandId, result.toIdempotencyDto());

            // Step 5: Publish success event with version info
            // currentVersion = snapshot version after update (next version = snapshotVersionToRevertTo + 1)
            // previousVersion = snapshot version to revert to (the snapshot we just created)
            Long currentVersion = snapshotVersionToRevertTo + 1;
            publishUpdateSuccessEvent(command, assignmentId, result.getRuleId(),
                    currentVersion, snapshotVersionToRevertTo);

            logger.info("Successfully processed UpdateValidationRuleCommand: commandId={}, assignmentId={}, " +
                            "currentVersion={}, snapshotVersionToRevertTo={}, ruleVersionBeforeUpdate={}",
                    commandId, assignmentId, currentVersion, snapshotVersionToRevertTo, ruleVersionBeforeUpdate);
            return true;

        } catch (BusinessException e) {
            logger.error("Validation failed for UpdateValidationRuleCommand: commandId={}, error={}",
                    commandId, e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error processing UpdateValidationRuleCommand: commandId={}", commandId, e);
            throw ExceptionFactory.createValidationException("PROCESSING_ERROR", "Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Validate command using Bean Validation
     */
    private void validateCommand(UpdateValidationRuleCommand command) {
        if (command == null || command.getPayload() == null) {
            logger.error("Received null command or null payload");
            throw ExceptionFactory.createValidationException("INVALID_COMMAND", "Command or payload is null");
        }

        UpdateValidationRuleCommandDTO dto = dtoMapper.toDTO(command);
        if (dto == null) {
            logger.error("Failed to convert command to DTO: commandId={}", command.getId());
            throw ExceptionFactory.createValidationException("INVALID_COMMAND", "Failed to convert command to DTO");
        }

        Set<ConstraintViolation<UpdateValidationRuleCommandDTO>> violations = validator.validate(dto);

        if (!violations.isEmpty()) {
            List<ErrorDetail> errorDetails = violations.stream()
                    .map(violation -> ErrorDetail.of(
                            violation.getPropertyPath().toString(),
                            violation.getMessage(),
                            String.format("Invalid value: %s", violation.getInvalidValue()),
                            violation.getInvalidValue()
                    ))
                    .toList();

            String errorMessage = String.format("Validation failed with %d error(s)", violations.size());

            logger.error("UpdateValidationRuleCommand validation failed: commandId={}, errorCount={}, errors={}",
                    command.getId(), violations.size(), errorDetails);

            throw ExceptionFactory.createValidationException(
                    "METHOD_ARGUMENT_NOT_VALID",
                    errorMessage,
                    errorDetails.toArray(new ErrorDetail[0])
            );
        }

        logger.debug("UpdateValidationRuleCommand validation passed: commandId={}", command.getId());
    }

    /**
     * Process the update command
     */
    private UpdateProcessingResult processUpdate(
            String commandId,
            Assignment assignment,
            UpdateValidationRuleCommandPayload payload) {
        try {
            String assignmentId = assignment.getId();
            logger.info("Processing update for assignmentId={}", assignmentId);

            // Update basic fields if provided
            if (payload.getActive() != null) {
                assignment.setActive(payload.getActive());
            }

            // Update ruleId with validation
            UpdateProcessingResult ruleIdResult = updateRuleIdIfProvided(assignment, payload);
            if (ruleIdResult != null) {
                return ruleIdResult;
            }

            if (payload.getObjectType() != null && assignment.getSubject() != null) {
                assignment.getSubject().setType(payload.getObjectType());
            }

            // Update objectId with duplicate check
            UpdateProcessingResult objectIdResult = updateObjectIdIfProvided(assignment, payload);
            if (objectIdResult != null) {
                return objectIdResult;
            }

            // Update includedAll from applicability
            updateIncludedAllIfApplicable(assignment, payload);

            assignment.setUpdatedAt(Instant.now());

            // Save assignment changes
            assignment = assignmentPort.save(assignment);

            // Update applicability rules if provided
            ApplicabilityScope applicableTo = payload.getApplicableTo();
            if (applicableTo != null) {
                updateApplicabilityRules(assignment, applicableTo);
            }

            // Update timeframe if provided and re-deploy rule if needed
            boolean hasTemporalPolicyChanges = updateTimeframeIfProvided(assignment, payload);
            assignment = redeployRuleIfNeeded(assignment, payload, hasTemporalPolicyChanges, applicableTo);

            logger.info("Successfully updated assignmentId={}, updatedBy={}, reason={}",
                    assignmentId, payload.getUpdatedBy(), payload.getReason());

            return UpdateProcessingResult.success(assignmentId, assignment.getRuleId());

        } catch (Exception e) {
            logger.error("Error processing update: commandId={}", commandId, e);
            return UpdateProcessingResult.failure("PROCESSING_ERROR", e.getMessage());
        }
    }

    /**
     * Update ruleId if provided, with validation that rule exists.
     * Returns failure result if validation fails, null if successful or not provided.
     */
    private UpdateProcessingResult updateRuleIdIfProvided(
            Assignment assignment,
            UpdateValidationRuleCommandPayload payload) {
        if (payload.getRuleId() == null) {
            return null;
        }
        if (!validationRulePort.existsById(payload.getRuleId())) {
            return UpdateProcessingResult.failure(
                    ErrorCode.VALIDATION_RULE_NOT_FOUND.name(),
                    "Validation rule not found: " + payload.getRuleId()
            );
        }
        assignment.setRuleId(payload.getRuleId());
        return null;
    }

    /**
     * Update objectId if provided, with duplicate assignment check.
     * Returns failure result if duplicate exists, null if successful or not provided.
     */
    private UpdateProcessingResult updateObjectIdIfProvided(
            Assignment assignment,
            UpdateValidationRuleCommandPayload payload) {
        if (payload.getObjectId() == null) {
            return null;
        }
        String currentEntityId = assignment.getSubject() != null ? assignment.getSubject().getKey() : null;
        String newObjectId = payload.getObjectId();
        if (!newObjectId.equals(currentEntityId)) {
            String objectType = payload.getObjectType() != null
                    ? payload.getObjectType()
                    : (assignment.getSubject() != null ? assignment.getSubject().getType() : null);
            if (assignmentPort.existsBySubjectTypeAndSubjectKey(objectType, newObjectId)) {
                return UpdateProcessingResult.failure(
                        ErrorCode.DUPLICATE_ASSIGNMENT_VALIDATION_RULE.name(),
                        "Assignment already exists for objectType=" + objectType + ", objectId=" + newObjectId
                );
            }
        }
        if (assignment.getSubject() != null) {
            assignment.getSubject().setKey(payload.getObjectId());
        }
        return null;
    }

    /**
     * Update includedAll flag if applicableTo specifies includedAll = true.
     */
    private void updateIncludedAllIfApplicable(
            Assignment assignment,
            UpdateValidationRuleCommandPayload payload) {
        if (payload.getApplicableTo() != null && Boolean.TRUE.equals(payload.getApplicableTo().getIncludedAll())) {
            assignment.setIncludedAll(true);
        }
    }

    /**
     * Update timeframe if provided. Returns true if temporal policy was changed.
     */
    private boolean updateTimeframeIfProvided(
            Assignment assignment,
            UpdateValidationRuleCommandPayload payload) {
        TimeFrame timeframe = payload.getTimeframe();
        if (timeframe == null) {
            return false;
        }
        updateTimeframe(assignment, timeframe);
        return true;
    }

    /**
     * Re-deploy rule to engine if needed (temporal policy changes or activation).
     * Returns updated assignment.
     */
    private Assignment redeployRuleIfNeeded(
            Assignment assignment,
            UpdateValidationRuleCommandPayload payload,
            boolean hasTemporalPolicyChanges,
            ApplicabilityScope applicableTo) {
        if (hasTemporalPolicyChanges || Boolean.TRUE.equals(payload.getActive())) {
            return deployRuleToEngine(assignment, assignment.getRuleId(), hasTemporalPolicyChanges, applicableTo);
        }
        return assignment;
    }

    /**
     * Update applicability rules - delete old and create new
     */
    private void updateApplicabilityRules(Assignment assignment, ApplicabilityScope applicabilityScope) {
        String assignmentId = assignment.getId();
        logger.info("Updating applicability rules for assignmentId={}", assignmentId);

        // Delete existing applicability rules
        applicabilityRulePort.deleteByAssignmentId(assignmentId);
        logger.debug("Deleted existing applicability rules for assignmentId={}", assignmentId);

        int savedCount = 0;

        // Create included rules
        if (applicabilityScope.getIncluded() != null && !applicabilityScope.getIncluded().isEmpty()) {
            for (var rule : applicabilityScope.getIncluded()) {
                ApplicabilityRule domainRule = createApplicabilityRuleDomain(
                        assignmentId,
                        ApplicabilityRule.RuleType.INCLUDED,
                        rule
                );
                applicabilityRulePort.save(domainRule);
                savedCount++;
            }
        }

        // Create excluded rules
        if (applicabilityScope.getExcluded() != null && !applicabilityScope.getExcluded().isEmpty()) {
            for (var rule : applicabilityScope.getExcluded()) {
                ApplicabilityRule domainRule = createApplicabilityRuleDomain(
                        assignmentId,
                        ApplicabilityRule.RuleType.EXCLUDED,
                        rule
                );
                applicabilityRulePort.save(domainRule);
                savedCount++;
            }
        }

        logger.info("Saved {} applicability rules for assignmentId={}", savedCount, assignmentId);
    }

    /**
     * Create ApplicabilityRule domain model from command data
     */
    private ApplicabilityRule createApplicabilityRuleDomain(
            String assignmentId,
            ApplicabilityRule.RuleType ruleType,
            UpdateValidationRuleCommand.ApplicabilityRule rule) {

        ApplicabilityRule.ApplicabilityRuleBuilder builder = ApplicabilityRule.builder()
                .id(IdGenerator.generateId())
                .assignmentId(assignmentId)
                .ruleType(ruleType)
                .objectId(rule.getId())
                .skipInitially(rule.getSkipInitially() != null ? rule.getSkipInitially() : 0)
                .repeatCount(rule.getRepeat() != null ? rule.getRepeat() : 1)
                .createdAt(Instant.now())
                .updatedAt(Instant.now());

        if (rule.getObject() != null) {
            builder.objectType(ApplicabilityRule.ObjectType.valueOf(rule.getObject().name()));
        }

        if (rule.getEffect() != null) {
            builder.effect(ApplicabilityRule.EffectType.valueOf(rule.getEffect().name()));
        }

        if (rule.getTarget() != null) {
            builder.target(ApplicabilityRule.TargetType.valueOf(rule.getTarget().name()));
        }

        return builder.build();
    }

    /**
     * Update timeframe - delete old temporal links/policies and create new
     */
    @SuppressWarnings("java:S2139")
    private void updateTimeframe(Assignment assignment, TimeFrame timeframeData) {
        try {
            String assignmentId = assignment.getId();
            logger.info("Updating timeframe for assignmentId={}", assignmentId);

            // Delete existing temporal links and policies for this assignment
            List<RuleTemporalLink> existingLinks = ruleTemporalLinkPort.findByAssignmentId(assignmentId);
            for (RuleTemporalLink link : existingLinks) {
                String policyId = link.getTemporalPolicyId();
                ruleTemporalLinkPort.deleteById(link.getId());
                // Delete temporal policy if no other links reference it
                if (ruleTemporalLinkPort.findByPolicyId(policyId).isEmpty()) {
                    temporalPolicyPort.deleteById(policyId);
                }
            }
            logger.debug("Deleted {} existing temporal links for assignmentId={}", existingLinks.size(), assignmentId);

            // Create new temporal policy
            String timeFrameId = timeframeData.getTimeFrameId();
            String mode = timeframeData.getMode() != null ? timeframeData.getMode().toString() : "REQUIRED";
            String timezone = timeframeData.getTimezone() != null ? timeframeData.getTimezone() : "UTC";

            if (timeFrameId == null) {
                timeFrameId = IdGenerator.generateId();
            }

            // Create TemporalPolicy domain model
            TemporalPolicy temporalPolicy = createTemporalPolicy(timeFrameId, timezone, timeframeData);
            temporalPolicy = temporalPolicyPort.save(temporalPolicy);

            // Create RuleTemporalLink
            RuleTemporalLink link = RuleTemporalLink.builder()
                    .id(IdGenerator.generateId())
                    .assignmentId(assignmentId)
                    .temporalPolicyId(temporalPolicy.getId())
                    .mode(mode)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            ruleTemporalLinkPort.save(link);

            // Create RuleTimeFrame for backward compatibility (only if ruleId exists)
            String ruleId = assignment.getRuleId();
            if (ruleId != null && !ruleId.isEmpty()) {
                var validationRuleOpt = validationRulePort.findById(ruleId);
                if (validationRuleOpt.isPresent()) {
                    RuleTimeFrame ruleTimeFrame = RuleTimeFrame.builder()
                            .id(IdGenerator.generateId())
                            .validationRuleId(ruleId)
                            .timeFrameId(timeFrameId)
                            .mode(mode)
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build();
                    ruleTimeFramePort.save(ruleTimeFrame);
                    logger.debug("Created legacy RuleTimeFrame: ruleId={}, timeFrameId={}", ruleId, timeFrameId);
                } else {
                    logger.debug("Skipping legacy RuleTimeFrame - rule not found: ruleId={}", ruleId);
                }
            } else {
                logger.debug("Skipping legacy RuleTimeFrame - ruleId is null/empty for assignmentId={}", assignmentId);
            }

            logger.info("Successfully updated timeframe: assignmentId={}, timeFrameId={}, policyId={}",
                    assignmentId, timeFrameId, temporalPolicy.getId());

        } catch (Exception e) {
            logger.error("Failed to update timeframe for assignmentId={}: {}",
                    assignment.getId(), e.getMessage(), e);
            throw new TimeframeProcessingException(
                    "Failed to update timeframe for assignmentId=" + assignment.getId() + ": " + e.getMessage(), e);
        }
    }

    /**
     * Create TemporalPolicy domain model from TimeFrame data
     */
    private TemporalPolicy createTemporalPolicy(String timeFrameId, String timezone, TimeFrame timeframeData) {
        TemporalPolicy.TemporalPolicyBuilder builder = TemporalPolicy.builder()
                .id(IdGenerator.generateId())
                .name("timeframe-" + timeFrameId)
                .tz(timezone)
                .createdAt(Instant.now())
                .updatedAt(Instant.now());

        // Process validity timeframe
        if (timeframeData.getValidityTimeframe() != null) {
            UpdateValidationRuleCommand.ValidityTimeframe validity = timeframeData.getValidityTimeframe();
            Instant startTs = calculateStartTs(validity);
            builder.startTs(startTs)
                    .endTs(validity.getExpirationDate())
                    .metadata(buildValidityMetadata(validity));
        }

        // Process validity days of week
        if (timeframeData.getValidityDaysOfWeek() != null && !timeframeData.getValidityDaysOfWeek().isEmpty()) {
            String rrule = buildRRuleFromDaysOfWeek(timeframeData.getValidityDaysOfWeek());
            builder.rrule(rrule);
        }

        // Process validity hours per day as TimeOfDayWindows
        if (timeframeData.getValidityHoursPerDay() != null && !timeframeData.getValidityHoursPerDay().isEmpty()) {
            List<TimeOfDayWindow> windows = createTimeOfDayWindows(timeframeData.getValidityHoursPerDay());
            builder.timeOfDayWindows(windows);
        }

        return builder.build();
    }

    /**
     * Calculate start timestamp from validity data
     */
    private Instant calculateStartTs(UpdateValidationRuleCommand.ValidityTimeframe validity) {
        Instant startTs = validity.getStartDate();
        boolean hasDurationAndInterval = validity.getDuration() != null && validity.getInterval() != null;
        if (startTs == null && hasDurationAndInterval) {
            startTs = Instant.now();
            logger.info("Duration/Interval mode: startDate not provided, using current time as startTs={}", startTs);
        }
        return startTs;
    }

    /**
     * Build metadata map from validity timeframe
     */
    private Map<String, Object> buildValidityMetadata(UpdateValidationRuleCommand.ValidityTimeframe validity) {
        Map<String, Object> metadata = new HashMap<>();
        addIfNotNull(metadata, "interval", validity.getInterval());
        addIfNotNull(metadata, "duration", validity.getDuration());
        addIfNotNull(metadata, "activityDurationAfterPublishing", validity.getActivityDurationAfterPublishing());
        return metadata.isEmpty() ? null : metadata;
    }

    /**
     * Add value to metadata map if not null
     */
    private void addIfNotNull(Map<String, Object> metadata, String key, Object value) {
        if (value != null) {
            metadata.put(key, value);
        }
    }

    /**
     * Build RRULE string from validityDaysOfWeek
     */
    private String buildRRuleFromDaysOfWeek(List<Integer> daysOfWeek) {
        String[] dayCodes = {"MO", "TU", "WE", "TH", "FR", "SA", "SU"};

        String byDay = daysOfWeek.stream()
                .filter(day -> day >= 1 && day <= 7)
                .map(day -> dayCodes[day - 1])
                .collect(Collectors.joining(","));

        return "FREQ=WEEKLY;BYDAY=" + byDay;
    }

    /**
     * Create TimeOfDayWindow domain models for validity hours per day
     */
    private List<TimeOfDayWindow> createTimeOfDayWindows(
            List<UpdateValidationRuleCommand.ValidityHoursPerDay> validityHours) {
        return validityHours.stream()
                .map(hours -> new TimeOfDayWindow(
                        extractTimeOnly(hours.getStartTime()),
                        extractTimeOnly(hours.getExpirationTime())
                ))
                .collect(Collectors.toList());
    }

    /**
     * Extract time portion from time string
     */
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
     * Returns updated assignment with temporal_bundle_hash if deployed successfully.
     */
    private Assignment deployRuleToEngine(Assignment assignment,
                                          String ruleId,
                                          boolean hasTemporalPolicy,
                                          ApplicabilityScope applicableToData) {
        try {
            if (assignment.getActive() == null || !assignment.getActive()) {
                logger.info("Skipping rule deployment - assignment is not active: ruleId={}, assignmentId={}",
                        ruleId, assignment.getId());
                return assignment;
            }

            if (ruleId == null || ruleId.isEmpty()) {
                logger.info("Skipping rule deployment - ruleId is null/empty: assignmentId={}", assignment.getId());
                return assignment;
            }

            var validationRuleOpt = validationRulePort.findById(ruleId);
            if (validationRuleOpt.isEmpty()) {
                logger.warn("Validation rule not found for deployment: ruleId={}", ruleId);
                return assignment;
            }

            var validationRule = validationRuleOpt.get();
            boolean ruleNotYetDeployed = (validationRule.getBundleHash() == null || validationRule.getBundleHash().isEmpty());
            boolean needsDeployment = ruleNotYetDeployed || hasTemporalPolicy;

            if (needsDeployment) {
                String deploymentReason = ruleNotYetDeployed ? "rule not yet deployed" : "temporal policy updated";
                logger.info("Deploying rule after update: ruleId={}, assignmentId={}, reason={}",
                        ruleId, assignment.getId(), deploymentReason);

                // Convert to SettingValidationRuleCommand.ApplicabilityScope for compatibility
                vn.viettel.vds.promotion.validation.command.SettingValidationRuleCommand.ApplicabilityScope settingApplicableTo = null;
                if (applicableToData != null) {
                    settingApplicableTo = convertToSettingApplicabilityScope(applicableToData);
                }

                var publishResult = rulePublishingService.publishRule(ruleId, assignment.getId(), settingApplicableTo);

                if (publishResult.isSuccess()) {
                    // Update temporalBundleHash using toBuilder pattern
                    Assignment updatedAssignment = assignment.toBuilder()
                            .temporalBundleHash(publishResult.getBundleHash())
                            .build();
                    // Save updated assignment via port
                    updatedAssignment = assignmentPort.save(updatedAssignment);
                    logger.info("Rule deployed successfully after update: ruleId={}, bundleHash={}",
                            ruleId, publishResult.getBundleHash());
                    return updatedAssignment;
                } else {
                    logger.error("Failed to deploy rule after update: ruleId={}, error={}",
                            ruleId, publishResult.getErrorMessage());
                }
            }

            return assignment;

        } catch (Exception e) {
            logger.error("Error deploying rule to validation-engine: ruleId={}, assignmentId={}",
                    ruleId, assignment.getId(), e);
            return assignment;
        }
    }

    /**
     * Convert UpdateValidationRuleCommand.ApplicabilityScope to SettingValidationRuleCommand.ApplicabilityScope
     */
    private vn.viettel.vds.promotion.validation.command.SettingValidationRuleCommand.ApplicabilityScope
    convertToSettingApplicabilityScope(ApplicabilityScope source) {
        if (source == null) {
            return null;
        }

        var result = vn.viettel.vds.promotion.validation.command.SettingValidationRuleCommand.ApplicabilityScope.builder()
                .includedAll(source.getIncludedAll())
                .build();

        if (source.getIncluded() != null) {
            result.setIncluded(source.getIncluded().stream()
                    .map(this::convertToSettingApplicabilityRule)
                    .toList());
        }

        if (source.getExcluded() != null) {
            result.setExcluded(source.getExcluded().stream()
                    .map(this::convertToSettingApplicabilityRule)
                    .toList());
        }

        return result;
    }

    /**
     * Convert UpdateValidationRuleCommand.ApplicabilityRule to SettingValidationRuleCommand.ApplicabilityRule
     */
    private vn.viettel.vds.promotion.validation.command.SettingValidationRuleCommand.ApplicabilityRule
    convertToSettingApplicabilityRule(UpdateValidationRuleCommand.ApplicabilityRule source) {
        if (source == null) {
            return null;
        }

        return vn.viettel.vds.promotion.validation.command.SettingValidationRuleCommand.ApplicabilityRule.builder()
                .object(source.getObject() != null ?
                        vn.viettel.vds.promotion.validation.command.SettingValidationRuleCommand.ObjectType.valueOf(source.getObject().name()) : null)
                .id(source.getId())
                .effect(source.getEffect() != null ?
                        vn.viettel.vds.promotion.validation.command.SettingValidationRuleCommand.EffectType.valueOf(source.getEffect().name()) : null)
                .target(source.getTarget() != null ?
                        vn.viettel.vds.promotion.validation.command.SettingValidationRuleCommand.TargetType.valueOf(source.getTarget().name()) : null)
                .skipInitially(source.getSkipInitially())
                .repeat(source.getRepeat())
                .build();
    }

    /**
     * Handle dead letter commands
     */
    public void handleDeadLetterCommand(UpdateValidationRuleCommand command) {
        String commandId = command.getId();
        logger.error("Processing dead letter command: commandId={}", commandId);

        try {
            logger.error("Dead letter command details: commandId={}, type={}, source={}, assignmentId={}",
                    command.getId(),
                    command.getType(),
                    command.getSource(),
                    command.getPayload() != null ? command.getPayload().getAssignmentId() : null);
        } catch (Exception e) {
            logger.error("Failed to process dead letter command: commandId={}", commandId, e);
        }
    }

    /**
     * Publish success event with version info for saga correlation.
     */
    private void publishUpdateSuccessEvent(UpdateValidationRuleCommand command, String assignmentId,
                                           String ruleId, Long currentVersion, Long previousVersion) {
        try {
            String eventId = IdGenerator.generateId();
            UpdateValidationRuleCommandPayload commandPayload = command.getPayload();

            Map<String, String> metadata = new HashMap<>();
            if (command.getMetadata() != null) {
                metadata.putAll(command.getMetadata());
            }
            metadata.put("commandId", command.getId());
            metadata.put("source", SOURCE);

            // Build update result with version info
            ValidationSettingUpdateResultEvent.UpdateResult updateResult =
                    ValidationSettingUpdateResultEvent.UpdateResult.builder()
                            .assignmentId(assignmentId)
                            .ruleId(ruleId)
                            .currentVersion(currentVersion)
                            .previousVersion(previousVersion)
                            .build();

            // Build payload
            ValidationSettingUpdateResultEvent.ValidationSettingUpdatePayload eventPayload =
                    ValidationSettingUpdateResultEvent.ValidationSettingUpdatePayload.builder()
                            .commandId(command.getId())
                            .isSuccess(true)
                            .updateResult(updateResult)
                            .processedBy(SOURCE)
                            .processedAt(Instant.now())
                            .build();

            ValidationSettingUpdateResultEvent event = ValidationSettingUpdateResultEvent.builder()
                    .id(eventId)
                    .type("ValidationSettingUpdateResultEvent")
                    .source(SOURCE)
                    .subject(commandPayload != null ? commandPayload.getObjectId() : assignmentId)
                    .occurredAt(Instant.now())
                    .version(1)
                    .payload(eventPayload)
                    .assignmentId(assignmentId)
                    .ruleId(ruleId)
                    .metadata(metadata)
                    .build();

            String key = commandPayload != null ? commandPayload.getObjectId() : assignmentId;
            kafkaTemplate.send(validationEventTopic, key, event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            logger.error("Failed to publish ValidationSettingUpdateResultEvent: assignmentId={}",
                                    assignmentId, ex);
                        } else {
                            logger.info("Published ValidationSettingUpdateResultEvent: eventId={}, " +
                                            "assignmentId={}, currentVersion={}, previousVersion={}",
                                    eventId, assignmentId, currentVersion, previousVersion);
                        }
                    });

        } catch (Exception e) {
            logger.error("Error publishing ValidationSettingUpdateResultEvent: assignmentId={}", assignmentId, e);
        }
    }

    /**
     * Result wrapper for update processing
     */
    public static class UpdateProcessingResult {
        private final boolean success;
        private final String errorCode;
        private final String errorMessage;
        private final String assignmentId;
        private final String ruleId;

        private UpdateProcessingResult(boolean success, String errorCode, String errorMessage,
                                       String assignmentId, String ruleId) {
            this.success = success;
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
            this.assignmentId = assignmentId;
            this.ruleId = ruleId;
        }

        public static UpdateProcessingResult success(String assignmentId, String ruleId) {
            return new UpdateProcessingResult(true, null, null, assignmentId, ruleId);
        }

        public static UpdateProcessingResult failure(String errorCode, String errorMessage) {
            return new UpdateProcessingResult(false, errorCode, errorMessage, null, null);
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

        public String getAssignmentId() {
            return assignmentId;
        }

        public String getRuleId() {
            return ruleId;
        }

        public IdempotencyDto toIdempotencyDto() {
            return new IdempotencyDto(success, assignmentId, ruleId);
        }
    }

    /**
     * Serializable DTO for idempotency storage
     */
    public static class IdempotencyDto {
        private final boolean success;
        private final String assignmentId;
        private final String ruleId;

        public IdempotencyDto(boolean success, String assignmentId, String ruleId) {
            this.success = success;
            this.assignmentId = assignmentId;
            this.ruleId = ruleId;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getAssignmentId() {
            return assignmentId;
        }

        public String getRuleId() {
            return ruleId;
        }
    }
}
