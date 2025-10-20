package vn.viettel.vds.promotion.validation.domain.dto.command;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Command to assign and configure validation rules for campaigns
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SettingValidationRuleCommand {

    @JsonProperty("id")
    private String id;

    @JsonProperty("type")
    private String type;

    @JsonProperty("source")
    private String source;

    @JsonProperty("subject")
    private String subject;

    @JsonProperty("occurredAt")
    private Instant occurredAt;

    @JsonProperty("version")
    private Integer version;

    @JsonProperty("payload")
    private SettingValidationRuleCommandPayload payload;

    @JsonProperty("metadata")
    private Map<String, String> metadata;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SettingValidationRuleCommandPayload {

        @JsonProperty("assignRule")
        private RuleAssignment assignRule;

        @JsonProperty("applicableTo")
        private ApplicabilityScope applicableTo;

        @JsonProperty("timeframe")
        private TimeFrame timeframe;

        @JsonProperty("priority")
        @Builder.Default
        private Integer priority = 0;

        @JsonProperty("notes")
        private String notes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RuleAssignment {

        @JsonProperty("ruleId")
        private String ruleId;

        @JsonProperty("assignmentId")
        private String assignmentId;

        @JsonProperty("active")
        @Builder.Default
        private Boolean active = true;

        @JsonProperty("trafficPercent")
        @Builder.Default
        private Integer trafficPercent = 100;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ApplicabilityScope {

        @JsonProperty("included")
        private List<ApplicabilityRule> included;

        @JsonProperty("excluded")
        private List<ApplicabilityRule> excluded;

        @JsonProperty("includedAll")
        @Builder.Default
        private Boolean includedAll = false;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ApplicabilityRule {

        @JsonProperty("object")
        private ObjectType object;

        @JsonProperty("id")
        private String id;

        @JsonProperty("effect")
        @Builder.Default
        private EffectType effect = EffectType.APPLY_TO_EVERY;

        @JsonProperty("target")
        @Builder.Default
        private TargetType target = TargetType.ITEM;

        @JsonProperty("skipInitially")
        @Builder.Default
        private Integer skipInitially = 0;

        @JsonProperty("repeat")
        @Builder.Default
        private Integer repeat = 1;
    }

    public enum ObjectType {
        COLLECTION,
        PRODUCT,
        SKU
    }

    public enum EffectType {
        APPLY_TO_EVERY,
        APPLY_TO_CHEAPEST,
        APPLY_TO_MOST_EXPENSIVE
    }

    public enum TargetType {
        ITEM,
        ORDER,
        CUSTOMER
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TimeFrame {

        @JsonProperty("validityTimeframe")
        private ValidityTimeframe validityTimeframe;

        @JsonProperty("validityDaysOfWeek")
        private List<Integer> validityDaysOfWeek;

        @JsonProperty("validityHoursPerDay")
        private List<ValidityHoursPerDay> validityHoursPerDay;

        @JsonProperty("timeFrameId")
        private String timeFrameId;

        @JsonProperty("mode")
        @Builder.Default
        private TimeFrameMode mode = TimeFrameMode.ALLOW;

        @JsonProperty("timezone")
        @Builder.Default
        private String timezone = "UTC";
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ValidityTimeframe {

        @JsonProperty("startDate")
        private Instant startDate;

        @JsonProperty("expirationDate")
        private Instant expirationDate;

        @JsonProperty("interval")
        private String interval;

        @JsonProperty("duration")
        private String duration;

        @JsonProperty("activityDurationAfterPublishing")
        private String activityDurationAfterPublishing;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ValidityHoursPerDay {

        @JsonProperty("dayOfWeek")
        private Integer dayOfWeek;

        @JsonProperty("startTime")
        private String startTime;

        @JsonProperty("expirationTime")
        private String expirationTime;
    }

    public enum TimeFrameMode {
        ALLOW,
        DENY
    }
}
