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
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.*;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.*;
import vn.viettel.vds.promotion.validation.command.UpdateValidationRuleCommand;
import vn.viettel.vds.promotion.validation.command.UpdateValidationRuleCommand.ApplicabilityScope;
import vn.viettel.vds.promotion.validation.command.UpdateValidationRuleCommand.TimeFrame;
import vn.viettel.vds.promotion.validation.command.UpdateValidationRuleCommand.UpdateValidationRuleCommandPayload;
import vn.viettel.vds.promotion.validation.domain.common.ErrorCode;
import vn.viettel.vds.promotion.validation.domain.exception.TimeframeProcessingException;
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

    private final AssignmentJpaRepository assignmentRepository;
    private final AssignmentApplicabilityRuleJpaRepository applicabilityRuleRepository;
    private final RuleTimeFrameJpaRepository ruleTimeFrameRepository;
    private final TemporalPolicyJpaRepository temporalPolicyRepository;
    private final RuleTemporalLinkJpaRepository ruleTemporalLinkRepository;
    private final ValidationRuleJpaRepository validationRuleRepository;
    private final IdempotencyService idempotencyService;
    private final vn.viettel.vds.promotion.validation.domain.service.RulePublishingService rulePublishingService;
    private final Validator validator;
    private final UpdateValidationRuleCommandDTOMapper dtoMapper;
    private final AssignmentSnapshotService assignmentSnapshotService;
    private final ValidationRuleSnapshotService validationRuleSnapshotService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.validation-event:promotion_validation_event}")
    private String validationEventTopic;

    public UpdateValidationRuleCommandHandler(
            AssignmentJpaRepository assignmentRepository,
            AssignmentApplicabilityRuleJpaRepository applicabilityRuleRepository,
            RuleTimeFrameJpaRepository ruleTimeFrameRepository,
            TemporalPolicyJpaRepository temporalPolicyRepository,
            RuleTemporalLinkJpaRepository ruleTemporalLinkRepository,
            ValidationRuleJpaRepository validationRuleRepository,
            IdempotencyService idempotencyService,
            vn.viettel.vds.promotion.validation.domain.service.RulePublishingService rulePublishingService,
            Validator validator,
            UpdateValidationRuleCommandDTOMapper dtoMapper,
            AssignmentSnapshotService assignmentSnapshotService,
            ValidationRuleSnapshotService validationRuleSnapshotService,
            KafkaTemplate<String, Object> kafkaTemplate) {
        this.assignmentRepository = assignmentRepository;
        this.applicabilityRuleRepository = applicabilityRuleRepository;
        this.ruleTimeFrameRepository = ruleTimeFrameRepository;
        this.temporalPolicyRepository = temporalPolicyRepository;
        this.ruleTemporalLinkRepository = ruleTemporalLinkRepository;
        this.validationRuleRepository = validationRuleRepository;
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
            AssignmentEntity assignmentEntity;
            String assignmentId;
            String payloadAssignmentId = payload.getAssignmentId();

            if (payloadAssignmentId != null && !payloadAssignmentId.isBlank()) {
                // Find by assignmentId
                assignmentEntity = assignmentRepository.findById(payloadAssignmentId)
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

                List<AssignmentEntity> assignments = assignmentRepository.findByEntityTypeAndEntityId(objectType, objectId);
                if (assignments.isEmpty()) {
                    logger.error("Assignment not found: objectType={}, objectId={}", objectType, objectId);
                    throw ExceptionFactory.createValidationException(
                            ErrorCode.ASSIGNMENT_VALIDATION_NOT_FOUND.name(),
                            "Assignment not found: objectType=" + objectType + ", objectId=" + objectId
                    );
                }
                // Take the first one (latest by default query ordering)
                assignmentEntity = assignments.get(0);
                assignmentId = assignmentEntity.getId();
                logger.info("Found assignment by objectReference: objectType={}, objectId={}, assignmentId={}",
                        objectType, objectId, assignmentId);
            }

            // Step 3: Create snapshots BEFORE update for compensation
            String sagaId = command.getMetadata() != null ? command.getMetadata().get("sagaId") : null;

            // 3a. Create Assignment snapshot
            AssignmentSnapshotEntity assignmentSnapshot = assignmentSnapshotService.createSnapshot(
                    assignmentId, sagaId, commandId, AssignmentSnapshotEntity.SnapshotReason.BEFORE_UPDATE);

            // 3b. Create ValidationRule snapshot if ruleId exists (for RevertValidationRuleCommand support)
            String existingRuleId = assignmentEntity.getRuleId();
            Long ruleVersionBeforeUpdate = null;
            if (existingRuleId != null && !existingRuleId.isEmpty()) {
                var ruleOpt = validationRuleRepository.findById(existingRuleId);
                if (ruleOpt.isPresent()) {
                    ValidationRuleEntity rule = ruleOpt.get();
                    ruleVersionBeforeUpdate = rule.getRuleVersion();
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
            UpdateProcessingResult result = processUpdate(commandId, assignmentEntity, payload);

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
            AssignmentEntity assignmentEntity,
            UpdateValidationRuleCommandPayload payload) {
        try {
            String assignmentId = assignmentEntity.getId();
            logger.info("Processing update for assignmentId={}", assignmentId);

            // Update basic fields if provided
            if (payload.getActive() != null) {
                assignmentEntity.setActive(payload.getActive());
            }

            // Update ruleId with validation
            UpdateProcessingResult ruleIdResult = updateRuleIdIfProvided(assignmentEntity, payload);
            if (ruleIdResult != null) {
                return ruleIdResult;
            }

            if (payload.getObjectType() != null) {
                assignmentEntity.setEntityType(payload.getObjectType());
            }

            // Update objectId with duplicate check
            UpdateProcessingResult objectIdResult = updateObjectIdIfProvided(assignmentEntity, payload);
            if (objectIdResult != null) {
                return objectIdResult;
            }

            // Update includedAll from applicability
            updateIncludedAllIfApplicable(assignmentEntity, payload);

            assignmentEntity.setUpdatedAt(Instant.now());

            // Save assignment changes
            assignmentRepository.save(assignmentEntity);

            // Update applicability rules if provided
            ApplicabilityScope applicableTo = payload.getApplicableTo();
            if (applicableTo != null) {
                updateApplicabilityRules(assignmentEntity, applicableTo);
            }

            // Update timeframe if provided and re-deploy rule if needed
            boolean hasTemporalPolicyChanges = updateTimeframeIfProvided(assignmentEntity, payload);
            redeployRuleIfNeeded(assignmentEntity, payload, hasTemporalPolicyChanges, applicableTo);

            logger.info("Successfully updated assignmentId={}, updatedBy={}, reason={}",
                    assignmentId, payload.getUpdatedBy(), payload.getReason());

            return UpdateProcessingResult.success(assignmentId, assignmentEntity.getRuleId());

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
            AssignmentEntity assignmentEntity,
            UpdateValidationRuleCommandPayload payload) {
        if (payload.getRuleId() == null) {
            return null;
        }
        if (!validationRuleRepository.existsById(payload.getRuleId())) {
            return UpdateProcessingResult.failure(
                    ErrorCode.VALIDATION_RULE_NOT_FOUND.name(),
                    "Validation rule not found: " + payload.getRuleId()
            );
        }
        assignmentEntity.setRuleId(payload.getRuleId());
        return null;
    }

    /**
     * Update objectId if provided, with duplicate assignment check.
     * Returns failure result if duplicate exists, null if successful or not provided.
     */
    private UpdateProcessingResult updateObjectIdIfProvided(
            AssignmentEntity assignmentEntity,
            UpdateValidationRuleCommandPayload payload) {
        if (payload.getObjectId() == null) {
            return null;
        }
        String currentEntityId = assignmentEntity.getEntityId();
        String newObjectId = payload.getObjectId();
        if (!newObjectId.equals(currentEntityId)) {
            String objectType = payload.getObjectType() != null
                    ? payload.getObjectType()
                    : assignmentEntity.getEntityType();
            if (assignmentRepository.existsByEntityTypeAndEntityId(objectType, newObjectId)) {
                return UpdateProcessingResult.failure(
                        ErrorCode.DUPLICATE_ASSIGNMENT_VALIDATION_RULE.name(),
                        "Assignment already exists for objectType=" + objectType + ", objectId=" + newObjectId
                );
            }
        }
        assignmentEntity.setEntityId(payload.getObjectId());
        return null;
    }

    /**
     * Update includedAll flag if applicableTo specifies includedAll = true.
     */
    private void updateIncludedAllIfApplicable(
            AssignmentEntity assignmentEntity,
            UpdateValidationRuleCommandPayload payload) {
        if (payload.getApplicableTo() != null && Boolean.TRUE.equals(payload.getApplicableTo().getIncludedAll())) {
            assignmentEntity.setIncludedAll(true);
        }
    }

    /**
     * Update timeframe if provided. Returns true if temporal policy was changed.
     */
    private boolean updateTimeframeIfProvided(
            AssignmentEntity assignmentEntity,
            UpdateValidationRuleCommandPayload payload) {
        TimeFrame timeframe = payload.getTimeframe();
        if (timeframe == null) {
            return false;
        }
        updateTimeframe(assignmentEntity, timeframe);
        return true;
    }

    /**
     * Re-deploy rule to engine if needed (temporal policy changes or activation).
     */
    private void redeployRuleIfNeeded(
            AssignmentEntity assignmentEntity,
            UpdateValidationRuleCommandPayload payload,
            boolean hasTemporalPolicyChanges,
            ApplicabilityScope applicableTo) {
        if (hasTemporalPolicyChanges || Boolean.TRUE.equals(payload.getActive())) {
            deployRuleToEngine(assignmentEntity, assignmentEntity.getRuleId(), hasTemporalPolicyChanges, applicableTo);
        }
    }

    /**
     * Update applicability rules - delete old and create new
     */
    private void updateApplicabilityRules(AssignmentEntity assignmentEntity, ApplicabilityScope applicabilityScope) {
        String assignmentId = assignmentEntity.getId();
        logger.info("Updating applicability rules for assignmentId={}", assignmentId);

        // Delete existing applicability rules
        applicabilityRuleRepository.deleteByAssignmentId(assignmentId);
        logger.debug("Deleted existing applicability rules for assignmentId={}", assignmentId);

        int savedCount = 0;

        // Create included rules
        if (applicabilityScope.getIncluded() != null && !applicabilityScope.getIncluded().isEmpty()) {
            for (var rule : applicabilityScope.getIncluded()) {
                AssignmentApplicabilityRuleEntity entity = createApplicabilityRuleEntity(
                        assignmentEntity,
                        AssignmentApplicabilityRuleEntity.RuleType.INCLUDED.name(),
                        rule
                );
                applicabilityRuleRepository.save(entity);
                savedCount++;
            }
        }

        // Create excluded rules
        if (applicabilityScope.getExcluded() != null && !applicabilityScope.getExcluded().isEmpty()) {
            for (var rule : applicabilityScope.getExcluded()) {
                AssignmentApplicabilityRuleEntity entity = createApplicabilityRuleEntity(
                        assignmentEntity,
                        AssignmentApplicabilityRuleEntity.RuleType.EXCLUDED.name(),
                        rule
                );
                applicabilityRuleRepository.save(entity);
                savedCount++;
            }
        }

        logger.info("Saved {} applicability rules for assignmentId={}", savedCount, assignmentId);
    }

    /**
     * Create ApplicabilityRuleEntity from command data
     */
    private AssignmentApplicabilityRuleEntity createApplicabilityRuleEntity(
            AssignmentEntity assignment,
            String ruleType,
            UpdateValidationRuleCommand.ApplicabilityRule rule) {

        AssignmentApplicabilityRuleEntity entity = new AssignmentApplicabilityRuleEntity();
        entity.setId(IdGenerator.generateId());
        entity.setAssignment(assignment);
        entity.setRuleType(ruleType);

        if (rule.getObject() != null) {
            entity.setObjectType(rule.getObject().name());
        }
        entity.setObjectId(rule.getId());

        if (rule.getEffect() != null) {
            entity.setEffect(rule.getEffect().name());
        }

        if (rule.getTarget() != null) {
            entity.setTarget(rule.getTarget().name());
        }

        entity.setSkipInitially(rule.getSkipInitially() != null ? rule.getSkipInitially() : 0);
        entity.setRepeatCount(rule.getRepeat() != null ? rule.getRepeat() : 1);

        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());

        return entity;
    }

    /**
     * Update timeframe - delete old temporal links/policies and create new
     */
    @SuppressWarnings("java:S2139")
    private void updateTimeframe(AssignmentEntity assignment, TimeFrame timeframeData) {
        try {
            String assignmentId = assignment.getId();
            logger.info("Updating timeframe for assignmentId={}", assignmentId);

            // Delete existing temporal links and policies for this assignment
            List<RuleTemporalLinkEntity> existingLinks = ruleTemporalLinkRepository.findByAssignmentId(assignmentId);
            for (RuleTemporalLinkEntity link : existingLinks) {
                String policyId = link.getTemporalPolicy().getId();
                ruleTemporalLinkRepository.delete(link);
                // Delete temporal policy if no other links reference it
                if (ruleTemporalLinkRepository.findByTemporalPolicyId(policyId).isEmpty()) {
                    temporalPolicyRepository.deleteById(policyId);
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

            // Create TemporalPolicy
            TemporalPolicyEntity temporalPolicy = createTemporalPolicy(timeFrameId, timezone, timeframeData);
            temporalPolicy = temporalPolicyRepository.save(temporalPolicy);

            // Create TemporalPolicyWindows
            if (timeframeData.getValidityHoursPerDay() != null && !timeframeData.getValidityHoursPerDay().isEmpty()) {
                createTemporalPolicyWindows(temporalPolicy, timeframeData.getValidityHoursPerDay());
            }

            // Create RuleTemporalLink
            RuleTemporalLinkEntity link = new RuleTemporalLinkEntity();
            link.setId(IdGenerator.generateId());
            link.setAssignment(assignment);
            link.setTemporalPolicy(temporalPolicy);
            link.setMode(mode);
            link.setCreatedAt(Instant.now());
            link.setUpdatedAt(Instant.now());
            ruleTemporalLinkRepository.save(link);

            // Create RuleTimeFrame for backward compatibility (only if ruleId exists)
            String ruleId = assignment.getRuleId();
            if (ruleId != null && !ruleId.isEmpty()) {
                var validationRuleOpt = validationRuleRepository.findById(ruleId);
                if (validationRuleOpt.isPresent()) {
                    RuleTimeFrameEntity ruleTimeFrame = new RuleTimeFrameEntity();
                    ruleTimeFrame.setId(IdGenerator.generateId());
                    ruleTimeFrame.setValidationRule(validationRuleOpt.get());
                    ruleTimeFrame.setTimeFrameId(timeFrameId);
                    ruleTimeFrame.setMode(mode);
                    ruleTimeFrame.setCreatedAt(Instant.now());
                    ruleTimeFrame.setUpdatedAt(Instant.now());
                    ruleTimeFrameRepository.save(ruleTimeFrame);
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
     * Create TemporalPolicy entity from TimeFrame data
     */
    private TemporalPolicyEntity createTemporalPolicy(String timeFrameId, String timezone, TimeFrame timeframeData) {
        TemporalPolicyEntity policy = new TemporalPolicyEntity();
        policy.setId(IdGenerator.generateId());
        policy.setName("timeframe-" + timeFrameId);
        policy.setTz(timezone);

        if (timeframeData.getValidityTimeframe() != null) {
            var validity = timeframeData.getValidityTimeframe();
            policy.setStartTs(validity.getStartDate());
            policy.setEndTs(validity.getExpirationDate());

            if (validity.getInterval() != null || validity.getDuration() != null || validity.getActivityDurationAfterPublishing() != null) {
                Map<String, Object> metadata = new HashMap<>();
                if (validity.getInterval() != null) {
                    metadata.put("interval", validity.getInterval());
                }
                if (validity.getDuration() != null) {
                    metadata.put("duration", validity.getDuration());
                }
                if (validity.getActivityDurationAfterPublishing() != null) {
                    metadata.put("activityDurationAfterPublishing", validity.getActivityDurationAfterPublishing());
                }
                policy.setMetadata(metadata);
            }
        }

        if (timeframeData.getValidityDaysOfWeek() != null && !timeframeData.getValidityDaysOfWeek().isEmpty()) {
            String rrule = buildRRuleFromDaysOfWeek(timeframeData.getValidityDaysOfWeek());
            policy.setRrule(rrule);
        }

        policy.setCreatedAt(Instant.now());
        policy.setUpdatedAt(Instant.now());

        return policy;
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
     * Create TemporalPolicyWindow entities
     */
    private void createTemporalPolicyWindows(TemporalPolicyEntity temporalPolicy,
                                             List<UpdateValidationRuleCommand.ValidityHoursPerDay> validityHours) {
        for (var hours : validityHours) {
            TemporalPolicyWindowEntity window = new TemporalPolicyWindowEntity();
            window.setId(IdGenerator.generateId());
            window.setTemporalPolicy(temporalPolicy);
            window.setStart(extractTimeOnly(hours.getStartTime()));
            window.setEnd(extractTimeOnly(hours.getExpirationTime()));
            window.setCreatedAt(Instant.now());
            window.setUpdatedAt(Instant.now());

            temporalPolicy.getTimeOfDayWindows().add(window);
        }
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
     */
    private void deployRuleToEngine(AssignmentEntity assignmentEntity,
                                    String ruleId,
                                    boolean hasTemporalPolicy,
                                    ApplicabilityScope applicableToData) {
        try {
            if (assignmentEntity.getActive() == null || !assignmentEntity.getActive()) {
                logger.info("Skipping rule deployment - assignment is not active: ruleId={}, assignmentId={}",
                        ruleId, assignmentEntity.getId());
                return;
            }

            var validationRuleOpt = validationRuleRepository.findById(ruleId);
            if (validationRuleOpt.isEmpty()) {
                logger.warn("Validation rule not found for deployment: ruleId={}", ruleId);
                return;
            }

            var validationRule = validationRuleOpt.get();
            boolean ruleNotYetDeployed = (validationRule.getBundleHash() == null || validationRule.getBundleHash().isEmpty());
            boolean needsDeployment = ruleNotYetDeployed || hasTemporalPolicy;

            if (needsDeployment) {
                String deploymentReason = ruleNotYetDeployed ? "rule not yet deployed" : "temporal policy updated";
                logger.info("Deploying rule after update: ruleId={}, assignmentId={}, reason={}",
                        ruleId, assignmentEntity.getId(), deploymentReason);

                // Convert to SettingValidationRuleCommand.ApplicabilityScope for compatibility
                vn.viettel.vds.promotion.validation.command.SettingValidationRuleCommand.ApplicabilityScope settingApplicableTo = null;
                if (applicableToData != null) {
                    settingApplicableTo = convertToSettingApplicabilityScope(applicableToData);
                }

                var publishResult = rulePublishingService.publishRule(ruleId, assignmentEntity.getId(), settingApplicableTo);

                if (publishResult.isSuccess()) {
                    assignmentEntity.setTemporalBundleHash(publishResult.getBundleHash());
                    // Explicit save to persist temporalBundleHash - entity was saved before this method was called
                    assignmentRepository.save(assignmentEntity);
                    logger.info("Rule deployed successfully after update: ruleId={}, bundleHash={}",
                            ruleId, publishResult.getBundleHash());
                } else {
                    logger.error("Failed to deploy rule after update: ruleId={}, error={}",
                            ruleId, publishResult.getErrorMessage());
                }
            }

        } catch (Exception e) {
            logger.error("Error deploying rule to validation-engine: ruleId={}, assignmentId={}",
                    ruleId, assignmentEntity.getId(), e);
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
