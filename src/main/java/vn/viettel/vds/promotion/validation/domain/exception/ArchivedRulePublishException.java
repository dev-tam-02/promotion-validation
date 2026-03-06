package vn.viettel.vds.promotion.validation.domain.exception;

import com.promix.platform.core.exception.BusinessRuleException;

import java.util.Map;

/**
 * Exception thrown when attempting to publish an archived rule.
 */
public class ArchivedRulePublishException extends BusinessRuleException {

    private static final String ERROR_CODE = "CANNOT_PUBLISH_ARCHIVED_RULE";

    public ArchivedRulePublishException(String ruleId) {
        super(ERROR_CODE,
                String.format("Cannot publish archived rule: %s", ruleId),
                Map.of("ruleId", ruleId));
    }
}
