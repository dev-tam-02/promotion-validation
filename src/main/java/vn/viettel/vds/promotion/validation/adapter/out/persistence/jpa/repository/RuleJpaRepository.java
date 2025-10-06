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
 * JPA repository for validation rules
 */
@ConditionalOnPromixJpa
@Repository
public interface RuleJpaRepository extends JpaRepository<RuleJpaEntity, String> {

    /**
     * Find rule by rule code
     */
    Optional<RuleJpaEntity> findByRuleCode(String ruleCode);

    /**
     * Find all active rules
     */
    List<RuleJpaEntity> findByActiveTrue();

    /**
     * Find rules by type
     */
    List<RuleJpaEntity> findByType(String type);

    /**
     * Find rules by campaign ID
     */
    List<RuleJpaEntity> findByCampaignId(String campaignId);

    /**
     * Find rules by rule set ID
     */
    List<RuleJpaEntity> findByRuleSetId(String ruleSetId);

    /**
     * Find rules by priority range
     */
    @Query("SELECT r FROM RuleJpaEntity r WHERE r.priority BETWEEN :minPriority AND :maxPriority ORDER BY r.priority")
    List<RuleJpaEntity> findByPriorityBetween(
            @Param("minPriority") Integer minPriority,
            @Param("maxPriority") Integer maxPriority
    );

    /**
     * Check if rule exists by code
     */
    boolean existsByRuleCode(String ruleCode);

    /**
     * Delete rules by campaign ID
     */
    void deleteByCampaignId(String campaignId);

    /**
     * Find active rules by campaign
     */
    @Query("SELECT r FROM RuleJpaEntity r WHERE r.campaignId = :campaignId AND r.active = true ORDER BY r.priority")
    List<RuleJpaEntity> findActiveByCampaignId(@Param("campaignId") String campaignId);

    /**
     * Find rules ordered by priority
     */
    List<RuleJpaEntity> findAllByOrderByPriorityAsc();

    /**
     * Count active rules
     */
    long countByActiveTrue();
}