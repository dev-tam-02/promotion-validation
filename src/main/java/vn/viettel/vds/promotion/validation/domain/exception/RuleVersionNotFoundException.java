package vn.viettel.vds.promotion.validation.domain.exception;

import com.promix.platform.core.exception.ResourceNotFoundException;

/**
 * Exception thrown when a rule version is not found.
 */
public class RuleVersionNotFoundException extends ResourceNotFoundException {

    private static final String ERROR_CODE = "RULE_VERSION_NOT_FOUND";

    public RuleVersionNotFoundException(String ruleId, Integer version) {
        super(ERROR_CODE, "RuleVersion", ruleId + "@" + version,
                String.format("Rule version not found: ruleId=%s, version=%d", ruleId, version));
    }

    public RuleVersionNotFoundException(String ruleVersionId) {
        super(ERROR_CODE, "RuleVersion", ruleVersionId);
    }
}
