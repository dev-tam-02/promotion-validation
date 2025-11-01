package vn.viettel.vds.promotion.validation.application.service;

import com.promix.platform.core.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.viettel.vds.promotion.validation.application.port.in.DeleteValidationRuleAssignmentUseCase;
import vn.viettel.vds.promotion.validation.application.port.out.ValidationRuleAssignmentPort;
import vn.viettel.vds.promotion.validation.domain.exception.AssignmentAlreadyDeletedException;
import vn.viettel.vds.promotion.validation.domain.exception.AssignmentNotFoundException;
import vn.viettel.vds.promotion.validation.domain.exception.ValidationRuleNotFoundException;
import vn.viettel.vds.promotion.validation.domain.model.ValidationRuleAssignment;
import vn.viettel.vds.promotion.validation.domain.model.ValidationRuleAssignmentDeleted;

import java.time.OffsetDateTime;

/**
 * Service implementation cho việc xóa validation rule assignment.
 * Implement use case DeleteValidationRuleAssignmentUseCase.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeleteValidationRuleAssignmentService implements DeleteValidationRuleAssignmentUseCase {

    private static final String SYSTEM_USER = "system";

    private final ValidationRuleAssignmentPort assignmentPort;
    private final RuleService ruleService;

    @Override
    @Transactional
    public void deleteAssignment(String validationRuleId, String objectId) {
        String correlationId = IdGenerator.generateId();
        long startTime = System.currentTimeMillis();

        log.info("[{}] Starting delete assignment: validationRuleId={}, objectId={}",
                correlationId, validationRuleId, objectId);

        try {
            // Step 1: Validate input parameters
            validateInput(validationRuleId, objectId);

            // Step 2: Kiểm tra validation rule tồn tại
            checkValidationRuleExists(validationRuleId);

            // Step 3: Kiểm tra assignment tồn tại
            ValidationRuleAssignment assignment = findAssignment(validationRuleId, objectId);

            // Step 4: Thực hiện soft delete
            ValidationRuleAssignment deletedAssignment = performSoftDelete(assignment, SYSTEM_USER);

            // Step 5: Archive vào bảng deleted
            archiveDeletedAssignment(deletedAssignment, SYSTEM_USER);

            long duration = System.currentTimeMillis() - startTime;
            log.info("[{}] Delete assignment completed successfully in {}ms",
                    correlationId, duration);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[{}] Delete assignment failed after {}ms: {}",
                    correlationId, duration, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Validate input parameters
     */
    private void validateInput(String validationRuleId, String objectId) {
        if (validationRuleId == null || validationRuleId.trim().isEmpty()) {
            throw new IllegalArgumentException("VALIDATION_RULE_ID_REQUIRED: validation_rule_id is required");
        }
        if (objectId == null || objectId.trim().isEmpty()) {
            throw new IllegalArgumentException("OBJECT_ID_REQUIRED: object_id is required");
        }

        // Validate UUID format (basic check)
        if (!isValidUuid(validationRuleId)) {
            throw new IllegalArgumentException("VALIDATION_RULE_ID_INVALID: invalid UUID format");
        }
        if (!isValidUuid(objectId)) {
            throw new IllegalArgumentException("OBJECT_ID_INVALID: invalid UUID format");
        }
    }

    /**
     * Kiểm tra validation rule có tồn tại không
     */
    private void checkValidationRuleExists(String validationRuleId) {
        log.debug("Checking if validation rule exists: id={}", validationRuleId);

        // Sử dụng existing RuleService để check rule tồn tại
        try {
            ruleService.getRuleById(validationRuleId);
        } catch (Exception e) {
            log.warn("Validation rule not found: id={}", validationRuleId);
            throw new ValidationRuleNotFoundException(validationRuleId);
        }

        log.debug("Validation rule exists: id={}", validationRuleId);
    }

    /**
     * Tìm assignment
     */
    private ValidationRuleAssignment findAssignment(String validationRuleId, String objectId) {
        log.debug("Finding assignment: validationRuleId={}, objectId={}", validationRuleId, objectId);

        return assignmentPort.findByValidationRuleIdAndObjectId(validationRuleId, objectId)
                .orElseThrow(() -> {
                    log.warn("Assignment not found: validationRuleId={}, objectId={}",
                            validationRuleId, objectId);
                    return new AssignmentNotFoundException(validationRuleId, objectId);
                });
    }

    /**
     * Thực hiện soft delete assignment
     */
    private ValidationRuleAssignment performSoftDelete(
            ValidationRuleAssignment assignment,
            String deletedBy) {

        log.debug("Performing soft delete: assignmentId={}, deletedBy={}", assignment.getId(), deletedBy);

        // Mark as deleted
        ValidationRuleAssignment deletedAssignment = assignment.markAsDeleted(deletedBy);

        // Save (update) with optimistic locking
        try {
            ValidationRuleAssignment saved = assignmentPort.save(deletedAssignment);
            log.info("Soft delete successful: assignmentId={}, version={}",
                    saved.getId(), saved.getVersion());
            return saved;
        } catch (OptimisticLockingFailureException e) {
            log.warn("Optimistic locking failure - assignment already deleted: id={}",
                    assignment.getId());
            throw new AssignmentAlreadyDeletedException(assignment.getId());
        }
    }

    /**
     * Archive deleted assignment vào bảng deleted
     */
    private void archiveDeletedAssignment(ValidationRuleAssignment assignment, String deletedBy) {
        log.debug("Archiving deleted assignment: assignmentId={}", assignment.getId());

        String archiveId = IdGenerator.generateId();
        ValidationRuleAssignmentDeleted deleted = ValidationRuleAssignmentDeleted.fromAssignment(
                assignment,
                archiveId,
                deletedBy,
                null // reason - có thể thêm sau nếu cần
        );

        ValidationRuleAssignmentDeleted archived = assignmentPort.saveDeleted(deleted);
        log.info("Deleted assignment archived successfully: archiveId={}, assignmentId={}",
                archived.getId(), archived.getAssignmentId());
    }

    /**
     * Validate UUID format (simple check)
     */
    private boolean isValidUuid(String uuid) {
        if (uuid == null) {
            return false;
        }
        // Simple UUID pattern check
        String uuidPattern = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";
        return uuid.matches(uuidPattern);
    }
}
