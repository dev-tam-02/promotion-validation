package vn.viettel.vds.promotion.validation.application.port.out;

import vn.viettel.vds.promotion.validation.domain.model.RuleTimeFrame;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port for RuleTimeFrame persistence operations.
 * <p>
 * NOTE: This is legacy code kept for backward compatibility.
 * New implementations should use RuleTemporalLinkPersistencePort with TemporalPolicy instead.
 */
public interface RuleTimeFramePersistencePort {

    RuleTimeFrame save(RuleTimeFrame ruleTimeFrame);

    Optional<RuleTimeFrame> findById(String id);

    List<RuleTimeFrame> findByValidationRuleId(String ruleId);

    List<RuleTimeFrame> findByTimeFrameId(String timeFrameId);

    List<RuleTimeFrame> findByValidationRuleIdAndMode(String ruleId, String mode);

    void deleteByValidationRuleId(String ruleId);

    void deleteById(String id);
}
