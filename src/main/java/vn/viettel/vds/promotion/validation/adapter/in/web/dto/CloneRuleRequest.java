package vn.viettel.vds.promotion.validation.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to clone an existing rule")
public class CloneRuleRequest {

    @Schema(description = "New rule code", example = "WEEKEND_VIP_500K_V2", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "New rule code is required")
    @Size(max = 100, message = "Rule code must not exceed 100 characters")
    @JsonProperty("newCode")
    private String newCode;

    @Schema(description = "New rule name", example = "Weekend VIP ≥500k promotion V2", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "New rule name is required")
    @Size(max = 255, message = "Rule name must not exceed 255 characters")
    @JsonProperty("newName")
    private String newName;

    // Getters and setters
    public String getNewCode() {
        return newCode;
    }

    public void setNewCode(String newCode) {
        this.newCode = newCode;
    }

    public String getNewName() {
        return newName;
    }

    public void setNewName(String newName) {
        this.newName = newName;
    }
}