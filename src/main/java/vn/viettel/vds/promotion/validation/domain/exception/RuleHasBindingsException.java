package vn.viettel.vds.promotion.validation.domain.exception;

/**
 * Exception thrown when attempting to delete a rule that is still assigned to campaigns.
 */
public class RuleHasBindingsException extends RuntimeException {

    private final String ruleId;
    private final long bindingCount;

    public RuleHasBindingsException(String ruleId, long bindingCount) {
        super(String.format("Rule '%s' cannot be deleted: it is assigned to %d campaign(s)", ruleId, bindingCount));
        this.ruleId = ruleId;
        this.bindingCount = bindingCount;
    }

    public String getRuleId() {
        return ruleId;
    }

    public long getBindingCount() {
        return bindingCount;
    }
}
