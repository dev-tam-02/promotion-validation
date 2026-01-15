package vn.viettel.vds.promotion.validation.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Request DTO for creating or updating a rule binding.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create or update a rule binding")
public class RuleBindingRequest {

    @Schema(description = "Validation rule ID", example = "rule-discount-20", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "ruleId is required")
    @Size(max = 36, message = "ruleId must not exceed 36 characters")
    private String ruleId;

    @Schema(description = "Pin to specific rule version (null = always use latest)")
    private Integer ruleVersionPinned;

    @Schema(description = "Target type", example = "CAMPAIGN", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "targetType is required")
    @Size(max = 50, message = "targetType must not exceed 50 characters")
    private String targetType;

    @Schema(description = "Target ID", example = "CAMP-001", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "targetId is required")
    @Size(max = 100, message = "targetId must not exceed 100 characters")
    private String targetId;

    @Schema(description = "Binding priority (higher = evaluated first)", example = "10")
    @Min(value = 0, message = "priority must be non-negative")
    private Integer priority;

    @Schema(description = "Whether binding is active", example = "true")
    private Boolean active;

    // ========== Time Constraints ==========

    @Schema(description = "Start of validity period", example = "2024-01-01T00:00:00Z")
    private Instant validFrom;

    @Schema(description = "End of validity period", example = "2024-12-31T23:59:59Z")
    private Instant validTo;

    @Schema(description = "Timezone for time evaluations", example = "Asia/Ho_Chi_Minh")
    @Size(max = 50, message = "timezone must not exceed 50 characters")
    private String timezone;

    @Schema(description = "RFC 5545 recurrence rule", example = "FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR")
    @Size(max = 1000, message = "rrule must not exceed 1000 characters")
    private String rrule;

    @Schema(description = "Time windows within each day")
    private List<TimeWindowDto> timeWindows;

    @Schema(description = "Dates when the rule does NOT apply", example = "[\"2024-01-01\",\"2024-12-25\"]")
    private List<String> excludedDates;

    // ========== Product Scope ==========

    @Schema(description = "If true, applies to all products", example = "false")
    private Boolean includedAll;

    @Schema(description = "Included product IDs", example = "[\"PROD-001\",\"PROD-002\"]")
    private List<String> includedProducts;

    @Schema(description = "Excluded product IDs", example = "[\"PROD-999\"]")
    private List<String> excludedProducts;

    @Schema(description = "Included category IDs", example = "[\"CAT-ELECTRONICS\"]")
    private List<String> includedCategories;

    @Schema(description = "Excluded category IDs", example = "[\"CAT-PREMIUM\"]")
    private List<String> excludedCategories;

    @Schema(description = "Included brand IDs")
    private List<String> includedBrands;

    @Schema(description = "Excluded brand IDs")
    private List<String> excludedBrands;

    // ========== Traffic Control ==========

    @Schema(description = "Percentage of traffic this binding applies to (0-100)", example = "100")
    @Min(value = 0, message = "trafficPercent must be at least 0")
    @Max(value = 100, message = "trafficPercent must not exceed 100")
    private Integer trafficPercent;

    @Schema(description = "Strategy for consistent traffic assignment", example = "CUSTOMER_ID")
    private String stickyKeyStrategy;

    /**
     * Time window DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Time window within a day")
    public static class TimeWindowDto {
        @Schema(description = "Start time (HH:mm)", example = "09:00")
        private String start;

        @Schema(description = "End time (HH:mm)", example = "17:00")
        private String end;
    }
}
