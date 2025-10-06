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
import vn.viettel.vds.promotion.validation.domain.entity.Assignment;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class AssignmentService {

    private static final Logger logger = LoggerFactory.getLogger(AssignmentService.class);

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
    public Assignment createAssignment(String tenantId, String ruleId, String subjectType, String subjectKey,
                                       Boolean active, Instant validFrom, Instant validTo,
                                       Integer trafficPercent, Assignment.StickyKeyStrategy stickyKeyStrategy,
                                       String createdBy) {
        logger.info("Creating assignment: tenant={}, rule={}, subject={}:{}",
                tenantId, ruleId, subjectType, subjectKey);

        // Verify rule exists
        ruleService.getRuleById(ruleId);

        // Check for overlapping assignments if this assignment is active
        if (Boolean.TRUE.equals(active)) {
            checkForOverlappingAssignments(tenantId, subjectType, subjectKey, validFrom, validTo, null);
        }

        Assignment assignment = new Assignment();
        assignment.setId(generateAssignmentId(tenantId, subjectType, subjectKey));
        assignment.setTenantId(tenantId);
        assignment.setRuleId(ruleId);
        assignment.setRuleVersionPinned(null); // Use latest version by default

        Assignment.Subject subject = new Assignment.Subject();
        subject.setType(subjectType);
        subject.setKey(subjectKey);
        assignment.setSubject(subject);

        assignment.setAssignmentVersion(1);
        assignment.setActive(active != null ? active : true);
        assignment.setValidFrom(validFrom);
        assignment.setValidTo(validTo);
        assignment.setTrafficPercent(trafficPercent != null ? trafficPercent : 100);
        assignment.setStickyKeyStrategy(stickyKeyStrategy != null ? stickyKeyStrategy : Assignment.StickyKeyStrategy.CUSTOMER_ID);
        assignment.setCreatedAt(Instant.now());
        assignment.setUpdatedAt(Instant.now());

        Assignment saved = assignmentPersistencePort.save(assignment);

        // Log audit event
        auditService.logAssignmentUpdated(tenantId, saved.getId(), createdBy,
                java.util.Map.of("action", "create"));

        logger.info("Assignment created successfully: id={}", saved.getId());
        return saved;
    }

    /**
     * Update an existing assignment
     */
    public Assignment updateAssignment(String assignmentId, Boolean active, Instant validFrom, Instant validTo,
                                       Integer trafficPercent, Assignment.StickyKeyStrategy stickyKeyStrategy,
                                       Integer ruleVersionPinned, String updatedBy) {
        logger.info("Updating assignment: id={}", assignmentId);

        Assignment assignment = getAssignmentById(assignmentId);

        // Check for overlapping assignments if making this assignment active
        if (Boolean.TRUE.equals(active) && !Boolean.TRUE.equals(assignment.getActive())) {
            checkForOverlappingAssignments(
                    assignment.getTenantId(),
                    assignment.getSubject().getType(),
                    assignment.getSubject().getKey(),
                    validFrom != null ? validFrom : assignment.getValidFrom(),
                    validTo != null ? validTo : assignment.getValidTo(),
                    assignmentId
            );
        }

        if (active != null) {
            assignment.setActive(active);
        }

        if (validFrom != null) {
            assignment.setValidFrom(validFrom);
        }

        if (validTo != null) {
            assignment.setValidTo(validTo);
        }

        if (trafficPercent != null) {
            assignment.setTrafficPercent(trafficPercent);
        }

        if (stickyKeyStrategy != null) {
            assignment.setStickyKeyStrategy(stickyKeyStrategy);
        }

        if (ruleVersionPinned != null) {
            assignment.setRuleVersionPinned(ruleVersionPinned);
        }

        // Bump assignment version
        assignment.setAssignmentVersion(assignment.getAssignmentVersion() + 1);
        assignment.setUpdatedAt(Instant.now());

        Assignment saved = assignmentPersistencePort.save(assignment);

        // Log audit event
        auditService.logAssignmentUpdated(assignment.getTenantId(), saved.getId(), updatedBy,
                java.util.Map.of("action", "update", "version", saved.getAssignmentVersion()));

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
                .orElseThrow(() -> new ResourceNotFoundException());
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
        auditService.logAssignmentUpdated(assignment.getTenantId(), saved.getId(), updatedBy,
                java.util.Map.of("action", "deactivate"));

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
            throw new BusinessException(new ResponseInfo("ASSIGNMENT_OVERLAP",
                    String.format("Active assignment already exists for subject %s:%s in the specified time range",
                            subjectType, subjectKey), 400));
        }
    }

    private String generateAssignmentId(String tenantId, String subjectType, String subjectKey) {
        return "asg_" + tenantId + "_" + subjectType + "_" + subjectKey;
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
    public java.util.Map<String, Object> getValidationSettings(String objectType, String objectId) {
        Optional<Assignment> assignment = assignmentPersistencePort
                .findBySubjectTypeAndSubjectKey(objectType, objectId);

        if (assignment.isEmpty()) {
            return null;
        }

        Assignment assign = assignment.get();

        // Build validation settings map
        java.util.Map<String, Object> settings = new java.util.HashMap<>();

        // Add timeframe information
        if (assign.getValidFrom() != null || assign.getValidTo() != null) {
            java.util.Map<String, Object> timeframe = new java.util.HashMap<>();
            java.util.Map<String, Object> validityTimeframe = new java.util.HashMap<>();

            if (assign.getValidFrom() != null) {
                validityTimeframe.put("startDate", assign.getValidFrom().toString());
            }
            if (assign.getValidTo() != null) {
                validityTimeframe.put("expirationDate", assign.getValidTo().toString());
            }

            timeframe.put("validityTimeframe", validityTimeframe);
            settings.put("timeframe", timeframe);
        }

        // Add rule information
        if (assign.getRuleId() != null) {
            settings.put("ruleId", assign.getRuleId());
        }

        // Add assignment metadata
        settings.put("active", assign.getActive());
        settings.put("trafficPercent", assign.getTrafficPercent());

        return settings;
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