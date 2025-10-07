package vn.viettel.vds.promotion.validation.application.service;

import com.promix.platform.core.util.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.viettel.vds.promotion.validation.domain.common.ErrorCode;
import vn.viettel.vds.promotion.validation.domain.common.Result;
import vn.viettel.vds.promotion.schema.validation.command.ApplicabilityScope;
import vn.viettel.vds.promotion.schema.validation.command.SettingValidationRuleCommand;
import vn.viettel.vds.promotion.schema.validation.command.SettingValidationRuleCommandPayload;
import vn.viettel.vds.promotion.schema.validation.command.TimeFrame;
import vn.viettel.vds.promotion.validation.adapter.out.integration.ValidationEngineDeploymentService;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.AssignmentEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.RuleNodeEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.RuleTimeFrameEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.ValidationRuleEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.AssignmentJpaRepository;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.RuleTimeFrameJpaRepository;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.ValidationRuleJpaRepository;

import java.time.Instant;
import java.util.List;

/**
 * Service to handle SettingValidationRuleCommand processing
 */
@Service
@Transactional
public class SettingValidationRuleCommandHandler {

    private static final Logger logger = LoggerFactory.getLogger(SettingValidationRuleCommandHandler.class);

    private final AssignmentJpaRepository assignmentRepository;
    private final RuleTimeFrameJpaRepository ruleTimeFrameRepository;
    private final ValidationRuleJpaRepository validationRuleRepository;
    private final SettingValidationRuleEventPublisher eventPublisher;
    private final ValidationEngineDeploymentService validationEngineClient;
    private final IdempotencyService idempotencyService;

    public SettingValidationRuleCommandHandler(
            AssignmentJpaRepository assignmentRepository,
            RuleTimeFrameJpaRepository ruleTimeFrameRepository,
            ValidationRuleJpaRepository validationRuleRepository,
            SettingValidationRuleEventPublisher eventPublisher,
            ValidationEngineDeploymentService validationEngineClient,
            IdempotencyService idempotencyService) {
        this.assignmentRepository = assignmentRepository;
        this.ruleTimeFrameRepository = ruleTimeFrameRepository;
        this.validationRuleRepository = validationRuleRepository;
        this.eventPublisher = eventPublisher;
        this.validationEngineClient = validationEngineClient;
        this.idempotencyService = idempotencyService;
    }

    /**
     * Handle SettingValidationRuleCommand
     */
    public boolean handleCommand(SettingValidationRuleCommand command) {
        String commandId = command.getId().toString();

        try {
            logger.info("Processing SettingValidationRuleCommand: commandId={}", commandId);

            // Check idempotency - if already processed, return success immediately
            if (idempotencyService.isProcessed(commandId)) {
                logger.info("Command already processed (idempotent check): commandId={}", commandId);
                return true;
            }

            // Extract command payload
            SettingValidationRuleCommandPayload payload = command.getPayload();
            if (payload == null) {
                logger.error("Command payload is null: commandId={}", commandId);
                publishErrorEvent(commandId, "INVALID_PAYLOAD", "Command payload is missing");
                return false;
            }

            // Process the command
            CommandProcessingResult result = processCommand(commandId, command, payload);

            // Publish success/failure event
            if (result.isSuccess()) {
                publishSuccessEvent(commandId, result);

                // Mark as processed after successful processing
                idempotencyService.markAsProcessed(commandId, result);

                logger.info("Successfully processed SettingValidationRuleCommand: commandId={}", commandId);
                return true;
            } else {
                publishErrorEvent(commandId, result.getErrorCode(), result.getErrorMessage());
                logger.error("Failed to process SettingValidationRuleCommand: commandId={}, error={}",
                        commandId, result.getErrorMessage());
                return false;
            }

        } catch (Exception e) {
            logger.error("Unexpected error processing SettingValidationRuleCommand: commandId={}", commandId, e);
            publishErrorEvent(commandId, "PROCESSING_ERROR", "Unexpected error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Process the command and create/update assignments
     */
    private CommandProcessingResult processCommand(
            String commandId,
            SettingValidationRuleCommand command,
            SettingValidationRuleCommandPayload payload) {
        try {
            // Validate command components
            Result<ComponentsData> componentsResult = validateCommandComponents(command, payload);
            if (componentsResult.isFailure()) {
                return CommandProcessingResult.failure(
                    componentsResult.getFirstErrorCode().orElse(ErrorCode.COMMAND_VALIDATION_ERROR).name(),
                    componentsResult.getFirstErrorMessage().orElse("Command validation failed")
                );
            }

            ComponentsData components = componentsResult.getValue();

            // ✅ NEW: Validate rule has product.applicability.in node if applicableTo provided
            if (components.applicableToData() != null) {
                boolean isValid = validateRuleHasProductApplicabilityNode(
                    components.ruleId(),
                    components.applicableToData()
                );
                if (!isValid) {
                    return CommandProcessingResult.failure(
                        ErrorCode.RULE_MISSING_APPLICABILITY_NODE.name(),
                        "Rule must contain product.applicability.in condition node when applicableTo is provided"
                    );
                }
            }

            // ✅ FIXED: Create rule assignment with campaign ID
            vn.viettel.vds.promotion.validation.domain.model.Assignment assignment =
                    createRuleAssignment(
                        components.assignRuleData(),
                        components.campaignId(),
                        components.priority(),
                        components.notes()
                    );

            // Convert to JPA entity and save
            AssignmentEntity assignmentEntity = toAssignmentEntity(assignment);
            assignmentEntity = assignmentRepository.save(assignmentEntity);
            assignment.setId(assignmentEntity.getId());

            logger.info("Created assignment for campaign: campaignId={}, ruleId={}, assignmentId={}",
                    components.campaignId(), components.ruleId(), assignment.getId());

            // Process timeframe if provided
            String timeFrameId = null;
            if (components.timeframeData() != null) {
                timeFrameId = processTimeframe(components.ruleId(), components.timeframeData());
            }

            // Deploy rule to validation-engine
            deployRuleToEngine(assignment, components.ruleId());

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
            SettingValidationRuleCommand command,
            SettingValidationRuleCommandPayload payload) {

        // Extract command components
        vn.viettel.vds.promotion.schema.validation.command.RuleAssignment assignRuleData = payload.getAssignRule();
        ApplicabilityScope applicableToData = payload.getApplicableTo();
        TimeFrame timeframeData = payload.getTimeframe();
        Integer priority = payload.getPriority();
        String notes = payload.getNotes() != null ? payload.getNotes().toString() : null;

        // Validate assignRule
        if (assignRuleData == null) {
            return Result.failure(ErrorCode.MISSING_ASSIGN_RULE, "assignRule is required in payload");
        }

        // Get campaign ID from command.subject
        String campaignId = getCampaignIdFromCommand(command);
        if (campaignId == null || campaignId.isEmpty()) {
            return Result.failure(ErrorCode.MISSING_CAMPAIGN_ID, "Campaign ID is required in command.subject");
        }

        // Validate rule exists
        String ruleId = assignRuleData.getRuleId().toString();
        if (ruleId == null || !validationRuleRepository.existsById(ruleId)) {
            return Result.failure(ErrorCode.RULE_NOT_FOUND, "Validation rule not found: " + ruleId);
        }

        // Return validated components
        return Result.success(new ComponentsData(
            assignRuleData,
            applicableToData,
            timeframeData,
            priority,
            notes,
            campaignId,
            ruleId
        ));
    }

    /**
     * ✅ NEW: Extract campaign ID from command.subject
     */
    private String getCampaignIdFromCommand(SettingValidationRuleCommand command) {
        if (command == null || command.getSubject() == null) {
            logger.error("Command or command.subject is null");
            return null;
        }

        String subject = command.getSubject().toString();

        if (subject == null || subject.trim().isEmpty()) {
            logger.error("Campaign ID (command.subject) is null or empty");
            return null;
        }

        logger.debug("Extracted campaign ID from command.subject: {}", subject);
        return subject.trim();
    }

    /**
     * ✅ NEW: Validate that rule contains product.applicability.in node
     * This ensures consistency between rule definition and applicableTo data
     */
    private boolean validateRuleHasProductApplicabilityNode(
            String ruleId,
            ApplicabilityScope applicableToData) {

        try {
            var ruleOpt = validationRuleRepository.findById(ruleId);
            if (ruleOpt.isEmpty()) {
                logger.error("Rule not found during validation: ruleId={}", ruleId);
                return false;
            }

            var rule = ruleOpt.get();

            // Check if rule has nodes
            if (rule.getNodes() == null || rule.getNodes().isEmpty()) {
                logger.warn("Rule has no nodes: ruleId={}", ruleId);
                return false;
            }

            // Search for product.applicability.in operator in rule tree
            boolean hasApplicabilityNode = findProductApplicabilityNode(rule.getNodes());

            if (!hasApplicabilityNode) {
                logger.error("Rule does not contain product.applicability.in node: ruleId={}", ruleId);
                return false;
            }

            logger.info("Validated rule contains product.applicability.in node: ruleId={}", ruleId);
            return true;

        } catch (Exception e) {
            logger.error("Error validating rule applicability node: ruleId={}", ruleId, e);
            return false;
        }
    }

    /**
     * ✅ NEW: Recursively search for product.applicability.in node in rule tree
     * Works with ValidationRuleEntity JPA entity nodes
     */
    private boolean findProductApplicabilityNode(List<RuleNodeEntity> nodes) {

        if (nodes == null || nodes.isEmpty()) {
            return false;
        }

        // Search through all nodes for product.applicability.in operator
        for (RuleNodeEntity node : nodes) {
            // Check if this is a COND node with product.applicability.in operator
            if ("COND".equals(node.getType()) &&
                    "product.applicability.in".equals(node.getOperatorName())) {
                return true;
            }
        }

        return false;
    }

    /**
     * ✅ FIXED: Create RuleAssignment entity with campaign ID
     */
    private vn.viettel.vds.promotion.validation.domain.model.Assignment createRuleAssignment(
            vn.viettel.vds.promotion.schema.validation.command.RuleAssignment assignRuleData,
            String campaignId,  // ✅ Changed from ApplicabilityScope to campaignId
            Integer priority,
            String notes) {

        String ruleId = assignRuleData.getRuleId().toString();
        String assignmentId = assignRuleData.getAssignmentId() != null ?
                assignRuleData.getAssignmentId().toString() : null;
        Boolean active = assignRuleData.getActive();
        Integer trafficPercent = assignRuleData.getTrafficPercent();

        // Generate assignment ID if not provided
        if (assignmentId == null) {
            assignmentId = IdGenerator.generateId();
        }

        // ✅ FIXED: Create Subject with campaign ID
        vn.viettel.vds.promotion.validation.domain.model.Assignment.Subject subject =
                new vn.viettel.vds.promotion.validation.domain.model.Assignment.Subject();
        subject.setType("campaign");      // ✅ Always "campaign"
        subject.setKey(campaignId);       // ✅ Campaign ID from command.subject

        // Create and configure assignment
        vn.viettel.vds.promotion.validation.domain.model.Assignment assignment =
                new vn.viettel.vds.promotion.validation.domain.model.Assignment();
        assignment.setId(assignmentId);
        assignment.setRuleId(ruleId);
        assignment.setSubject(subject);
        assignment.setAssignmentVersion(1);
        assignment.setActive(active);
        assignment.setTrafficPercent(trafficPercent);
        assignment.setCreatedAt(Instant.now());
        assignment.setUpdatedAt(Instant.now());

        logger.debug("Created assignment entity: assignmentId={}, campaignId={}, ruleId={}",
                assignmentId, campaignId, ruleId);

        return assignment;
    }

    /**
     * Process timeframe configuration
     */
    private String processTimeframe(String ruleId, TimeFrame timeframeData) {
        try {
            // Extract timeframe components
            String timeFrameId = timeframeData.getTimeFrameId() != null ?
                    timeframeData.getTimeFrameId().toString() : null;
            String mode = timeframeData.getMode().toString();

            // Generate timeFrame ID if not provided
            if (timeFrameId == null) {
                timeFrameId = IdGenerator.generateId();
            }

            // Get validation rule entity
            ValidationRuleEntity validationRule = validationRuleRepository.findById(ruleId)
                    .orElseThrow(() -> new RuntimeException("Validation rule not found: " + ruleId));

            // Create RuleTimeFrame entity
            RuleTimeFrameEntity ruleTimeFrame = new RuleTimeFrameEntity();
            ruleTimeFrame.setId(IdGenerator.generateId());
            ruleTimeFrame.setValidationRule(validationRule); // Link to validation rule
            ruleTimeFrame.setTimeFrameId(timeFrameId);
            ruleTimeFrame.setMode(mode);

            ruleTimeFrameRepository.save(ruleTimeFrame);

            // TODO: Process complex timeframe logic (validity hours, days of week, etc.)
            // This would require additional entities or JSON storage in RuleTimeFrame

            logger.debug("Created timeframe: ruleId={}, timeFrameId={}", ruleId, timeFrameId);
            return timeFrameId;

        } catch (Exception e) {
            logger.error("Error processing timeframe: ruleId={}", ruleId, e);
            throw new RuntimeException("Failed to process timeframe", e);
        }
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
    private void publishErrorEvent(String commandId, String errorCode, String errorMessage) {
        try {
            eventPublisher.publishErrorEvent(commandId, errorCode, errorMessage);
        } catch (Exception e) {
            logger.error("Failed to publish error event: commandId={}", commandId, e);
        }
    }

    /**
     * Deploy rule to validation-engine after successful assignment creation
     */
    private void deployRuleToEngine(vn.viettel.vds.promotion.validation.domain.model.Assignment assignment, String ruleId) {
        try {
            // Get the validation rule details
            var validationRule = validationRuleRepository.findById(ruleId);
            if (validationRule.isEmpty()) {
                logger.warn("Validation rule not found for deployment: ruleId={}", ruleId);
                return;
            }

            var rule = validationRule.get();

            // Only deploy if assignment is active
            if (assignment.getActive() != null && assignment.getActive()) {
                logger.info("Deploying rule to validation-engine: ruleId={}, assignmentId={}",
                        ruleId, assignment.getId());

                boolean deployed = validationEngineClient.deployRule(rule);
                if (deployed) {
                    logger.info("Successfully deployed rule to validation-engine: ruleId={}, assignmentId={}",
                            ruleId, assignment.getId());
                } else {
                    logger.error("Failed to deploy rule to validation-engine: ruleId={}, assignmentId={}",
                            ruleId, assignment.getId());
                }
            } else {
                logger.info("Skipping rule deployment - assignment is not active: ruleId={}, assignmentId={}",
                        ruleId, assignment.getId());
            }

        } catch (Exception e) {
            logger.error("Error deploying rule to validation-engine: ruleId={}, assignmentId={}",
                    ruleId, assignment.getId(), e);
            // Don't fail the entire command processing for deployment issues
        }
    }

    /**
     * Handle dead letter commands (for monitoring/alerting)
     */
    public void handleDeadLetterCommand(SettingValidationRuleCommand command) {
        String commandId = command.getId().toString();
        logger.error("Processing dead letter command: commandId={}", commandId);

        // Could implement:
        // - Store in dead letter table for manual processing
        // - Send alerts to monitoring system
        // - Log detailed information for debugging

        try {
            // Publish dead letter event for monitoring
            eventPublisher.publishDeadLetterEvent(commandId, command);
        } catch (Exception e) {
            logger.error("Failed to publish dead letter event: commandId={}", commandId, e);
        }
    }

    /**
     * Convert domain Assignment to JPA AssignmentEntity
     */
    private AssignmentEntity toAssignmentEntity(vn.viettel.vds.promotion.validation.domain.model.Assignment assignment) {
        AssignmentEntity entity = new AssignmentEntity();
        entity.setId(assignment.getId());
        entity.setTenantId(assignment.getTenantId());
        entity.setRuleId(assignment.getRuleId());
        entity.setRuleVersionPinned(assignment.getRuleVersionPinned());

        // Convert Subject
        if (assignment.getSubject() != null) {
            AssignmentEntity.SubjectEmbeddable subject = new AssignmentEntity.SubjectEmbeddable();
            subject.setType(assignment.getSubject().getType());
            subject.setKey(assignment.getSubject().getKey());
            entity.setSubject(subject);
        }

        entity.setAssignmentVersion(assignment.getAssignmentVersion());
        entity.setActive(assignment.getActive());
        entity.setValidFrom(assignment.getValidFrom());
        entity.setValidTo(assignment.getValidTo());
        entity.setTrafficPercent(assignment.getTrafficPercent());

        // Convert StickyKeyStrategy enum
        if (assignment.getStickyKeyStrategy() != null) {
            entity.setStickyKeyStrategy(
                AssignmentEntity.StickyKeyStrategy.valueOf(assignment.getStickyKeyStrategy().name())
            );
        }

        entity.setCreatedAt(assignment.getCreatedAt());
        entity.setUpdatedAt(assignment.getUpdatedAt());

        return entity;
    }

    /**
     * Record to hold validated command components
     */
    private record ComponentsData(
        vn.viettel.vds.promotion.schema.validation.command.RuleAssignment assignRuleData,
        ApplicabilityScope applicableToData,
        TimeFrame timeframeData,
        Integer priority,
        String notes,
        String campaignId,
        String ruleId
    ) {}

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

        private CommandProcessingResult(boolean success, String errorCode, String errorMessage,
                                        vn.viettel.vds.promotion.validation.domain.model.Assignment assignment, ApplicabilityScope applicabilityData,
                                        String timeFrameId, TimeFrame timeframeData) {
            this.success = success;
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
            this.assignment = assignment;
            this.applicabilityData = applicabilityData;
            this.timeFrameId = timeFrameId;
            this.timeframeData = timeframeData;
        }

        public static CommandProcessingResult success(vn.viettel.vds.promotion.validation.domain.model.Assignment assignment,
                                                      ApplicabilityScope applicabilityData,
                                                      String timeFrameId,
                                                      TimeFrame timeframeData) {
            return new CommandProcessingResult(true, null, null, assignment, applicabilityData, timeFrameId, timeframeData);
        }

        public static CommandProcessingResult failure(String errorCode, String errorMessage) {
            return new CommandProcessingResult(false, errorCode, errorMessage, null, null, null, null);
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
    }
}