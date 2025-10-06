package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository;

import com.promix.platform.data.jpa.autoconfigure.condition.ConditionalOnPromixJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.RuleAssignmentEntity;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@ConditionalOnPromixJpa
@Repository
public interface RuleAssignmentRepository extends JpaRepository<RuleAssignmentEntity, String> {

    /**
     * Find active assignments by subject type and key.
     */
    @Query("SELECT ra FROM RuleAssignmentEntity ra WHERE ra.subject.type = :type AND ra.subject.key = :key AND ra.active = true")
    List<RuleAssignmentEntity> findActiveAssignmentsBySubject(@Param("type") String type, @Param("key") String key);

    /**
     * Find assignments by validation rule ID.
     */
    List<RuleAssignmentEntity> findByValidationRuleId(String validationRuleId);

    /**
     * Find assignments by validation rule ID and active status.
     */
    List<RuleAssignmentEntity> findByValidationRuleIdAndActive(String validationRuleId, Boolean active);

    /**
     * Find latest assignment version for a rule.
     */
    @Query("SELECT ra FROM RuleAssignmentEntity ra WHERE ra.validationRule.id = :ruleId ORDER BY ra.assignmentVersion DESC")
    Optional<RuleAssignmentEntity> findLatestAssignmentVersionByRuleId(@Param("ruleId") String ruleId);

    /**
     * Find valid assignments at a specific time.
     */
    @Query("SELECT ra FROM RuleAssignmentEntity ra WHERE ra.active = true " +
            "AND (ra.validFrom IS NULL OR ra.validFrom <= :checkTime) " +
            "AND (ra.validTo IS NULL OR ra.validTo >= :checkTime)")
    List<RuleAssignmentEntity> findValidAssignmentsAt(@Param("checkTime") Instant checkTime);

    /**
     * Find assignments by subject ID.
     */
    List<RuleAssignmentEntity> findBySubjectId(String subjectId);

    /**
     * Find assignments by subject ID and active status.
     */
    List<RuleAssignmentEntity> findBySubjectIdAndActive(String subjectId, Boolean active);
}