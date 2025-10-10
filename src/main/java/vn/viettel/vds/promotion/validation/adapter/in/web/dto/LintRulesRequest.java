package vn.viettel.vds.promotion.validation.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

@Schema(description = "Request to lint/validate rule structure")
public class LintRulesRequest {

    @Schema(description = "Rule nodes to validate", required = true)
    @NotEmpty(message = "Nodes list cannot be empty")
    @Valid
    @JsonProperty("nodes")
    private List<RuleNodeDto> nodes;

    // Getters and setters
    public List<RuleNodeDto> getNodes() {
        return nodes;
    }

    public void setNodes(List<RuleNodeDto> nodes) {
        this.nodes = nodes;
    }
}