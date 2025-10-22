package vn.viettel.vds.promotion.validation.application.service;

import com.promix.platform.core.util.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.viettel.vds.promotion.validation.command.SettingValidationRuleCommand;
import vn.viettel.vds.promotion.validation.command.SettingValidationRuleCommand.ApplicabilityScope;
import vn.viettel.vds.promotion.validation.command.SettingValidationRuleCommand.SettingValidationRuleCommandPayload;
import vn.viettel.vds.promotion.validation.command.SettingValidationRuleCommand.TimeFrame;
import vn.viettel.vds.promotion.validation.adapter.out.integration.ValidationEngineDeploymentService;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.AssignmentEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.RuleNodeEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.RuleTimeFrameEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.ValidationRuleEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.AssignmentJpaRepository;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.RuleTimeFrameJpaRepository;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.ValidationRuleJpaRepository;
import vn.viettel.vds.promotion.validation.domain.common.ErrorCode;
import vn.viettel.vds.promotion.validation.domain.exception.TimeframeProcessingException;
import vn.viettel.vds.promotion.validation.domain.common.Result;

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
    @SuppressWarnings("unused") // Reserved for future use
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
        String commandId = command.getId();

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

            // ✅ ENHANCED: Auto-create product.applicability.in node if applicableTo provided
            if (components.applicableToData() != null) {
                boolean hasNode = validateRuleHasProductApplicabilityNode(
                        components.ruleId()
                );
                if (!hasNode) {
                    if (logger.isInfoEnabled()) {
                        logger.info("Rule does not have product.applicability.in node. Auto-creating it for ruleId={}", components.ruleId());
                    }
                    // Auto-create the node instead of failing
                    boolean created = createProductApplicabilityNode(
                            components.ruleId(),
                            components.applicableToData()
                    );
                    if (!created) {
                        return CommandProcessingResult.failure(
                                ErrorCode.RULE_MISSING_APPLICABILITY_NODE.name(),
                                "Failed to create product.applicability.in node for rule"
                        );
                    }
                }
            }

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
     * ✅ NEW: Validate that rule contains product.applicability.in node
     * This ensures consistency between rule definition and applicableTo data
     */
    private boolean validateRuleHasProductApplicabilityNode(
            String ruleId) {

        try {
            var ruleOpt = validationRuleRepository.findById(ruleId);
            if (ruleOpt.isEmpty()) {
                handleMissingRule(ruleId);
                return false;
            }

            var rule = ruleOpt.get();

            // Check if rule has nodes
            if (rule.getNodes() == null || rule.getNodes().isEmpty()) {
                handleEmptyRuleNodes(ruleId);
                return false;
            }

            // Search for product.applicability.in operator in rule tree
            boolean hasApplicabilityNode = findProductApplicabilityNode(rule.getNodes());

            if (!hasApplicabilityNode) {
                handleMissingApplicabilityNode(ruleId);
                return false;
            }

            logger.info("Validated rule contains product.applicability.in node: ruleId={}", ruleId);
            return true;

        } catch (Exception e) {
            handleValidationException(e, ruleId);
            return false;
        }
    }

    private void handleMissingRule(String ruleId) {
        logger.error("Rule not found during validation: ruleId={}", ruleId);
    }

    private void handleEmptyRuleNodes(String ruleId) {
        logger.warn("Rule has no nodes: ruleId={}", ruleId);
    }

    private void handleMissingApplicabilityNode(String ruleId) {
        logger.error("Rule does not contain product.applicability.in node: ruleId={}", ruleId);
    }

    private void handleValidationException(Exception e, String ruleId) {
        logger.error("Error validating rule applicability node: ruleId={}", ruleId, e);
    }

    /**
     * ✅ NEW: Create product.applicability.in condition node in rule
     * Adds the node to rule's nodes if it doesn't exist
     */
    private boolean createProductApplicabilityNode(
            String ruleId,
            ApplicabilityScope applicableToData) {
        try {
            var ruleOpt = validationRuleRepository.findById(ruleId);
            if (ruleOpt.isEmpty()) {
                handleMissingRuleForCreation(ruleId);
                return false;
            }

            ValidationRuleEntity rule = ruleOpt.get();

            // Create new COND node for product.applicability.in
            RuleNodeEntity productNode = createProductApplicabilityNodeEntity(rule);

            // Build params map for RuleNodeEntity from applicableToData
            java.util.Map<String, Object> params = buildApplicabilityParams(applicableToData);

            productNode.setParams(params);
            productNode.setCreatedAt(Instant.now());
            productNode.setUpdatedAt(Instant.now());

            // Add node to rule
            if (rule.getNodes() == null) {
                rule.setNodes(new java.util.ArrayList<>());
            }
            rule.getNodes().add(productNode);

            // Save rule with new node
            validationRuleRepository.save(rule);

            logger.info("Created product.applicability.in node for rule: ruleId={}, nodeId={}, params={}",
                    ruleId, productNode.getId(), params);
            return true;

        } catch (Exception e) {
            handleCreationException(e, ruleId);
            return false;
        }
    }

    private void handleMissingRuleForCreation(String ruleId) {
        logger.error("Rule not found when creating applicability node: ruleId={}", ruleId);
    }

    private RuleNodeEntity createProductApplicabilityNodeEntity(ValidationRuleEntity rule) {
        RuleNodeEntity productNode = new RuleNodeEntity();
        String generatedId = IdGenerator.generateId();
        productNode.setId(generatedId);
        productNode.setNodeId(generatedId);
        productNode.setType("COND");
        productNode.setOperatorName("product.applicability.in");
        productNode.setValidationRule(rule);
        return productNode;
    }

    private java.util.Map<String, Object> buildApplicabilityParams(ApplicabilityScope applicableToData) {
        java.util.Map<String, Object> params = new java.util.HashMap<>();

        if (Boolean.TRUE.equals(applicableToData.getIncludedAll())) {
            params.put("includedAll", true);
        } else {
            addIncludedItems(params, applicableToData);
            addExcludedItems(params, applicableToData);
        }
        return params;
    }

    private void addIncludedItems(java.util.Map<String, Object> params, ApplicabilityScope applicableToData) {
        if (applicableToData.getIncluded() != null && !applicableToData.getIncluded().isEmpty()) {
            // Extract IDs from ApplicabilityRule objects
            List<String> includedIds = applicableToData.getIncluded().stream()
                    .map(SettingValidationRuleCommand.ApplicabilityRule::getId)
                    .toList();
            params.put("included", includedIds);
        }
    }

    private void addExcludedItems(java.util.Map<String, Object> params, ApplicabilityScope applicableToData) {
        if (applicableToData.getExcluded() != null && !applicableToData.getExcluded().isEmpty()) {
            // Extract IDs from ApplicabilityRule objects
            List<String> excludedIds = applicableToData.getExcluded().stream()
                    .map(SettingValidationRuleCommand.ApplicabilityRule::getId)
                    .toList();
            params.put("excluded", excludedIds);
        }
    }

    private void handleCreationException(Exception e, String ruleId) {
        logger.error("Error creating product applicability node: ruleId={}", ruleId, e);
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
     * Process timeframe configuration
     */
    private String processTimeframe(String ruleId, TimeFrame timeframeData) {
        try {
            // Extract timeframe components
            String timeFrameId = timeframeData.getTimeFrameId();
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

            // Complex timeframe logic (validity hours, days of week, etc.) is stored in mode field
            // Additional entities or JSON storage in RuleTimeFrame can be added when needed

            logger.debug("Created timeframe: ruleId={}, timeFrameId={}", ruleId, timeFrameId);
            return timeFrameId;

        } catch (Exception e) {
            throw new TimeframeProcessingException("Failed to process timeframe for ruleId=" + ruleId + " due to: " + e.getMessage(), e);
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
            // Assignment result handled via publishSuccessEvent

            // Only deploy if assignment is active
            if (assignment.getActive() != null && assignment.getActive()) {
                logger.info("Rule assignment created successfully: ruleId={}, assignmentId={}",
                        ruleId, assignment.getId());
                // Note: deployRule() is deprecated and does nothing.
                // Actual rule deployment is handled by RulePublishingService.publishRule()
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
        String commandId = command.getId();
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
    }
}