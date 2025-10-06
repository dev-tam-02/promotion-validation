package vn.viettel.vds.promotion.validation.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

@Schema(description = "Request to update an existing rule")
public class UpdateRuleRequest {

    @Schema(description = "Rule name")
    @Size(max = 255, message = "Rule name must not exceed 255 characters")
    @JsonProperty("name")
    private String name;

    @Schema(description = "Root logic operator", allowableValues = {"ALL", "ANY", "NONE"})
    @JsonProperty("logic")
    private String logic;

    @Schema(description = "Rule limits configuration", example = "{\"perCodeTotal\": 1000, \"perCustomer\": 3}")
    @JsonProperty("limits")
    private Map<String, Object> limits;

    @Schema(description = "Rule nodes (conditions and groups)")
    @Valid
    @JsonProperty("nodes")
    private List<RuleNodeDto> nodes;

    @Schema(description = "Additional notes or comments")
    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    @JsonProperty("notes")
    private String notes;

    // Getters and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLogic() {
        return logic;
    }

    public void setLogic(String logic) {
        this.logic = logic;
    }

    public Map<String, Object> getLimits() {
        return limits;
    }

    public void setLimits(Map<String, Object> limits) {
        this.limits = limits;
    }

    public List<RuleNodeDto> getNodes() {
        return nodes;
    }

    public void setNodes(List<RuleNodeDto> nodes) {
        this.nodes = nodes;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}