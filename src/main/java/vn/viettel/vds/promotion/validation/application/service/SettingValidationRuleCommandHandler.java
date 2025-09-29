package vn.viettel.vds.promotion.validation.application.service;

import com.promix.platform.core.util.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.viettel.vds.promotion.schema.validation.command.ApplicabilityScope;
import vn.viettel.vds.promotion.schema.validation.command.SettingValidationRuleCommand;
import vn.viettel.vds.promotion.schema.validation.command.SettingValidationRuleCommandPayload;
import vn.viettel.vds.promotion.schema.validation.command.TimeFrame;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.entity.RuleTimeFrame;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.repository.AssignmentRepository;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.repository.RuleTimeFrameRepository;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.repository.ValidationRuleRepository;
import vn.viettel.vds.promotion.validation.adapter.out.integration.ValidationEngineDeploymentService;

import java.time.Instant;

/**
 * Service to handle SettingValidationRuleCommand processing
 */
@Service
@Transactional
public class SettingValidationRuleCommandHandler {

    private static final Logger logger = LoggerFactory.getLogger(SettingValidationRuleCommandHandler.class);

    private final AssignmentRepository assignmentRepository;
    private final RuleTimeFrameRepository ruleTimeFrameRepository;
    private final ValidationRuleRepository validationRuleRepository;
    private final SettingValidationRuleEventPublisher eventPublisher;
    private final CommandMappingService mappingService;
    private final ValidationEngineDeploymentService validationEngineClient;

    public SettingValidationRuleCommandHandler(
            AssignmentRepository assignmentRepository,
            RuleTimeFrameRepository ruleTimeFrameRepository,
            ValidationRuleRepository validationRuleRepository,
            SettingValidationRuleEventPublisher eventPublisher,
            CommandMappingService mappingService,
            ValidationEngineDeploymentService validationEngineClient) {
        this.assignmentRepository = assignmentRepository;
        this.ruleTimeFrameRepository = ruleTimeFrameRepository;
        this.validationRuleRepository = validationRuleRepository;
        this.eventPublisher = eventPublisher;
        this.mappingService = mappingService;
        this.validationEngineClient = validationEngineClient;
    }

    /**
     * Handle SettingValidationRuleCommand
     */
    public boolean handleCommand(SettingValidationRuleCommand command) {
        String commandId = command.getId().toString();

        try {
            logger.info("Processing SettingValidationRuleCommand: commandId={}", commandId);

            // Extract command payload
            SettingValidationRuleCommandPayload payload = command.getPayload();
            if (payload == null) {
                logger.error("Command payload is null: commandId={}", commandId);
                publishErrorEvent(commandId, "INVALID_PAYLOAD", "Command payload is missing");
                return false;
            }

            // Process the command
            CommandProcessingResult result = processCommand(commandId, payload);

            // Publish success/failure event
            if (result.isSuccess()) {
                publishSuccessEvent(commandId, result);
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
    private CommandProcessingResult processCommand(String commandId, SettingValidationRuleCommandPayload payload) {
        try {
            // Extract command components
            vn.viettel.vds.promotion.schema.validation.command.RuleAssignment assignRuleData = payload.getAssignRule();
            ApplicabilityScope applicableToData = payload.getApplicableTo();
            TimeFrame timeframeData = payload.getTimeframe();
            Integer priority = payload.getPriority();
            String notes = payload.getNotes() != null ? payload.getNotes().toString() : null;

            if (assignRuleData == null) {
                return CommandProcessingResult.failure("MISSING_ASSIGN_RULE", "assignRule is required");
            }

            // Validate rule exists
            String ruleId = assignRuleData.getRuleId().toString();
            if (ruleId == null || !validationRuleRepository.existsById(ruleId)) {
                return CommandProcessingResult.failure("RULE_NOT_FOUND", "Validation rule not found: " + ruleId);
            }

            // Create rule assignment
            vn.viettel.vds.promotion.validation.domain.entity.Assignment assignment = createRuleAssignment(assignRuleData, applicableToData, priority, notes);
            assignment = assignmentRepository.save(assignment);

            // Process timeframe if provided
            String timeFrameId = null;
            if (timeframeData != null) {
                timeFrameId = processTimeframe(assignment.getId(), timeframeData);
            }

            // Deploy rule to validation-engine
            deployRuleToEngine(assignment, ruleId);

            // Create processing result
            return CommandProcessingResult.success(
                    assignment,
                    applicableToData,
                    timeFrameId,
                    timeframeData
            );

        } catch (Exception e) {
            logger.error("Error processing command components: commandId={}", commandId, e);
            return CommandProcessingResult.failure("PROCESSING_ERROR", e.getMessage());
        }
    }

    /**
     * Create RuleAssignment entity from command data
     */
    private vn.viettel.vds.promotion.validation.domain.entity.Assignment createRuleAssignment(
            vn.viettel.vds.promotion.schema.validation.command.RuleAssignment assignRuleData,
            ApplicabilityScope applicableToData,
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

        // Create Subject from applicableTo
        vn.viettel.vds.promotion.validation.domain.entity.Assignment.Subject subject = mappingService.createSubjectFromApplicableTo(applicableToData);

        // Create and configure assignment
        vn.viettel.vds.promotion.validation.domain.entity.Assignment assignment = new vn.viettel.vds.promotion.validation.domain.entity.Assignment();
        assignment.setId(assignmentId);
        assignment.setRuleId(ruleId);
        assignment.setSubject(subject);
        assignment.setAssignmentVersion(1);
        assignment.setActive(active);
        assignment.setTrafficPercent(trafficPercent);
        assignment.setCreatedAt(Instant.now());
        assignment.setUpdatedAt(Instant.now());

        return assignment;
    }

    /**
     * Process timeframe configuration
     */
    private String processTimeframe(String assignmentId, TimeFrame timeframeData) {
        try {
            // Extract timeframe components
            String timeFrameId = timeframeData.getTimeFrameId() != null ?
                    timeframeData.getTimeFrameId().toString() : null;
            String mode = timeframeData.getMode().toString();

            // Generate timeFrame ID if not provided
            if (timeFrameId == null) {
                timeFrameId = IdGenerator.generateId();
            }

            // Create RuleTimeFrame entity
            RuleTimeFrame ruleTimeFrame = new RuleTimeFrame();
            ruleTimeFrame.setId(IdGenerator.generateId());
            ruleTimeFrame.setRuleId(assignmentId); // Link to assignment
            ruleTimeFrame.setTimeFrameId(timeFrameId);
            ruleTimeFrame.setMode(mode);

            ruleTimeFrameRepository.save(ruleTimeFrame);

            // TODO: Process complex timeframe logic (validity hours, days of week, etc.)
            // This would require additional entities or JSON storage in RuleTimeFrame

            logger.debug("Created timeframe: assignmentId={}, timeFrameId={}", assignmentId, timeFrameId);
            return timeFrameId;

        } catch (Exception e) {
            logger.error("Error processing timeframe: assignmentId={}", assignmentId, e);
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
    private void deployRuleToEngine(vn.viettel.vds.promotion.validation.domain.entity.Assignment assignment, String ruleId) {
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
     * Result wrapper for command processing
     */
    public static class CommandProcessingResult {
        private final boolean success;
        private final String errorCode;
        private final String errorMessage;
        private final vn.viettel.vds.promotion.validation.domain.entity.Assignment assignment;
        private final ApplicabilityScope applicabilityData;
        private final String timeFrameId;
        private final TimeFrame timeframeData;

        private CommandProcessingResult(boolean success, String errorCode, String errorMessage,
                                      vn.viettel.vds.promotion.validation.domain.entity.Assignment assignment, ApplicabilityScope applicabilityData,
                                      String timeFrameId, TimeFrame timeframeData) {
            this.success = success;
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
            this.assignment = assignment;
            this.applicabilityData = applicabilityData;
            this.timeFrameId = timeFrameId;
            this.timeframeData = timeframeData;
        }

        public static CommandProcessingResult success(vn.viettel.vds.promotion.validation.domain.entity.Assignment assignment,
                                                    ApplicabilityScope applicabilityData,
                                                    String timeFrameId,
                                                    TimeFrame timeframeData) {
            return new CommandProcessingResult(true, null, null, assignment, applicabilityData, timeFrameId, timeframeData);
        }

        public static CommandProcessingResult failure(String errorCode, String errorMessage) {
            return new CommandProcessingResult(false, errorCode, errorMessage, null, null, null, null);
        }

        // Getters
        public boolean isSuccess() { return success; }
        public String getErrorCode() { return errorCode; }
        public String getErrorMessage() { return errorMessage; }
        public vn.viettel.vds.promotion.validation.domain.entity.Assignment getAssignment() { return assignment; }
        public ApplicabilityScope getApplicabilityData() { return applicabilityData; }
        public String getTimeFrameId() { return timeFrameId; }
        public TimeFrame getTimeframeData() { return timeframeData; }
    }
}