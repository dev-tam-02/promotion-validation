package vn.viettel.vds.promotion.validation.domain.exception;

import com.promix.platform.core.exception.BadRequestException;

import java.util.Map;

/**
 * Exception thrown when attempting to delete a rule that is still assigned to campaigns.
 * <p>
 * Error code {@code RULE_HAS_BINDINGS}, HTTP 400 Bad Request — mirrors the
 * {@code @ApiResponse} declared on the delete endpoint so the CMS shows the
 * "rule is assigned" toast instead of falling through to the generic 500 handler.
 */
public class RuleHasBindingsException extends BadRequestException {

    private static final String ERROR_CODE = "RULE_HAS_BINDINGS";

    public RuleHasBindingsException(String ruleId, long bindingCount) {
        super(ERROR_CODE,
                "Quy tắc đã được gán cho chiến dịch, không thể xóa",
                Map.of("ruleId", ruleId, "bindingCount", bindingCount));
    }
}
