package vn.viettel.vds.promotion.validation.adapter.out.persistence.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.entity.RuleTimeFrame;

import java.util.List;

@Repository
public interface RuleTimeFrameRepository extends MongoRepository<RuleTimeFrame, String> {

    /**
     * Find timeframes by rule ID
     */
    List<RuleTimeFrame> findByRuleId(String ruleId);

    /**
     * Find timeframes by timeframe ID
     */
    List<RuleTimeFrame> findByTimeFrameId(String timeFrameId);

    /**
     * Find timeframes by rule ID and mode
     */
    List<RuleTimeFrame> findByRuleIdAndMode(String ruleId, String mode);

    /**
     * Delete timeframes by rule ID
     */
    void deleteByRuleId(String ruleId);
}