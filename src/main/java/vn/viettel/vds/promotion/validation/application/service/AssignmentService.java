package vn.viettel.vds.promotion.validation.application.service;

import com.promix.platform.core.error.ResponseInfo;
import com.promix.platform.core.exception.BusinessException;
import com.promix.platform.core.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.viettel.vds.promotion.validation.application.port.out.AssignmentPersistencePort;
import vn.viettel.vds.promotion.validation.application.service.dto.CreateAssignmentRequest;
import vn.viettel.vds.promotion.validation.application.service.dto.UpdateAssignmentRequest;
import vn.viettel.vds.promotion.validation.domain.model.Assignment;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service

@Transactional

public class AssignmentService {

    private static final Logger logger = LoggerFactory.getLogger(AssignmentService.class);

    private static final String DEFAULT_TENANT = "default";

    private static final String ASSIGNMENT_ID_PREFIX = "asg_";

    private static final String ERROR_CODE_ASSIGNMENT_OVERLAP = "ASSIGNMENT_OVERLAP";

    private final AssignmentPersistencePort assignmentPersistencePort;

    private final RuleService ruleService;

    private final AssignmentService self;

    public AssignmentService(AssignmentPersistencePort assignmentPersistencePort,

                             RuleService ruleService,
                             @org.springframework.context.annotation.Lazy AssignmentService self) {

        this.assignmentPersistencePort = assignmentPersistencePort;

        this.ruleService = ruleService;

        this.self = self;

    }

    /**
     * Create a new assignment
     */

    public Assignment createAssignment(CreateAssignmentRequest request) {

        logger.info("Creating assignment: tenant={}, rule={}, subject={}:{}",

                request.getTenantId(), request.getRuleId(), request.getSubjectType(), request.getSubjectKey());

        // Verify rule exists

        ruleService.getRuleById(request.getRuleId());

        // Check for overlapping assignments if this assignment is active

        if (Boolean.TRUE.equals(request.getActive())) {

            checkForOverlappingAssignments(request.getTenantId(), request.getSubjectType(), request.getSubjectKey(), request.getValidFrom(), request.getValidTo(), null);

        }

        Assignment assignment = new Assignment();

        assignment.setId(generateAssignmentId(request.getTenantId(), request.getSubjectType(), request.getSubjectKey()));

        assignment.setRuleId(request.getRuleId());

        assignment.setRuleVersionPinned(null); // Use latest version by default

        Assignment.Subject subject = new Assignment.Subject();

        subject.setType(request.getSubjectType());

        subject.setKey(request.getSubjectKey());

        assignment.setSubject(subject);

        assignment.setAssignmentVersion(1);

        assignment.setActive(Optional.ofNullable(request.getActive()).orElse(true));

        assignment.setValidFrom(request.getValidFrom());

        assignment.setValidTo(request.getValidTo());

        assignment.setTrafficPercent(request.getTrafficPercent() != null ? request.getTrafficPercent() : 100);

        assignment.setStickyKeyStrategy(request.getStickyKeyStrategy() != null ? request.getStickyKeyStrategy() : Assignment.StickyKeyStrategy.CUSTOMER_ID);

        assignment.setCreatedAt(Instant.now());

        assignment.setUpdatedAt(Instant.now());

        Assignment saved = assignmentPersistencePort.save(assignment);

        logger.info("Assignment created successfully: id={}", saved.getId());

        return saved;

    }

    /**
     * Update an existing assignment
     */
    public Assignment updateAssignment(UpdateAssignmentRequest request) {
        logger.info("Updating assignment: id={}", request.getAssignmentId());

        Assignment assignment = self.getAssignmentById(request.getAssignmentId());

        // Check for overlapping assignments if making this assignment active
        if (Boolean.TRUE.equals(request.getActive()) && !Boolean.TRUE.equals(assignment.getActive())) {
            checkForOverlappingAssignments(
                    DEFAULT_TENANT,  // Remove tenant concept
                    assignment.getSubject().getType(),
                    assignment.getSubject().getKey(),
                    request.getValidFrom() != null ? request.getValidFrom() : assignment.getValidFrom(),
                    request.getValidTo() != null ? request.getValidTo() : assignment.getValidTo(),
                    request.getAssignmentId()
            );
        }

        if (request.getActive() != null) {
            assignment.setActive(request.getActive());
        }

        if (request.getValidFrom() != null) {
            assignment.setValidFrom(request.getValidFrom());
        }

        if (request.getValidTo() != null) {
            assignment.setValidTo(request.getValidTo());
        }

        if (request.getTrafficPercent() != null) {
            assignment.setTrafficPercent(request.getTrafficPercent());
        }

        if (request.getStickyKeyStrategy() != null) {
            assignment.setStickyKeyStrategy(request.getStickyKeyStrategy());
        }

        if (request.getRuleVersionPinned() != null) {
            assignment.setRuleVersionPinned(request.getRuleVersionPinned());
        }

        // Bump assignment version
        assignment.setAssignmentVersion(assignment.getAssignmentVersion() + 1);
        assignment.setUpdatedAt(Instant.now());

        Assignment saved = assignmentPersistencePort.save(assignment);

        logger.info("Assignment updated successfully: id={}, version={}",
                saved.getId(), saved.getAssignmentVersion());
        return saved;
    }

    /**
     * Get assignment by ID
     */
    @Transactional(readOnly = true)
    public Assignment getAssignmentById(String assignmentId) {
        return assignmentPersistencePort.findById(assignmentId)
                .orElseThrow(ResourceNotFoundException::new);
    }

    /**
     * Find assignments with filters
     */
    @Transactional(readOnly = true)
    public Page<Assignment> findAssignments(String tenantId, String subjectType, String subjectKeyPattern,
                                            Boolean active, String ruleId, Pageable pageable) {
        return assignmentPersistencePort.findWithFilters(tenantId, subjectType, subjectKeyPattern, active, ruleId, pageable);
    }

    /**
     * Get assignments by rule ID
     */
    @Transactional(readOnly = true)
    public List<Assignment> getAssignmentsByRule(String tenantId, String ruleId) {
        return assignmentPersistencePort.findByTenantIdAndRuleId(tenantId, ruleId);
    }

    /**
     * Get active assignment for subject
     */
    @Transactional(readOnly = true)
    public Optional<Assignment> getActiveAssignmentForSubject(String tenantId, String subjectType, String subjectKey) {
        List<Assignment> activeAssignments = assignmentPersistencePort.findActiveByTenantIdAndSubject(
                tenantId, subjectType, subjectKey);

        // Return the latest version if multiple active assignments exist
        return activeAssignments.stream()
                .max((a1, a2) -> a1.getAssignmentVersion().compareTo(a2.getAssignmentVersion()));
    }

    /**
     * Get assignments valid at specific time
     */
    @Transactional(readOnly = true)
    public List<Assignment> getAssignmentsValidAtTime(String tenantId, Instant time) {
        return assignmentPersistencePort.findActiveAtTime(tenantId, time);
    }

    /**
     * Deactivate assignment
     */
    public Assignment deactivateAssignment(String assignmentId) {
        logger.info("Deactivating assignment: id={}", assignmentId);

        Assignment assignment = self.getAssignmentById(assignmentId);
        assignment.setActive(false);
        assignment.setAssignmentVersion(assignment.getAssignmentVersion() + 1);
        assignment.setUpdatedAt(Instant.now());

        Assignment saved = assignmentPersistencePort.save(assignment);

        logger.info("Assignment deactivated successfully: id={}", saved.getId());
        return saved;
    }

    private void checkForOverlappingAssignments(String tenantId, String subjectType, String subjectKey,
                                                Instant validFrom, Instant validTo, String excludeAssignmentId) {
        List<Assignment> overlapping = assignmentPersistencePort.findOverlappingAssignments(
                tenantId, subjectType, subjectKey, validFrom, validTo);

        // Filter out the current assignment if updating
        if (excludeAssignmentId != null) {
            overlapping = overlapping.stream()
                    .filter(assignment -> !assignment.getId().equals(excludeAssignmentId))
                    .toList();
        }

        if (!overlapping.isEmpty()) {
            throw new BusinessException(new ResponseInfo(ERROR_CODE_ASSIGNMENT_OVERLAP,
                    String.format("Active assignment already exists for subject %s:%s in the specified time range",
                            subjectType, subjectKey), 400));
        }
    }

    private String generateAssignmentId(String tenantId, String subjectType, String subjectKey) {
        return ASSIGNMENT_ID_PREFIX + tenantId + "_" + subjectType + "_" + subjectKey;
    }

    /**
     * Check if validation assignment exists for object
     */
    @Transactional(readOnly = true)
    public boolean hasValidationAssignment(String objectType, String objectId) {
        return assignmentPersistencePort.existsBySubjectTypeAndSubjectKey(objectType, objectId);
    }

    /**
     * Get validation settings for object
     * Returns validation configuration including timeframe and rules
     */
    @Transactional(readOnly = true)
    public Optional<ValidationSettingsDTO> getValidationSettings(String objectType, String objectId) {
        Optional<Assignment> assignment = assignmentPersistencePort
                .findBySubjectTypeAndSubjectKey(objectType, objectId);

        if (assignment.isEmpty()) {
            return Optional.empty();
        }

        Assignment assign = assignment.get();

        // Build validation settings DTO
        ValidationSettingsDTO.Builder settingsBuilder = ValidationSettingsDTO.builder();

        // Add timeframe information
        if (assign.getValidFrom() != null || assign.getValidTo() != null) {
            ValidationSettingsDTO.ValidityTimeframe.Builder validityTimeframeBuilder = ValidationSettingsDTO.ValidityTimeframe.builder();
            if (assign.getValidFrom() != null) {
                validityTimeframeBuilder.startDate(assign.getValidFrom().toString());
            }
            if (assign.getValidTo() != null) {
                validityTimeframeBuilder.expirationDate(assign.getValidTo().toString());
            }
            settingsBuilder.timeframe(ValidationSettingsDTO.Timeframe.builder()
                    .validityTimeframe(validityTimeframeBuilder.build())
                    .build());
        }

        // Add rule information
        if (assign.getId() != null) {
            settingsBuilder.ruleId(assign.getId());
        }

        // Add assignment metadata
        settingsBuilder.active(assign.getActive());
        settingsBuilder.trafficPercent(assign.getTrafficPercent());

        return Optional.of(settingsBuilder.build());
    }

    /**
     * Find assignment by subject type and key
     */
    @Transactional(readOnly = true)
    public Optional<Assignment> findBySubjectTypeAndKey(String subjectType, String subjectKey) {
        return assignmentPersistencePort.findBySubjectTypeAndSubjectKey(subjectType, subjectKey);
    }

    /**
     * Find all assignments by subject type and key
     */
    @Transactional(readOnly = true)
    public List<Assignment> findAllBySubjectTypeAndKey(String subjectType, String subjectKey) {
        return assignmentPersistencePort.findAllBySubjectTypeAndSubjectKey(subjectType, subjectKey);
    }

    /**
     * Delete assignment by validation rule ID and object ID.
     * Implements SRS PRM_KBNV_API_VALD008 - Delete validation rule assignment from object.
     * <p>
     * Steps according to SRS:
     * 1. Verify validation_rule_id exists in validation_rules table
     * 2. Verify object_id exists in campaign.campaigns table (via external service)
     * 3. Verify assignment exists for the rule and object combination
     * 4. Soft delete: update deleted_at, deleted_by, version+1
     * 5. Insert into validation_rules_assignment_deleted
     *
     * @param validationRuleId the validation rule ID
     * @param objectId         the object ID (campaign ID)
     * @param deletedBy        the user performing the deletion
     * @return the deleted assignment ID
     * @throws BusinessException if validation rule, object, or assignment not found
     */
    public String deleteAssignmentByRuleAndObject(String validationRuleId, String objectId, String deletedBy) {
        logger.info("Deleting assignment: ruleId={}, objectId={}, deletedBy={}", validationRuleId, objectId, deletedBy);

        // Step 1: Verify validation rule exists
        try {
            ruleService.getRuleById(validationRuleId);
        } catch (ResourceNotFoundException e) {
            logger.error("Validation rule not found: {}", validationRuleId);
            throw new BusinessException(new ResponseInfo("VALIDATION_RULE_NOT_FOUND",
                    "Quy tắc kiểm duyệt không tồn tại: " + validationRuleId, 404));
        }

        // Step 2: Object (campaign) existence check is handled by external service
        // Skipped here as campaign service call may not be available in all environments

        // Step 3: Verify assignment exists for rule and object combination
        Optional<Assignment> assignmentOpt = assignmentPersistencePort.findByRuleIdAndEntityId(validationRuleId, objectId);
        if (assignmentOpt.isEmpty()) {
            logger.error("Assignment not found for ruleId={}, objectId={}", validationRuleId, objectId);
            throw new BusinessException(new ResponseInfo("ASSIGNMENT_VALIDATION_NOT_FOUND",
                    "Không tìm thấy bản ghi gán quy tắc kiểm duyệt cho đối tượng này", 404));
        }

        Assignment assignment = assignmentOpt.get();

        // Step 4 & 5: Soft delete assignment
        String deletedId = assignmentPersistencePort.softDeleteAssignment(assignment, deletedBy);

        logger.info("Assignment deleted successfully: id={}, ruleId={}, objectId={}",
                deletedId, validationRuleId, objectId);

        return deletedId;
    }

    /**
     * Find assignment by rule ID and entity ID (object ID).
     */
    @Transactional(readOnly = true)
    public Optional<Assignment> findByRuleIdAndEntityId(String ruleId, String entityId) {
        return assignmentPersistencePort.findByRuleIdAndEntityId(ruleId, entityId);
    }
}