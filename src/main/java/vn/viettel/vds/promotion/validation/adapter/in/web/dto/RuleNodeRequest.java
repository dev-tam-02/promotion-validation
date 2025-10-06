package vn.viettel.vds.promotion.validation.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

@Schema(description = "Rule node request for creating/updating rules")
public record RuleNodeRequest(
        @Schema(description = "Unique node identifier", example = "n1")
        @JsonProperty("id")
        String id,

        @Schema(description = "Node type", example = "COND", allowableValues = {"GROUP", "COND"})
        @JsonProperty("type")
        String type,

        @Schema(description = "Group logic (required for GROUP nodes)", example = "ALL", allowableValues = {"ALL", "ANY", "NONE"})
        @JsonProperty("groupLogic")
        String groupLogic,

        @Schema(description = "Operator name (required for COND nodes)", example = "order.total.gte")
        @JsonProperty("operatorName")
        String operatorName,

        @Schema(description = "Operator version (optional, defaults to latest)", example = "1")
        @JsonProperty("operatorVersion")
        Integer operatorVersion,

        @Schema(description = "Operator parameters (required for COND nodes)",
                example = "{\"amount\": 500000, \"currency\": \"VND\"}")
        @JsonProperty("params")
        Map<String, Object> params,

        @Schema(description = "Reason code (required for COND nodes)", example = "ORDER_TOTAL_MIN")
        @JsonProperty("reasonCode")
        String reasonCode,

        @Schema(description = "Child node IDs (for GROUP nodes)", example = "[\"n2\", \"n3\"]")
        @JsonProperty("children")
        List<String> children,

        @Schema(description = "Display order", example = "1")
        @JsonProperty("order")
        Integer order
) {
}