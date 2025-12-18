package vn.viettel.vds.promotion.validation.application.port.out;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.viettel.vds.promotion.validation.domain.model.Assignment;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Outbound port for Assignment persistence operations.
 */
public interface AssignmentPersistencePort {

    Assignment save(Assignment assignment);

    Optional<Assignment> findById(String id);

    Optional<Assignment> findByTenantIdAndSubjectTypeAndSubjectKey(String tenantId, String subjectType, String subjectKey);

    List<Assignment> findActiveByTenantIdAndSubject(String tenantId, String subjectType, String subjectKey);

    List<Assignment> findByTenantIdAndRuleId(String tenantId, String ruleId);

    Page<Assignment> findWithFilters(String tenantId, String subjectType, String subjectKeyPattern,
                                     Boolean active, String ruleId, Pageable pageable);

    List<Assignment> findActiveAtTime(String tenantId, Instant time);

    List<Assignment> findOverlappingAssignments(String tenantId, String subjectType, String subjectKey,
                                                Instant validFrom, Instant validTo);

    Page<Assignment> findByTenantIdAndRuleIdPaged(String tenantId, String ruleId, Pageable pageable);

    boolean existsActiveAssignmentForSubject(String tenantId, String subjectType, String subjectKey);

    long countByTenantIdAndActive(String tenantId, Boolean active);

    Optional<Assignment> findLatestVersionByRuleId(String tenantId, String ruleId);

    boolean existsBySubjectTypeAndSubjectKey(String subjectType, String subjectKey);

    Optional<Assignment> findBySubjectTypeAndSubjectKey(String subjectType, String subjectKey);

    List<Assignment> findAllBySubjectTypeAndSubjectKey(String subjectType, String subjectKey);

    void delete(Assignment assignment);

    void deleteById(String id);

    /**
     * Find assignment by rule ID and entity ID (object_id).
     * Used for delete assignment operation (SRS PRM_KBNV_API_VALD008).
     */
    Optional<Assignment> findByRuleIdAndEntityId(String ruleId, String entityId);

    /**
     * Soft delete assignment: save to deleted table and remove from assignments table.
     * According to SRS PRM_KBNV_API_VALD008:
     * - Update deleted_at, deleted_by, version+1
     * - Insert into validation_rules_assignment_deleted
     * - Delete from assignments table
     *
     * @param assignment the assignment to delete
     * @param deletedBy  the user performing the deletion
     * @return the ID of the deleted assignment
     */
    String softDeleteAssignment(Assignment assignment, String deletedBy);
}
