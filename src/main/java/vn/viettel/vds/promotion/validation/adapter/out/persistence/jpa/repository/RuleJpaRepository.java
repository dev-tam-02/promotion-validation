package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository;

import com.promix.platform.data.jpa.autoconfigure.condition.ConditionalOnPromixJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.RuleJpaEntity;

import java.util.List;
import java.util.Optional;

/**
 * JPA repository for validation rules.
 * <p>
 * Note: This repository only includes queries for fields that exist in the validation_rules table.
 * Removed methods that referenced non-existent fields:
 * - findByRuleCode (ruleCode field doesn't exist)
 * - findByType (type field doesn't exist)
 * - findByCampaignId (campaignId field doesn't exist)
 * - findByRuleSetId (ruleSetId field doesn't exist)
 * - findByPriorityBetween (priority field doesn't exist)
 * <p>
 * Use the 'code' field instead of 'ruleCode' for code-based queries.
 */
@ConditionalOnPromixJpa
@Repository
public interface RuleJpaRepository extends JpaRepository<RuleJpaEntity, String> {

    /**
     * Find rule by code (this is the actual column in validation_rules table)
     */
    Optional<RuleJpaEntity> findByCode(String code);

    /**
     * Find rules by logic type
     */
    List<RuleJpaEntity> findByLogic(String logic);

    /**
     * Find all rules ordered by rule version descending
     */
    List<RuleJpaEntity> findAllByOrderByRuleVersionDesc();

    /**
     * Find rules by version number
     */
    List<RuleJpaEntity> findByRuleVersion(Long ruleVersion);

    /**
     * Check if rule exists by code
     */
    boolean existsByCode(String code);

    /**
     * Find rules whose name matches under the column collation. Used by the
     * duplicate-name check; the candidate set is narrowed to a code-point exact
     * match (cs_as) in the adapter, since the column collation may be ci/ai.
     */
    List<RuleJpaEntity> findByName(String name);

    /**
     * Find latest version for a given code
     */
    @Query("SELECT r FROM RuleJpaEntity r WHERE r.code = :code ORDER BY r.ruleVersion DESC")
    List<RuleJpaEntity> findByCodeOrderByRuleVersionDesc(@Param("code") String code);

    /**
     * Find rules with no bundleHash — used by bootstrap runner to compile seeded
     * system rules. VRUL001: no state filter (rules have no lifecycle state).
     */
    @Query("SELECT r FROM RuleJpaEntity r WHERE r.bundleHash IS NULL")
    List<RuleJpaEntity> findPublishedWithNullBundleHash();

    /**
     * SRS VRUL005 Bước 9: atomic optimistic-locked delete. Removes the rule only
     * if the held version still matches at the database, then reports the number
     * of affected rows. The caller treats {@code 0} as a version conflict — this
     * closes the read-then-delete race window (no row is deleted under a stale
     * version even if two requests pass the in-memory version check concurrently).
     */
    // clearAutomatically: a bulk DELETE bypasses the persistence context, so the
    // Rule entity loaded earlier in the same transaction would stay managed and
    // trigger a spurious OptimisticLockException at commit. Clearing the context
    // after the delete evicts that now-removed entity.
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM RuleJpaEntity r WHERE r.id = :id AND r.version = :version")
    int deleteByIdAndVersion(@Param("id") String id, @Param("version") long version);
}