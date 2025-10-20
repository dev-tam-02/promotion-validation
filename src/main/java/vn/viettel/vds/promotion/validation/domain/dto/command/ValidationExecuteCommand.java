package vn.viettel.vds.promotion.validation.domain.dto.command;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Command to execute validation for cashback eligibility
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ValidationExecuteCommand {

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
    private ValidationExecuteCommandPayload payload;

    @JsonProperty("metadata")
    private Map<String, String> metadata;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ValidationExecuteCommandPayload {

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

        @JsonProperty("orderAmount")
        private BigDecimal orderAmount;

        @JsonProperty("currency")
        private String currency;

        @JsonProperty("validationRules")
        private List<ValidationRule> validationRules;

        @JsonProperty("correlationId")
        private String correlationId;

        @JsonProperty("requestedAt")
        private Instant requestedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ValidationRule {

        @JsonProperty("ruleId")
        private String ruleId;

        @JsonProperty("ruleName")
        private String ruleName;

        @JsonProperty("ruleType")
        private RuleType ruleType;

        @JsonProperty("ruleValue")
        private String ruleValue;

        @JsonProperty("isActive")
        private Boolean isActive;
    }

    public enum RuleType {
        MIN_ORDER_AMOUNT,
        MAX_CASHBACK,
        USER_LIMIT,
        CAMPAIGN_BUDGET,
        TIME_RANGE,
        CUSTOMER_SEGMENT
    }
}
