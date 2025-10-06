package vn.viettel.vds.promotion.validation.adapter.in.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * DTO for validation response to REST API
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResponseDto {

    private String validationId;
    private String decision; // ALLOW, DENY, PENDING, ERROR
    private List<String> reasonCodes;
    private List<String> explanations;
    private Map<String, Object> metadata;
    private Instant timestamp;
    private Long processingTimeMs;

    // Convenience fields for backward compatibility
    private Boolean ok;
    private String reasonCode;
    private String explanation;

    /**
     * Create success response
     */
    public static ValidationResponseDto success(String validationId) {
        return ValidationResponseDto.builder()
                .validationId(validationId)
                .decision("ALLOW")
                .ok(true)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Create denial response
     */
    public static ValidationResponseDto deny(
            String validationId,
            String reasonCode,
            String explanation) {
        return ValidationResponseDto.builder()
                .validationId(validationId)
                .decision("DENY")
                .ok(false)
                .reasonCode(reasonCode)
                .explanation(explanation)
                .reasonCodes(List.of(reasonCode))
                .explanations(List.of(explanation))
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Create error response
     */
    public static ValidationResponseDto error(
            String validationId,
            String errorMessage) {
        return ValidationResponseDto.builder()
                .validationId(validationId)
                .decision("ERROR")
                .ok(false)
                .reasonCode("VALIDATION_ERROR")
                .explanation(errorMessage)
                .timestamp(Instant.now())
                .build();
    }
}