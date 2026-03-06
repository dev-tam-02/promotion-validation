package vn.viettel.vds.promotion.validation.domain.exception;

import com.promix.platform.core.exception.BadRequestException;

import java.util.Map;

/**
 * Exception thrown when rule structure is invalid.
 * HTTP Status: 400 Bad Request
 */
public class InvalidRuleStructureException extends BadRequestException {

    private static final String ERROR_CODE = "INVALID_RULE_STRUCTURE";

    public InvalidRuleStructureException(String message) {
        super(ERROR_CODE, message);
    }

    public InvalidRuleStructureException(String ruleId, String message) {
        super(ERROR_CODE, message, Map.of("ruleId", ruleId));
    }

    public InvalidRuleStructureException(String ruleId, String nodeId, String message) {
        super(ERROR_CODE, message, Map.of("ruleId", ruleId, "nodeId", nodeId));
    }
}
