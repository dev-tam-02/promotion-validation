package vn.viettel.vds.promotion.validation.adapter.in.web.dto;

/**
 * Response DTO for pre-flight validation rule compatibility check
 * Indicates whether a rule is compatible with the requested campaign type
 */
public record ValidateCompatibilityResponse(
    boolean isValid,
    String errorCode,
    String errorMessage,
    String ruleId,
    String ruleName,
    String ruleStatus
) {
    /**
     * Create a successful validation response
     */
    public static ValidateCompatibilityResponse success(String ruleId, String ruleName, String ruleStatus) {
        return new ValidateCompatibilityResponse(true, null, null, ruleId, ruleName, ruleStatus);
    }

    /**
     * Create a failed validation response
     */
    public static ValidateCompatibilityResponse failure(String ruleId, String errorCode, String errorMessage) {
        return new ValidateCompatibilityResponse(false, errorCode, errorMessage, ruleId, null, null);
    }
}
