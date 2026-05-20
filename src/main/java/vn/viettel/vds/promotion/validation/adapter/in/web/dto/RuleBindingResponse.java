package vn.viettel.vds.promotion.validation.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Response DTO for rule binding.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Rule binding response")
public class RuleBindingResponse {

    @Schema(description = "Binding ID", example = "rb-001")
    private String id;

    // ========== Rule Reference ==========

    @Schema(description = "Validation rule ID", example = "rule-discount-20")
    private String ruleId;

    @Schema(description = "Pinned rule version (null = always use latest)")
    private Integer ruleVersionPinned;

    // ========== Object Reference ==========

    @Schema(description = "Object type (CAMPAIGN, DISCOUNT, VOUCHER, CASHBACK)", example = "CAMPAIGN")
    private String objectType;

    @Schema(description = "Object ID", example = "CAMP-001")
    private String objectId;

    // ========== Priority & State ==========

    @Schema(description = "Binding priority", example = "10")
    private Integer priority;

    @Schema(description = "Whether binding is active", example = "true")
    private Boolean active;

    // ========== Time Constraints ==========

    @Schema(description = "Start of validity period", example = "2024-01-01T00:00:00Z")
    private Instant validFrom;

    @Schema(description = "End of validity period", example = "2024-12-31T23:59:59Z")
    private Instant validTo;

    @Schema(description = "Timezone", example = "Asia/Ho_Chi_Minh")
    private String timezone;

    @Schema(description = "RFC 5545 recurrence rule", example = "FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR")
    private String rrule;

    @Schema(description = "Time windows within each day")
    private List<TimeWindowDto> timeWindows;

    @Schema(description = "ISO 8601 duration per recurrence", example = "PT2H")
    private String duration;

    @Schema(description = "ISO 8601 extension window after publish", example = "PT6H")
    private String activityDurationAfterPublishing;

    @Schema(description = "Excluded dates", example = "[\"2024-01-01\",\"2024-12-25\"]")
    private List<String> excludedDates;

    // ========== Product Scope ==========

    @Schema(description = "Applies to all products", example = "false")
    private Boolean includedAll;

    @Schema(description = "Included product IDs")
    private List<String> includedProducts;

    @Schema(description = "Excluded product IDs")
    private List<String> excludedProducts;

    @Schema(description = "Included category IDs")
    private List<String> includedCategories;

    @Schema(description = "Excluded category IDs")
    private List<String> excludedCategories;

    @Schema(description = "Included brand IDs")
    private List<String> includedBrands;

    @Schema(description = "Excluded brand IDs")
    private List<String> excludedBrands;

    // ========== Traffic Control ==========

    @Schema(description = "Traffic percentage (0-100)", example = "100")
    private Integer trafficPercent;

    @Schema(description = "Sticky key strategy", example = "CUSTOMER_ID")
    private String stickyKeyStrategy;

    // ========== Compiled ==========

    @Schema(description = "Compiled DRL bundle hash")
    private String bundleHash;

    // ========== Audit ==========

    @Schema(description = "Creation timestamp", example = "2024-01-01T00:00:00Z")
    private Instant createdAt;

    @Schema(description = "Last update timestamp", example = "2024-01-15T10:30:00Z")
    private Instant updatedAt;

    @Schema(description = "Created by user", example = "admin")
    private String createdBy;

    @Schema(description = "Updated by user", example = "admin")
    private String updatedBy;

    @Schema(description = "Version for optimistic locking", example = "1")
    private Long version;

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

        @Schema(description = "Days of week this window applies to (1=Monday..7=Sunday, ISO-8601). "
                + "Null/empty means every day.", example = "[1, 2, 3, 4, 5]")
        private List<Integer> daysOfWeek;
    }
}
