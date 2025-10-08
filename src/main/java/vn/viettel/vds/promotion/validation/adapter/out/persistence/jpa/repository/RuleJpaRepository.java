package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository;

import com.promix.platform.data.jpa.autoconfigure.condition.ConditionalOnPromixJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.RuleJpaEntity;

import java.util.List;
import java.util.Optional;

/**
 * JPA repository for validation rules.
 *
 * Note: This repository only includes queries for fields that exist in the validation_rules table.
 * Removed methods that referenced non-existent fields:
 * - findByRuleCode (ruleCode field doesn't exist)
 * - findByType (type field doesn't exist)
 * - findByCampaignId (campaignId field doesn't exist)
 * - findByRuleSetId (ruleSetId field doesn't exist)
 * - findByPriorityBetween (priority field doesn't exist)
 *
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
     * Find rules by state (draft, published, archived)
     */
    List<RuleJpaEntity> findByState(String state);

    /**
     * Find rules by state ordered by rule version descending
     */
    @Query("SELECT r FROM RuleJpaEntity r WHERE r.state = :state ORDER BY r.ruleVersion DESC")
    List<RuleJpaEntity> findByStateOrderByRuleVersionDesc(@Param("state") String state);

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
     * Find latest version for a given code
     */
    @Query("SELECT r FROM RuleJpaEntity r WHERE r.code = :code ORDER BY r.ruleVersion DESC")
    List<RuleJpaEntity> findByCodeOrderByRuleVersionDesc(@Param("code") String code);

    /**
     * Find published rules
     */
    @Query("SELECT r FROM RuleJpaEntity r WHERE r.state = 'PUBLISHED' ORDER BY r.ruleVersion DESC")
    List<RuleJpaEntity> findPublishedRules();
}