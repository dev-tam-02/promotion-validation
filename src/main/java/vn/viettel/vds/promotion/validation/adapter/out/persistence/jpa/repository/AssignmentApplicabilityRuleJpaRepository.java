package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.AssignmentApplicabilityRuleEntity;

import java.util.List;

/**
 * Repository for AssignmentApplicabilityRuleEntity.
 * Provides methods to query applicability rules for assignments.
 */
@Repository
public interface AssignmentApplicabilityRuleJpaRepository extends JpaRepository<AssignmentApplicabilityRuleEntity, String> {

    /**
     * Find all applicability rules for a specific assignment
     */
    List<AssignmentApplicabilityRuleEntity> findByAssignmentId(String assignmentId);

    /**
     * Find applicability rules by assignment ID and rule type (INCLUDED/EXCLUDED)
     */
    List<AssignmentApplicabilityRuleEntity> findByAssignmentIdAndRuleType(String assignmentId, String ruleType);

    /**
     * Find all included rules for an assignment
     */
    @Query("SELECT r FROM AssignmentApplicabilityRuleEntity r WHERE r.assignment.id = :assignmentId AND r.ruleType = 'INCLUDED'")
    List<AssignmentApplicabilityRuleEntity> findIncludedRulesByAssignmentId(@Param("assignmentId") String assignmentId);

    /**
     * Find all excluded rules for an assignment
     */
    @Query("SELECT r FROM AssignmentApplicabilityRuleEntity r WHERE r.assignment.id = :assignmentId AND r.ruleType = 'EXCLUDED'")
    List<AssignmentApplicabilityRuleEntity> findExcludedRulesByAssignmentId(@Param("assignmentId") String assignmentId);

    /**
     * Find rules by object type and object ID (useful for finding all assignments that apply to a specific product)
     */
    @Query("SELECT r FROM AssignmentApplicabilityRuleEntity r WHERE r.objectType = :objectType AND r.objectId = :objectId")
    List<AssignmentApplicabilityRuleEntity> findByObjectTypeAndObjectId(
            @Param("objectType") String objectType,
            @Param("objectId") String objectId);

    /**
     * Delete all applicability rules for an assignment
     */
    void deleteByAssignmentId(String assignmentId);

    /**
     * Check if any applicability rules exist for an assignment
     */
    boolean existsByAssignmentId(String assignmentId);
}
