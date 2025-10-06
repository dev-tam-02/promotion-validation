package vn.viettel.vds.promotion.validation.domain.model;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Value object representing the result of a validation
 */
@Getter
@Builder
@ToString
public class ValidationResult {

    private final String validationId;
    private final String ruleId;
    private final String ruleCode;
    private final String message;
    private final Decision decision;
    private final List<String> reasonCodes;
    private final List<String> explanations;
    private final Map<String, Object> metadata;
    private final Instant timestamp;
    private final long processingTimeMs;

    /**
     * Static factory method for successful validation
     */
    public static ValidationResult allow(String validationId) {
        return ValidationResult.builder()
                .validationId(validationId)
                .decision(Decision.ALLOW)
                .reasonCodes(new ArrayList<>())
                .explanations(new ArrayList<>())
                .timestamp(Instant.now())
                .processingTimeMs(0)
                .build();
    }

    /**
     * Static factory method for denied validation
     */
    public static ValidationResult deny(
            String validationId,
            String reasonCode,
            String explanation) {
        return ValidationResult.builder()
                .validationId(validationId)
                .decision(Decision.DENY)
                .reasonCodes(List.of(reasonCode))
                .explanations(List.of(explanation))
                .timestamp(Instant.now())
                .processingTimeMs(0)
                .build();
    }

    /**
     * Static factory method for error result
     */
    public static ValidationResult error(
            String validationId,
            String errorMessage) {
        return ValidationResult.builder()
                .validationId(validationId)
                .decision(Decision.ERROR)
                .reasonCodes(List.of("VALIDATION_ERROR"))
                .explanations(List.of(errorMessage))
                .timestamp(Instant.now())
                .processingTimeMs(0)
                .build();
    }

    /**
     * Static factory method for skipped validation
     */
    public static ValidationResult skipped(Object ruleId, String reason) {
        return ValidationResult.builder()
                .validationId(String.valueOf(ruleId))
                .ruleId(String.valueOf(ruleId))
                .decision(Decision.PENDING)
                .message(reason)
                .reasonCodes(List.of("SKIPPED"))
                .explanations(List.of(reason))
                .timestamp(Instant.now())
                .processingTimeMs(0)
                .build();
    }

    /**
     * Static factory method for passed validation
     */
    public static ValidationResult passed(Object ruleId, String message) {
        return ValidationResult.builder()
                .validationId(String.valueOf(ruleId))
                .ruleId(String.valueOf(ruleId))
                .decision(Decision.ALLOW)
                .message(message)
                .reasonCodes(new ArrayList<>())
                .explanations(List.of(message))
                .timestamp(Instant.now())
                .processingTimeMs(0)
                .build();
    }

    /**
     * Static factory method for failed validation
     */
    public static ValidationResult failed(Object ruleId, String message) {
        return ValidationResult.builder()
                .validationId(String.valueOf(ruleId))
                .ruleId(String.valueOf(ruleId))
                .decision(Decision.DENY)
                .message(message)
                .reasonCodes(List.of("VALIDATION_FAILED"))
                .explanations(List.of(message))
                .timestamp(Instant.now())
                .processingTimeMs(0)
                .build();
    }

    /**
     * Check if validation passed
     */
    public boolean isAllowed() {
        return decision == Decision.ALLOW;
    }

    /**
     * Check if validation was denied
     */
    public boolean isDenied() {
        return decision == Decision.DENY;
    }

    /**
     * Check if validation had an error
     */
    public boolean hasError() {
        return decision == Decision.ERROR;
    }

    /**
     * Check if validation is still pending
     */
    public boolean isPending() {
        return decision == Decision.PENDING;
    }

    /**
     * Get the primary reason for denial
     */
    public String getPrimaryReasonCode() {
        if (reasonCodes != null && !reasonCodes.isEmpty()) {
            return reasonCodes.get(0);
        }
        return null;
    }

    /**
     * Get the primary explanation
     */
    public String getPrimaryExplanation() {
        if (explanations != null && !explanations.isEmpty()) {
            return explanations.get(0);
        }
        return null;
    }

    /**
     * Check if the result contains a specific reason code
     */
    public boolean hasReasonCode(String code) {
        return reasonCodes != null && reasonCodes.contains(code);
    }

    /**
     * Check if validation is valid (for backward compatibility)
     */
    public boolean isValid() {
        return isAllowed();
    }

    public enum Decision {
        ALLOW,
        DENY,
        PENDING,
        ERROR
    }
}