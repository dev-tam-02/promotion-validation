package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository;

import com.promix.platform.data.jpa.autoconfigure.condition.ConditionalOnPromixJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.RuleBundleEntity;

import java.util.List;
import java.util.Optional;

@ConditionalOnPromixJpa
@Repository
public interface RuleBundleRepository extends JpaRepository<RuleBundleEntity, String> {

    /**
     * Find bundle by hash.
     */
    Optional<RuleBundleEntity> findByBundleHash(String bundleHash);

    /**
     * Find bundles by subject type and key.
     */
    @Query("SELECT rb FROM RuleBundleEntity rb WHERE rb.subject.type = :type AND rb.subject.key = :key ORDER BY rb.ruleVersion DESC, rb.assignmentVersion DESC")
    List<RuleBundleEntity> findBySubjectTypeAndKey(@Param("type") String type, @Param("key") String key);

    /**
     * Find bundles by validation rule ID.
     */
    List<RuleBundleEntity> findByValidationRuleId(String validationRuleId);

    /**
     * Find latest bundle for subject and rule.
     */
    @Query("SELECT rb FROM RuleBundleEntity rb WHERE rb.subject.id = :subjectId AND rb.validationRule.id = :ruleId ORDER BY rb.ruleVersion DESC, rb.assignmentVersion DESC")
    Optional<RuleBundleEntity> findLatestBundleForSubjectAndRule(@Param("subjectId") String subjectId, @Param("ruleId") String ruleId);

    /**
     * Check if bundle hash exists.
     */
    boolean existsByBundleHash(String bundleHash);
}