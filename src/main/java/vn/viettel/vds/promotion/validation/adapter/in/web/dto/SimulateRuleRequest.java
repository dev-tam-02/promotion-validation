package vn.viettel.vds.promotion.validation.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

@Schema(description = "Request to simulate rule execution")
public class SimulateRuleRequest {

    @Schema(description = "Rule version to simulate (null for latest draft)", example = "3")
    @JsonProperty("version")
    private Integer version;

    @Schema(description = "Context data for simulation", required = true)
    @NotNull(message = "Context is required")
    @JsonProperty("context")
    private Map<String, Object> context;

    @Schema(description = "Explanation level", example = "FAIL_ONLY", allowableValues = {"NONE", "FAIL_ONLY", "FULL"})
    @JsonProperty("explain")
    private String explain = "NONE";

    // Getters and setters
    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Map<String, Object> getContext() {
        return context;
    }

    public void setContext(Map<String, Object> context) {
        this.context = context;
    }

    public String getExplain() {
        return explain;
    }

    public void setExplain(String explain) {
        this.explain = explain;
    }
}