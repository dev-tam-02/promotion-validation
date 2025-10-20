package vn.viettel.vds.promotion.validation.domain.dto.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Event published when validation rule assignment is processed
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SettingValidationRuleEvent {

    @JsonProperty("id")
    private String id;

    @JsonProperty("aggregate")
    private String aggregate;

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
    private SettingValidationRuleEventPayload payload;

    @JsonProperty("metadata")
    private Map<String, String> metadata;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SettingValidationRuleEventPayload {

        @JsonProperty("commandId")
        private String commandId;

        @JsonProperty("isSuccess")
        private Boolean isSuccess;

        @JsonProperty("errorCode")
        private String errorCode;

        @JsonProperty("errorMessage")
        private String errorMessage;

        @JsonProperty("assignmentResult")
        private AssignmentResult assignmentResult;

        @JsonProperty("applicabilityResult")
        private ApplicabilityResult applicabilityResult;

        @JsonProperty("timeframeResult")
        private TimeframeResult timeframeResult;

        @JsonProperty("processedBy")
        private String processedBy;

        @JsonProperty("processedAt")
        private Instant processedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AssignmentResult {

        @JsonProperty("assignmentId")
        private String assignmentId;

        @JsonProperty("ruleId")
        private String ruleId;

        @JsonProperty("active")
        private Boolean active;

        @JsonProperty("trafficPercent")
        private Integer trafficPercent;

        @JsonProperty("priority")
        private Integer priority;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ApplicabilityResult {

        @JsonProperty("subjectType")
        private String subjectType;

        @JsonProperty("subjectKey")
        private String subjectKey;

        @JsonProperty("includedItemsCount")
        private Integer includedItemsCount;

        @JsonProperty("excludedItemsCount")
        private Integer excludedItemsCount;

        @JsonProperty("includedAll")
        private Boolean includedAll;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TimeframeResult {

        @JsonProperty("timeFrameId")
        private String timeFrameId;

        @JsonProperty("validFrom")
        private Instant validFrom;

        @JsonProperty("validTo")
        private Instant validTo;

        @JsonProperty("mode")
        private String mode;

        @JsonProperty("timezone")
        private String timezone;
    }
}
