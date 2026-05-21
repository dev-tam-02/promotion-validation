package vn.viettel.vds.promotion.validation.adapter.in.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Response DTO for rule with its binding details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleWithBindingResponse {

    private RuleResponse rule;
    private BindingDetails binding;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BindingDetails {
        private String bindingId;
        private String objectType;
        private String objectId;
        private String ruleId;
        private Integer ruleVersionPinned;
        private Boolean active;
        private Integer priority;

        // Temporal constraints
        private Instant validFrom;
        private Instant validTo;
        private String timezone;
        private String rrule;
        private List<TimeWindowDto> timeWindows;
        private String duration;
        private String activityDurationAfterPublishing;
        private List<String> excludedDates;

        // Applicability
        private Boolean includedAll;
        private List<String> includedProducts;
        private List<String> excludedProducts;
        private List<String> includedCategories;
        private List<String> excludedCategories;
        private List<String> includedBrands;
        private List<String> excludedBrands;

        // Traffic control
        private Integer trafficPercent;
        private String stickyKeyStrategy;

        // Deployment
        private String bundleHash;

        // Audit
        private Instant createdAt;
        private Instant updatedAt;
        private String createdBy;
        private String updatedBy;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeWindowDto {
        private String start;
        private String end;
        /** ISO-8601 days (1=Mon..7=Sun). Null/empty = every day. */
        private List<Integer> daysOfWeek;
    }
}
