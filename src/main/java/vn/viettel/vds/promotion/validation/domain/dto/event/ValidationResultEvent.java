package vn.viettel.vds.promotion.validation.domain.dto.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Event published with validation results for cashback eligibility
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ValidationResultEvent {

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
    private ValidationResultEventPayload payload;

    @JsonProperty("metadata")
    private Map<String, String> metadata;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ValidationResultEventPayload {

        @JsonProperty("transactionId")
        private String transactionId;

        @JsonProperty("cashbackId")
        private String cashbackId;

        @JsonProperty("campaignId")
        private String campaignId;

        @JsonProperty("customerId")
        private String customerId;

        @JsonProperty("orderId")
        private String orderId;

        @JsonProperty("isValid")
        private Boolean isValid;

        @JsonProperty("validationStatus")
        private ValidationStatus validationStatus;

        @JsonProperty("validatedRules")
        private List<RuleValidationResult> validatedRules;

        @JsonProperty("eligibleCashbackAmount")
        private BigDecimal eligibleCashbackAmount;

        @JsonProperty("currency")
        private String currency;

        @JsonProperty("errorMessage")
        private String errorMessage;

        @JsonProperty("validationMessages")
        @Builder.Default
        private List<String> validationMessages = new ArrayList<>();

        @JsonProperty("failureReasons")
        @Builder.Default
        private List<String> failureReasons = new ArrayList<>();

        @JsonProperty("validationSummary")
        private String validationSummary;

        @JsonProperty("correlationId")
        private String correlationId;

        @JsonProperty("validatedAt")
        private Instant validatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RuleValidationResult {

        @JsonProperty("ruleId")
        private String ruleId;

        @JsonProperty("ruleName")
        private String ruleName;

        @JsonProperty("isPassed")
        private Boolean isPassed;

        @JsonProperty("failureReason")
        private String failureReason;

        @JsonProperty("actualValue")
        private String actualValue;

        @JsonProperty("expectedValue")
        private String expectedValue;
    }

    public enum ValidationStatus {
        PASSED,
        FAILED,
        PARTIAL,
        ERROR
    }
}
