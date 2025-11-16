package vn.viettel.vds.promotion.validation.application.service;

import com.promix.platform.core.error.ErrorDetail;
import com.promix.platform.core.exception.BusinessException;
import com.promix.platform.core.exception.factory.ExceptionFactory;
import com.promix.platform.core.util.IdGenerator;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.viettel.vds.promotion.validation.adapter.in.messaging.dto.SettingValidationRuleCommandDTO;
import vn.viettel.vds.promotion.validation.adapter.in.messaging.mapper.SettingValidationRuleCommandDTOMapper;
import vn.viettel.vds.promotion.validation.adapter.out.integration.ValidationEngineDeploymentService;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.AssignmentEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.RuleTemporalLinkEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.RuleTimeFrameEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.TemporalPolicyEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.TemporalPolicyWindowEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.ValidationRuleEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.AssignmentJpaRepository;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.RuleTemporalLinkJpaRepository;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.RuleTimeFrameJpaRepository;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.TemporalPolicyJpaRepository;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.ValidationRuleJpaRepository;
import vn.viettel.vds.promotion.validation.command.SettingValidationRuleCommand;
import vn.viettel.vds.promotion.validation.command.SettingValidationRuleCommand.ApplicabilityScope;
import vn.viettel.vds.promotion.validation.command.SettingValidationRuleCommand.SettingValidationRuleCommandPayload;
import vn.viettel.vds.promotion.validation.command.SettingValidationRuleCommand.TimeFrame;
import vn.viettel.vds.promotion.validation.domain.common.ErrorCode;
import vn.viettel.vds.promotion.validation.domain.common.Result;
import vn.viettel.vds.promotion.validation.domain.exception.TimeframeProcessingException;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service to handle SettingValidationRuleCommand processing
 */
@Service
@Transactional
public class SettingValidationRuleCommandHandler {

    private static final Logger logger = LoggerFactory.getLogger(SettingValidationRuleCommandHandler.class);

    private final AssignmentJpaRepository assignmentRepository;
    private final RuleTimeFrameJpaRepository ruleTimeFrameRepository;
    private final TemporalPolicyJpaRepository temporalPolicyRepository;
    private final RuleTemporalLinkJpaRepository ruleTemporalLinkRepository;
    private final ValidationRuleJpaRepository validationRuleRepository;
    private final SettingValidationRuleEventPublisher eventPublisher;
    private final IdempotencyService idempotencyService;
    private final vn.viettel.vds.promotion.validation.domain.service.RulePublishingService rulePublishingService;
    private final Validator validator;
    private final SettingValidationRuleCommandDTOMapper dtoMapper;

    public SettingValidationRuleCommandHandler(
            AssignmentJpaRepository assignmentRepository,
            RuleTimeFrameJpaRepository ruleTimeFrameRepository,
            TemporalPolicyJpaRepository temporalPolicyRepository,
            RuleTemporalLinkJpaRepository ruleTemporalLinkRepository,
            ValidationRuleJpaRepository validationRuleRepository,
            SettingValidationRuleEventPublisher eventPublisher,
            IdempotencyService idempotencyService,
            vn.viettel.vds.promotion.validation.domain.service.RulePublishingService rulePublishingService,
            Validator validator,
            SettingValidationRuleCommandDTOMapper dtoMapper) {
        this.assignmentRepository = assignmentRepository;
        this.ruleTimeFrameRepository = ruleTimeFrameRepository;
        this.temporalPolicyRepository = temporalPolicyRepository;
        this.ruleTemporalLinkRepository = ruleTemporalLinkRepository;
        this.validationRuleRepository = validationRuleRepository;
        this.eventPublisher = eventPublisher;
        this.idempotencyService = idempotencyService;
        this.rulePublishingService = rulePublishingService;
        this.validator = validator;
        this.dtoMapper = dtoMapper;
    }

    /**
     * Handle SettingValidationRuleCommand
     */
    @SuppressWarnings("java:S2139") // Exception is properly logged before rethrowing
    public boolean handleCommand(SettingValidationRuleCommand command) {
        String commandId = command.getId();

        try {
            logger.info("Processing SettingValidationRuleCommand: commandId={}", commandId);

            // Check idempotency - if already processed, return success immediately
            if (idempotencyService.isProcessed(commandId)) {
                logger.info("Command already processed (idempotent check): commandId={}", commandId);
                return true;
            }

            // Step 1: Validate command using Bean Validation
            validateCommand(command);

            // Extract command payload
            SettingValidationRuleCommandPayload payload = command.getPayload();
            if (payload == null) {
                logger.error("Command payload is null: commandId={}", commandId);
                publishErrorEvent(commandId, null, "INVALID_PAYLOAD", "Command payload is missing");
                return false;
            }

            // Extract campaignId from payload for error events
            String campaignId = payload.getObjectId();

            // Process the command
            CommandProcessingResult result = processCommand(commandId, payload);

            // Publish success/failure event
            if (result.isSuccess()) {
                publishSuccessEvent(commandId, result);

                // Mark as processed after successful processing
                // Convert to serializable DTO to avoid Avro Schema serialization issues
                IdempotencyResultDto idempotencyDto = result.toIdempotencyDto();
                idempotencyService.markAsProcessed(commandId, idempotencyDto);

                logger.info("Successfully processed SettingValidationRuleCommand: commandId={}", commandId);
                return true;
            } else {
                publishErrorEvent(commandId, campaignId, result.getErrorCode(), result.getErrorMessage());
                logger.error("Failed to process SettingValidationRuleCommand: commandId={}, error={}",
                        commandId, result.getErrorMessage());
                return false;
            }

        } catch (BusinessException e) {
            // Re-throw BusinessException (validation errors) to let promix-messaging handle it
            // BusinessException with BAD_REQUEST → DLQ immediately (non-retryable)
            logger.error("Validation failed for SettingValidationRuleCommand: commandId={}, error={}",
                commandId, e.getMessage(), e);
            throw e; // NOSONAR - Exception is logged before rethrowing for proper error tracking
        } catch (Exception e) {
            logger.error("Unexpected error processing SettingValidationRuleCommand: commandId={}", commandId, e);
            // Try to extract campaignId from command for error event
            String campaignId = command.getPayload() != null ? command.getPayload().getObjectId() : null;
            publishErrorEvent(commandId, campaignId, "PROCESSING_ERROR", "Unexpected error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Validate command using Bean Validation annotations on DTO.
     *
     * Validation flow:
     * 1. Check command and payload not null
     * 2. Convert command to DTO
     * 3. Run Bean Validation with group sequence
     * 4. Throw ValidationException if validation fails
     *
     * @param command the command to validate
     * @throws RuntimeException if validation fails with specific error codes
     */
    private void validateCommand(SettingValidationRuleCommand command) {
        // Step 1: Null check
        if (command == null || command.getPayload() == null) {
            logger.error("Received null command or null payload");
            throw ExceptionFactory.createValidationException(
                "INVALID_COMMAND",
                "Command or payload is null"
            );
        }

        // Step 2: Convert to DTO
        SettingValidationRuleCommandDTO dto = dtoMapper.toDTO(command);
        if (dto == null) {
            logger.error("Failed to convert command to DTO: commandId={}", command.getId());
            throw ExceptionFactory.createValidationException(
                "INVALID_COMMAND",
                "Failed to convert command to DTO"
            );
        }

        // Step 3: Bean Validation
        Set<ConstraintViolation<SettingValidationRuleCommandDTO>> violations = validator.validate(dto);

        // Step 4: Handle validation errors
        if (!violations.isEmpty()) {
            // Build ErrorDetail list from all violations
            List<ErrorDetail> errorDetails = violations.stream()
                .map(violation -> ErrorDetail.of(
                    violation.getPropertyPath().toString(),  // field
                    violation.getMessage(),                  // errorCode (from annotation)
                    String.format("Invalid value: %s", violation.getInvalidValue()),  // message
                    violation.getInvalidValue()             // details
                ))
                .toList();

            // Build summary error message
            String errorMessage = String.format("Validation failed with %d error(s)", violations.size());

            logger.error("SettingValidationRuleCommand validation failed: commandId={}, errorCount={}, errors={}",
                command.getId(), violations.size(), errorDetails);

            throw ExceptionFactory.createValidationException(
                "METHOD_ARGUMENT_NOT_VALID",
                errorMessage,
                errorDetails.toArray(new ErrorDetail[0])
            );
        }

        logger.debug("SettingValidationRuleCommand validation passed: commandId={}", command.getId());
    }

    /**
     * Process the command and create/update assignments
     */
    private CommandProcessingResult processCommand(
            String commandId,
            SettingValidationRuleCommandPayload payload) {
        try {
            // Validate command components
            Result<ComponentsData> componentsResult = validateCommandComponents(payload);
            if (componentsResult.isFailure()) {
                return CommandProcessingResult.failure(
                        componentsResult.getFirstErrorCode().orElse(ErrorCode.COMMAND_VALIDATION_ERROR).name(),
                        componentsResult.getFirstErrorMessage().orElse("Command validation failed")
                );
            }

            ComponentsData components = componentsResult.getValue();

            // ✅ REMOVED: No longer create node in DB
            // Product applicability node will be created dynamically during compilation
            // by RulePublishingService based on applicableToData

            // ✅ Create rule assignment with objectType + objectId
            vn.viettel.vds.promotion.validation.domain.model.Assignment assignment =
                    createRuleAssignment(
                            components.ruleId(),
                            components.objectType(),
                            components.objectId(),
                            components.active(),
                            components.trafficPercent()
                    );

            // Convert to JPA entity and save
            AssignmentEntity assignmentEntity = toAssignmentEntity(assignment);
            assignmentEntity = assignmentRepository.save(assignmentEntity);
            assignment.setId(assignmentEntity.getId());

            if (logger.isInfoEnabled()) {
                logger.info("Created assignment: objectType={}, objectId={}, ruleId={}, assignmentId={}",
                        components.objectType(), components.objectId(), components.ruleId(), assignment.getId());
            }

            // Process timeframe if provided
            String timeFrameId = null;
            boolean hasTemporalPolicy = false;
            if (components.timeframeData() != null) {
                timeFrameId = processTimeframe(assignmentEntity, components.timeframeData());
                hasTemporalPolicy = true;
            }

            // Deploy rule to validation-engine
            // Pass hasTemporalPolicy flag directly instead of querying DB (avoids @Transactional timing issues)
            // Pass assignmentEntity to update temporal_bundle_hash after deployment
            // Pass applicableToData to create product applicability node dynamically
            deployRuleToEngine(assignmentEntity, components.ruleId(), hasTemporalPolicy, components.applicableToData());

            // Create processing result
            return CommandProcessingResult.success(
                    assignment,
                    components.applicableToData(),
                    timeFrameId,
                    components.timeframeData()
            );

        } catch (Exception e) {
            logger.error("Error processing command components: commandId={}", commandId, e);
            return CommandProcessingResult.failure("PROCESSING_ERROR", e.getMessage());
        }
    }

    /**
     * Validate command components and extract data
     */
    private Result<ComponentsData> validateCommandComponents(
            SettingValidationRuleCommandPayload payload) {

        // Extract command components (flattened structure)
        String ruleId = payload.getRuleId();
        String objectType = payload.getObjectType();
        String objectId = payload.getObjectId();
        Boolean active = payload.getActive();
        Integer trafficPercent = payload.getTrafficPercent();
        ApplicabilityScope applicableToData = payload.getApplicableTo();
        TimeFrame timeframeData = payload.getTimeframe();
        Integer priority = payload.getPriority();
        String notes = payload.getNotes();

        // Validate required fields
        if (ruleId == null || ruleId.isEmpty()) {
            return Result.failure(ErrorCode.MISSING_ASSIGN_RULE, "ruleId is required in payload");
        }

        if (objectType == null || objectType.isEmpty()) {
            return Result.failure(ErrorCode.COMMAND_VALIDATION_ERROR, "objectType is required in payload");
        }

        if (objectId == null || objectId.isEmpty()) {
            return Result.failure(ErrorCode.COMMAND_VALIDATION_ERROR, "objectId is required in payload");
        }

        // Validate rule exists
        if (!validationRuleRepository.existsById(ruleId)) {
            return Result.failure(ErrorCode.RULE_NOT_FOUND, "Validation rule not found: " + ruleId);
        }

        // Return validated components
        return Result.success(new ComponentsData(
                ruleId,
                objectType,
                objectId,
                active,
                trafficPercent,
                applicableToData,
                timeframeData,
                priority,
                notes
        ));
    }

    /**
     * ✅ Create RuleAssignment entity with objectType + objectId (flexible design)
     */
    private vn.viettel.vds.promotion.validation.domain.model.Assignment createRuleAssignment(
            String ruleId,
            String objectType,
            String objectId,
            Boolean active,
            Integer trafficPercent) {

        // Generate assignment ID
        String assignmentId = IdGenerator.generateId();

        // ✅ Create Subject with objectType + objectId (supports multiple object types)
        vn.viettel.vds.promotion.validation.domain.model.Assignment.Subject subject =
                new vn.viettel.vds.promotion.validation.domain.model.Assignment.Subject();
        subject.setType(objectType);      // "campaign", "product", "customer", etc.
        subject.setKey(objectId);         // ID of the object

        // Create and configure assignment
        vn.viettel.vds.promotion.validation.domain.model.Assignment assignment =
                new vn.viettel.vds.promotion.validation.domain.model.Assignment();
        assignment.setId(assignmentId);
        assignment.setRuleId(ruleId);
        assignment.setSubject(subject);
        assignment.setAssignmentVersion(1);
        assignment.setActive(active == null || active);
        assignment.setTrafficPercent(trafficPercent != null ? trafficPercent : 100);
        assignment.setCreatedAt(Instant.now());
        assignment.setUpdatedAt(Instant.now());

        logger.debug("Created assignment entity: assignmentId={}, objectType={}, objectId={}, ruleId={}",
                assignmentId, objectType, objectId, ruleId);

        return assignment;
    }

    /**
     * Process timeframe configuration - ENHANCED version
     * Creates TemporalPolicy, TemporalPolicyWindows, and RuleTemporalLink
     *
     * FIXED: Link temporal policy with assignment instead of validation rule
     * Rationale: Temporal constraints are assignment-specific, not rule-specific
     */
    @SuppressWarnings("java:S2139") // Exception is properly logged and wrapped with contextual information
    private String processTimeframe(AssignmentEntity assignment, TimeFrame timeframeData) {
        try {
            logger.info("Processing timeframe for assignmentId={}, ruleId={}",
                    assignment.getId(), assignment.getRuleId());

            // Extract timeframe components
            String timeFrameId = timeframeData.getTimeFrameId();
            String mode = timeframeData.getMode() != null ? timeframeData.getMode().toString() : "REQUIRED";
            String timezone = timeframeData.getTimezone() != null ? timeframeData.getTimezone() : "UTC";

            // Generate timeFrame ID if not provided
            if (timeFrameId == null) {
                timeFrameId = IdGenerator.generateId();
            }

            // Get validation rule entity for backward compatibility (legacy)
            ValidationRuleEntity validationRule = validationRuleRepository.findById(assignment.getRuleId())
                    .orElseThrow(() -> new RuntimeException("Validation rule not found: " + assignment.getRuleId()));

            // Step 1: Create TemporalPolicy entity
            TemporalPolicyEntity temporalPolicy = createTemporalPolicy(timeFrameId, timezone, timeframeData);
            temporalPolicy = temporalPolicyRepository.save(temporalPolicy);
            logger.debug("Created temporal policy: policyId={}, name={}", temporalPolicy.getId(), temporalPolicy.getName());

            // Step 2: Create TemporalPolicyWindow entities for validity hours
            if (timeframeData.getValidityHoursPerDay() != null && !timeframeData.getValidityHoursPerDay().isEmpty()) {
                createTemporalPolicyWindows(temporalPolicy, timeframeData.getValidityHoursPerDay());
                logger.debug("Created {} validity hour windows", timeframeData.getValidityHoursPerDay().size());
            }

            // Step 3: Create RuleTemporalLink to link assignment with temporal policy
            // FIXED: Link with assignment instead of validationRule
            RuleTemporalLinkEntity link = new RuleTemporalLinkEntity();
            link.setId(IdGenerator.generateId());
            link.setAssignment(assignment);  // FIXED: was setValidationRule
            link.setTemporalPolicy(temporalPolicy);
            link.setMode(mode);
            link.setCreatedAt(Instant.now());
            link.setUpdatedAt(Instant.now());
            ruleTemporalLinkRepository.save(link);
            logger.debug("Created assignment temporal link: linkId={}, assignmentId={}, policyId={}, mode={}",
                    link.getId(), assignment.getId(), temporalPolicy.getId(), mode);

            // Step 4: Keep RuleTimeFrame for backward compatibility (legacy)
            RuleTimeFrameEntity ruleTimeFrame = new RuleTimeFrameEntity();
            ruleTimeFrame.setId(IdGenerator.generateId());
            ruleTimeFrame.setValidationRule(validationRule);
            ruleTimeFrame.setTimeFrameId(timeFrameId);
            ruleTimeFrame.setMode(mode);
            ruleTimeFrame.setCreatedAt(Instant.now());
            ruleTimeFrame.setUpdatedAt(Instant.now());
            ruleTimeFrameRepository.save(ruleTimeFrame);

            logger.info("Successfully processed timeframe: assignmentId={}, ruleId={}, timeFrameId={}, policyId={}",
                    assignment.getId(), assignment.getRuleId(), timeFrameId, temporalPolicy.getId());
            return timeFrameId;

        } catch (Exception e) {
            logger.error("Failed to process timeframe for assignmentId={}, ruleId={}: {}",
                    assignment.getId(), assignment.getRuleId(), e.getMessage(), e);
            throw new TimeframeProcessingException( // NOSONAR - Exception is logged and wrapped with contextual information
                    "Failed to process timeframe for assignmentId=" + assignment.getId() +
                    ", ruleId=" + assignment.getRuleId() + " due to: " + e.getMessage(), e);
        }
    }

    /**
     * Create TemporalPolicy entity from TimeFrame data
     */
    private TemporalPolicyEntity createTemporalPolicy(String timeFrameId, String timezone, TimeFrame timeframeData) {
        TemporalPolicyEntity policy = new TemporalPolicyEntity();
        policy.setId(IdGenerator.generateId());
        policy.setName("timeframe-" + timeFrameId); // Unique name based on timeFrameId
        policy.setTz(timezone);

        // Set validity timeframe (startDate and expirationDate)
        if (timeframeData.getValidityTimeframe() != null) {
            var validity = timeframeData.getValidityTimeframe();
            policy.setStartTs(validity.getStartDate());
            policy.setEndTs(validity.getExpirationDate());

            // Store interval, duration, activityDurationAfterPublishing in metadata
            if (validity.getInterval() != null || validity.getDuration() != null || validity.getActivityDurationAfterPublishing() != null) {
                java.util.Map<String, Object> metadata = new java.util.HashMap<>();
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

        // Convert validityDaysOfWeek to RRULE (RFC 5545 format)
        if (timeframeData.getValidityDaysOfWeek() != null && !timeframeData.getValidityDaysOfWeek().isEmpty()) {
            String rrule = buildRRuleFromDaysOfWeek(timeframeData.getValidityDaysOfWeek());
            policy.setRrule(rrule);
            logger.debug("Built RRULE from daysOfWeek: {}", rrule);
        }

        policy.setCreatedAt(Instant.now());
        policy.setUpdatedAt(Instant.now());

        return policy;
    }

    /**
     * Build RRULE string from validityDaysOfWeek
     * Example: [1, 3, 5] -> "FREQ=WEEKLY;BYDAY=MO,WE,FR"
     */
    private String buildRRuleFromDaysOfWeek(List<Integer> daysOfWeek) {
        // Map integers to RFC 5545 day codes: 1=MO, 2=TU, 3=WE, 4=TH, 5=FR, 6=SA, 7=SU
        String[] dayCodes = {"MO", "TU", "WE", "TH", "FR", "SA", "SU"};

        String byDay = daysOfWeek.stream()
                .filter(day -> day >= 1 && day <= 7)
                .map(day -> dayCodes[day - 1])
                .collect(Collectors.joining(","));

        return "FREQ=WEEKLY;BYDAY=" + byDay;
    }

    /**
     * Create TemporalPolicyWindow entities for validity hours per day
     */
    private void createTemporalPolicyWindows(TemporalPolicyEntity temporalPolicy,
                                            List<SettingValidationRuleCommand.ValidityHoursPerDay> validityHours) {
        for (var hours : validityHours) {
            TemporalPolicyWindowEntity window = new TemporalPolicyWindowEntity();
            window.setId(IdGenerator.generateId());
            window.setTemporalPolicy(temporalPolicy);

            // Parse startTime and endTime (format: "HH:mm:ss+07:00" or "HH:mm")
            window.setStart(extractTimeOnly(hours.getStartTime()));
            window.setEnd(extractTimeOnly(hours.getExpirationTime()));

            window.setCreatedAt(Instant.now());
            window.setUpdatedAt(Instant.now());

            // Add to temporal policy's windows collection
            temporalPolicy.getTimeOfDayWindows().add(window);

            logger.debug("Created temporal policy window: dayOfWeek={}, start={}, end={}",
                    hours.getDayOfWeek(), window.getStart(), window.getEnd());
        }
    }

    /**
     * Extract time portion from time string (e.g., "09:00:00+07:00" -> "09:00")
     */
    private String extractTimeOnly(String timeString) {
        if (timeString == null) {
            return null;
        }
        // Remove timezone offset and seconds if present
        // "09:00:00+07:00" -> "09:00"
        // "09:00+07:00" -> "09:00"
        // "09:00" -> "09:00"
        String time = timeString.split("\\+")[0].split("-")[0]; // Remove timezone
        String[] parts = time.split(":");
        if (parts.length >= 2) {
            return parts[0] + ":" + parts[1]; // HH:mm
        }
        return time;
    }

    /**
     * Publish success event
     */
    private void publishSuccessEvent(String commandId, CommandProcessingResult result) {
        try {
            eventPublisher.publishSuccessEvent(commandId, result);
        } catch (Exception e) {
            logger.error("Failed to publish success event: commandId={}", commandId, e);
        }
    }

    /**
     * Publish error event
     */
    private void publishErrorEvent(String commandId, String campaignId, String errorCode, String errorMessage) {
        try {
            eventPublisher.publishErrorEvent(commandId, campaignId, errorCode, errorMessage);
        } catch (Exception e) {
            logger.error("Failed to publish error event: commandId={}", commandId, e);
        }
    }

    /**
     * Deploy rule to validation-engine after successful assignment creation
     * Checks if rule has bundleHash, if not, deploys it via RulePublishingService
     * ENHANCED: Also re-deploys if assignment has temporal policy to ensure temporal constraints are sent to validation-engine
     *
     * @param assignmentEntity Assignment entity (to update temporal_bundle_hash after deployment)
     * @param ruleId Rule identifier
     * @param hasTemporalPolicy Whether assignment has temporal policy (passed directly to avoid @Transactional timing issues)
     * @param applicableToData Applicability scope data to create product applicability node dynamically
     */
    private void deployRuleToEngine(AssignmentEntity assignmentEntity,
                                    String ruleId,
                                    boolean hasTemporalPolicy,
                                    ApplicabilityScope applicableToData) {
        try {
            // Only deploy if assignment is active
            if (assignmentEntity.getActive() == null || !assignmentEntity.getActive()) {
                logger.info("Skipping rule deployment - assignment is not active: ruleId={}, assignmentId={}",
                        ruleId, assignmentEntity.getId());
                return;
            }

            // Get the validation rule details
            var validationRuleOpt = validationRuleRepository.findById(ruleId);
            if (validationRuleOpt.isEmpty()) {
                logger.warn("Validation rule not found for deployment: ruleId={}", ruleId);
                return;
            }

            var validationRule = validationRuleOpt.get();

            // Check if rule already has bundleHash
            boolean ruleNotYetDeployed = (validationRule.getBundleHash() == null || validationRule.getBundleHash().isEmpty());

            // Deploy if: rule not deployed YET OR assignment has temporal policy (need to send temporal constraints to engine)
            boolean needsDeployment = ruleNotYetDeployed || hasTemporalPolicy;

            if (!needsDeployment) {
                logger.info("Rule already deployed and no temporal policy: ruleId={}, bundleHash={}",
                        ruleId, validationRule.getBundleHash());
            } else {
                // Log deployment reason
                String deploymentReason = ruleNotYetDeployed ? "rule not yet deployed" : "temporal policy present";
                logger.info("Deploying rule: ruleId={}, assignmentId={}, reason={}",
                        ruleId, assignmentEntity.getId(), deploymentReason);

                // Deploy rule - pass assignmentId to include temporal policy data and applicableToData for product applicability node
                vn.viettel.vds.promotion.validation.domain.service.RulePublishingService.RulePublishResult publishResult =
                        rulePublishingService.publishRule(ruleId, assignmentEntity.getId(), applicableToData);

                if (publishResult.isSuccess()) {
                    String deployedBundleHash = publishResult.getBundleHash();

                    // Update assignment with bundleHash (especially important when hasTemporalPolicy=true)
                    // This bundleHash contains both business rule + temporal policy
                    assignmentEntity.setTemporalBundleHash(deployedBundleHash);
                    // No need to call assignmentRepository.save() - JPA will auto-flush in @Transactional

                    logger.info("Rule deployed successfully: ruleId={}, assignmentId={}, bundleHash={}, artifactSize={}, hasTemporalPolicy={}",
                            ruleId, assignmentEntity.getId(), deployedBundleHash, publishResult.getArtifactSize(), hasTemporalPolicy);
                    logger.info("Updated assignment.temporal_bundle_hash: assignmentId={}, bundleHash={}",
                            assignmentEntity.getId(), deployedBundleHash);
                } else {
                    logger.error("Failed to deploy rule: ruleId={}, assignmentId={}, error={}",
                            ruleId, assignmentEntity.getId(), publishResult.getErrorMessage());
                    // Don't fail the entire command - assignment is already created
                }
            }

            logger.info("Rule deployment check completed: ruleId={}, assignmentId={}, needsDeployment={}, hasTemporalPolicy={}",
                    ruleId, assignmentEntity.getId(), needsDeployment, hasTemporalPolicy);

        } catch (Exception e) {
            logger.error("Error deploying rule to validation-engine: ruleId={}, assignmentId={}",
                    ruleId, assignmentEntity.getId(), e);
            // Don't fail the entire command processing for deployment issues
        }
    }

    /**
     * Handle dead letter commands (for monitoring/alerting)
     */
    public void handleDeadLetterCommand(SettingValidationRuleCommand command) {
        String commandId = command.getId();
        logger.error("Processing dead letter command: commandId={}", commandId);

        // Could implement:
        // - Store in dead letter table for manual processing
        // - Send alerts to monitoring system
        // - Log detailed information for debugging

        try {
            // Publish dead letter event for monitoring
            eventPublisher.publishDeadLetterEvent(commandId);
        } catch (Exception e) {
            logger.error("Failed to publish dead letter event: commandId={}", commandId, e);
        }
    }

    /**
     * Convert domain Assignment to JPA AssignmentEntity
     * Maps domain Subject (type/key) to entity fields (entityType/entityId)
     */
    private AssignmentEntity toAssignmentEntity(vn.viettel.vds.promotion.validation.domain.model.Assignment assignment) {
        AssignmentEntity entity = new AssignmentEntity();
        entity.setId(assignment.getId());
        entity.setRuleId(assignment.getRuleId());

        // Convert Subject to flat fields (entityType/entityId)
        if (assignment.getSubject() != null) {
            entity.setEntityType(assignment.getSubject().getType());
            entity.setEntityId(assignment.getSubject().getKey());
        }

        entity.setActive(assignment.getActive());
        entity.setCreatedAt(assignment.getCreatedAt());
        entity.setUpdatedAt(assignment.getUpdatedAt());

        // Note: Fields removed from entity schema:
        // - tenantId (removed - multi-tenancy handled at application level)
        // - ruleVersionPinned (removed from schema)
        // - assignmentVersion (removed from schema)
        // - validFrom/validTo (removed from schema)
        // - trafficPercent (removed from schema)
        // - stickyKeyStrategy (removed from schema)

        return entity;
    }

    /**
     * Record to hold validated command components (flattened structure)
     */
    private record ComponentsData(
            String ruleId,
            String objectType,
            String objectId,
            Boolean active,
            Integer trafficPercent,
            ApplicabilityScope applicableToData,
            TimeFrame timeframeData,
            Integer priority,
            String notes
    ) {
    }

    /**
     * Serializable DTO for idempotency storage
     * Excludes Avro Schema objects to prevent Jackson serialization errors
     */
    public static class IdempotencyResultDto {
        private final boolean success;
        private final String assignmentId;
        private final String ruleId;
        private final String timeFrameId;

        public IdempotencyResultDto(boolean success, String assignmentId, String ruleId, String timeFrameId) {
            this.success = success;
            this.assignmentId = assignmentId;
            this.ruleId = ruleId;
            this.timeFrameId = timeFrameId;
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

        public String getTimeFrameId() {
            return timeFrameId;
        }
    }

    /**
     * Result wrapper for command processing
     */
    public static class CommandProcessingResult {
        private final boolean success;
        private final String errorCode;
        private final String errorMessage;
        private final vn.viettel.vds.promotion.validation.domain.model.Assignment assignment;
        private final ApplicabilityScope applicabilityData;
        private final String timeFrameId;
        private final TimeFrame timeframeData;

        private CommandProcessingResult(Builder builder) {
            this.success = builder.success;
            this.errorCode = builder.errorCode;
            this.errorMessage = builder.errorMessage;
            this.assignment = builder.assignment;
            this.applicabilityData = builder.applicabilityData;
            this.timeFrameId = builder.timeFrameId;
            this.timeframeData = builder.timeframeData;
        }

        public static CommandProcessingResult success(vn.viettel.vds.promotion.validation.domain.model.Assignment assignment,
                                                      ApplicabilityScope applicabilityData,
                                                      String timeFrameId,
                                                      TimeFrame timeframeData) {
            return new Builder()
                    .success(true)
                    .assignment(assignment)
                    .applicabilityData(applicabilityData)
                    .timeFrameId(timeFrameId)
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

        // Getters
        public boolean isSuccess() {
            return success;
        }

        public String getErrorCode() {
            return errorCode;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public vn.viettel.vds.promotion.validation.domain.model.Assignment getAssignment() {
            return assignment;
        }

        public ApplicabilityScope getApplicabilityData() {
            return applicabilityData;
        }

        public String getTimeFrameId() {
            return timeFrameId;
        }

        public TimeFrame getTimeframeData() {
            return timeframeData;
        }

        /**
         * Convert to serializable DTO for idempotency storage
         * Excludes Avro objects that cannot be serialized by Jackson
         */
        public IdempotencyResultDto toIdempotencyDto() {
            String assignmentId = assignment != null ? assignment.getId() : null;
            String ruleId = assignment != null ? assignment.getRuleId() : null;
            return new IdempotencyResultDto(success, assignmentId, ruleId, timeFrameId);
        }

        static class Builder {
            private boolean success;
            private String errorCode;
            private String errorMessage;
            private vn.viettel.vds.promotion.validation.domain.model.Assignment assignment;
            private ApplicabilityScope applicabilityData;
            private String timeFrameId;
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

            Builder assignment(vn.viettel.vds.promotion.validation.domain.model.Assignment assignment) {
                this.assignment = assignment;
                return this;
            }

            Builder applicabilityData(ApplicabilityScope applicabilityData) {
                this.applicabilityData = applicabilityData;
                return this;
            }

            Builder timeFrameId(String timeFrameId) {
                this.timeFrameId = timeFrameId;
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