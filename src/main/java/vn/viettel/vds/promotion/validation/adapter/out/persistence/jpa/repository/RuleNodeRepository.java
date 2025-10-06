package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository;

import com.promix.platform.data.jpa.autoconfigure.condition.ConditionalOnPromixJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.RuleNodeEntity;

import java.util.List;
import java.util.Optional;

@ConditionalOnPromixJpa
@Repository
public interface RuleNodeRepository extends JpaRepository<RuleNodeEntity, String> {

    /**
     * Find rule nodes by validation rule ID.
     */
    List<RuleNodeEntity> findByValidationRuleIdOrderByOrder(String validationRuleId);

    /**
     * Find rule node by node ID and validation rule ID.
     */
    Optional<RuleNodeEntity> findByNodeIdAndValidationRuleId(String nodeId, String validationRuleId);

    /**
     * Find root nodes (nodes without parent) for a validation rule.
     */
    List<RuleNodeEntity> findByValidationRuleIdAndParentIsNullOrderByOrder(String validationRuleId);

    /**
     * Find child nodes by parent ID.
     */
    List<RuleNodeEntity> findByParentIdOrderByOrder(String parentId);

    /**
     * Find nodes by type and validation rule ID.
     */
    List<RuleNodeEntity> findByTypeAndValidationRuleId(String type, String validationRuleId);

    /**
     * Find nodes by operator name.
     */
    @Query("SELECT n FROM RuleNodeEntity n WHERE n.operatorName = :operatorName AND n.validationRule.id = :ruleId")
    List<RuleNodeEntity> findByOperatorNameAndRuleId(@Param("operatorName") String operatorName, @Param("ruleId") String ruleId);

    /**
     * Delete all nodes for a validation rule.
     */
    void deleteByValidationRuleId(String validationRuleId);
}