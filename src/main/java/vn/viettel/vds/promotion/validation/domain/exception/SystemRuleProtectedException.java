package vn.viettel.vds.promotion.validation.domain.exception;

import com.promix.platform.core.exception.ConflictException;

/**
 * Exception thrown when attempting to delete or modify a system-managed rule.
 * System rules (is_system = true) are seeded by Liquibase and managed by the
 * platform — they must not be mutated or deleted via the admin API.
 *
 * <p>HTTP Status: 409 Conflict
 */
public class SystemRuleProtectedException extends ConflictException {

    private static final String ERROR_CODE = "SYSTEM_RULE_PROTECTED";

    public SystemRuleProtectedException(String ruleId) {
        super(ERROR_CODE,
                "System rule cannot be deleted or modified: " + ruleId,
                "ValidationRule", "id", ruleId);
    }
}
