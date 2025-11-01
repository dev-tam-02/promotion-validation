package vn.viettel.vds.promotion.validation.application.port.out;

import vn.viettel.vds.promotion.validation.domain.model.Rule;

import java.util.Optional;

/**
 * Outbound port cho ValidationRule persistence operations.
 * Interface này để kiểm tra sự tồn tại của validation rule.
 */
public interface ValidationRulePersistencePort {

    /**
     * Tìm validation rule theo ID.
     *
     * @param ruleId ID của validation rule
     * @return Optional chứa Rule nếu tìm thấy
     */
    Optional<Rule> findById(String ruleId);

    /**
     * Kiểm tra validation rule có tồn tại không.
     *
     * @param ruleId ID của validation rule
     * @return true nếu tồn tại, false nếu không
     */
    boolean exists(String ruleId);
}
