package vn.viettel.vds.promotion.validation.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Request to update operator status")
public class UpdateOperatorStatusRequest {

    @Schema(description = "New operator status", example = "deprecated", allowableValues = {"active", "deprecated"}, required = true)
    @NotBlank(message = "Status is required")
    @Pattern(regexp = "^(active|deprecated)$", message = "Status must be either 'active' or 'deprecated'")
    @JsonProperty("status")
    private String status;

    // Getters and setters
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}