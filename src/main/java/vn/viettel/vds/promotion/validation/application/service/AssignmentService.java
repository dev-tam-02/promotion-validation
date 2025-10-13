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
import vn.viettel.vds.promotion.validation.domain.model.Assignment;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import vn.viettel.vds.promotion.validation.application.service.dto.CreateAssignmentRequest;
import vn.viettel.vds.promotion.validation.application.service.dto.UpdateAssignmentRequest;

@Service

@Transactional

public class AssignmentService {

    private static final Logger logger = LoggerFactory.getLogger(AssignmentService.class);

    private static final String DEFAULT_TENANT = "default";

    private static final String ASSIGNMENT_ID_PREFIX = "asg_";

    private static final String AUDIT_ACTION_KEY = "action";

    private static final String AUDIT_VERSION_KEY = "version";

    private static final String ERROR_CODE_ASSIGNMENT_OVERLAP = "ASSIGNMENT_OVERLAP";

    private final AssignmentPersistencePort assignmentPersistencePort;

    private final RuleService ruleService;

    private final AuditService auditService;

    public AssignmentService(AssignmentPersistencePort assignmentPersistencePort,

                             RuleService ruleService, AuditService auditService) {

        this.assignmentPersistencePort = assignmentPersistencePort;

        this.ruleService = ruleService;

        this.auditService = auditService;

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

        // Log audit event

        auditService.logAssignmentUpdated(request.getTenantId(), saved.getId(), request.getCreatedBy(),

                java.util.Map.of(AUDIT_ACTION_KEY, AuditEvent.CREATE.getAction()));

        logger.info("Assignment created successfully: id={}", saved.getId());

        return saved;

    }

    /**
     * Update an existing assignment
     */
    public Assignment updateAssignment(UpdateAssignmentRequest request) {
        logger.info("Updating assignment: id={}", request.getAssignmentId());

        Assignment assignment = getAssignmentById(request.getAssignmentId());

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

        // Log audit event
        auditService.logAssignmentUpdated(DEFAULT_TENANT, saved.getId(), request.getUpdatedBy(),
                java.util.Map.of(AUDIT_ACTION_KEY, AuditEvent.UPDATE.getAction(), AUDIT_VERSION_KEY, saved.getAssignmentVersion()));

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
    public Assignment deactivateAssignment(String assignmentId, String updatedBy) {
        logger.info("Deactivating assignment: id={}", assignmentId);

        Assignment assignment = getAssignmentById(assignmentId);
        assignment.setActive(false);
        assignment.setAssignmentVersion(assignment.getAssignmentVersion() + 1);
        assignment.setUpdatedAt(Instant.now());

        Assignment saved = assignmentPersistencePort.save(assignment);

        // Log audit event
        auditService.logAssignmentUpdated(DEFAULT_TENANT, saved.getId(), updatedBy,
                java.util.Map.of(AUDIT_ACTION_KEY, AuditEvent.DEACTIVATE.getAction()));

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
}