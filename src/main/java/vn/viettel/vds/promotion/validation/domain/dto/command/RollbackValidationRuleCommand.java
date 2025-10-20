package vn.viettel.vds.promotion.validation.domain.dto.command;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Command to rollback/delete validation rule assignment for saga compensation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RollbackValidationRuleCommand {

    @JsonProperty("id")
    private String id;

    @JsonProperty("type")
    @Builder.Default
    private String type = "RollbackValidationRuleCommand";

    @JsonProperty("source")
    private String source;

    @JsonProperty("subject")
    private String subject;

    @JsonProperty("occurredAt")
    private Instant occurredAt;

    @JsonProperty("version")
    @Builder.Default
    private Integer version = 1;

    @JsonProperty("payload")
    private RollbackValidationRuleCommandPayload payload;

    @JsonProperty("metadata")
    private Map<String, String> metadata;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RollbackValidationRuleCommandPayload {

        @JsonProperty("campaignId")
        private String campaignId;

        @JsonProperty("validationRuleId")
        private String validationRuleId;

        @JsonProperty("rollbackReason")
        private String rollbackReason;

        @JsonProperty("correlationId")
        private String correlationId;

        @JsonProperty("timestamp")
        private Instant timestamp;

        @JsonProperty("rollbackAll")
        @Builder.Default
        private Boolean rollbackAll = true;
    }
}
