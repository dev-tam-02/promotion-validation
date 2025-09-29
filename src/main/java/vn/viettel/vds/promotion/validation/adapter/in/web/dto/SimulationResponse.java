package vn.viettel.vds.promotion.validation.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Rule simulation result")
public class SimulationResponse {

    @Schema(description = "Final decision", example = "allow", allowableValues = {"allow", "deny", "error"})
    @JsonProperty("decision")
    private String decision;

    @Schema(description = "Reason codes for denial", example = "[\"ORDER_TOTAL_MIN\", \"TIME_WINDOW\"]")
    @JsonProperty("reasonCodes")
    private List<String> reasonCodes;

    @Schema(description = "Detailed execution explanation")
    @JsonProperty("explain")
    private List<String> explain;

    // Getters and setters
    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }

    public List<String> getReasonCodes() { return reasonCodes; }
    public void setReasonCodes(List<String> reasonCodes) { this.reasonCodes = reasonCodes; }

    public List<String> getExplain() { return explain; }
    public void setExplain(List<String> explain) { this.explain = explain; }
}