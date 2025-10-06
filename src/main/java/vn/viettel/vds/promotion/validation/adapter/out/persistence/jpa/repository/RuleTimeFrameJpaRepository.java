package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository;

import com.promix.platform.data.jpa.autoconfigure.condition.ConditionalOnPromixJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.RuleTimeFrameEntity;

import java.util.List;

@ConditionalOnPromixJpa
@Repository
public interface RuleTimeFrameJpaRepository extends JpaRepository<RuleTimeFrameEntity, String> {

    /**
     * Find timeframes by validation rule ID
     */
    @Query("SELECT r FROM RuleTimeFrameEntity r WHERE r.validationRule.id = :ruleId")
    List<RuleTimeFrameEntity> findByValidationRuleId(@Param("ruleId") String ruleId);

    /**
     * Find timeframes by timeframe ID
     */
    List<RuleTimeFrameEntity> findByTimeFrameId(String timeFrameId);

    /**
     * Find timeframes by rule ID and mode
     */
    @Query("SELECT r FROM RuleTimeFrameEntity r WHERE r.validationRule.id = :ruleId AND r.mode = :mode")
    List<RuleTimeFrameEntity> findByValidationRuleIdAndMode(
            @Param("ruleId") String ruleId,
            @Param("mode") String mode
    );

    /**
     * Delete timeframes by validation rule ID
     */
    @Modifying
    @Query("DELETE FROM RuleTimeFrameEntity r WHERE r.validationRule.id = :ruleId")
    void deleteByValidationRuleId(@Param("ruleId") String ruleId);
}
