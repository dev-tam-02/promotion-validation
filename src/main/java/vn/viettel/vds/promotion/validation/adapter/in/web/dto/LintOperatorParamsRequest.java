package vn.viettel.vds.promotion.validation.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.Map;

@Schema(description = "Request to validate operator parameters")
public class LintOperatorParamsRequest {

    @Schema(description = "Tenant ID", example = "t1")
    @JsonProperty("tenantId")
    private String tenantId;

    @Schema(description = "Operator name", example = "order.total.gte", required = true)
    @NotBlank(message = "Operator name is required")
    @JsonProperty("name")
    private String name;

    @Schema(description = "Operator version", example = "1", required = true)
    @NotNull(message = "Version is required")
    @Positive(message = "Version must be positive")
    @JsonProperty("version")
    private Integer version;

    @Schema(description = "Parameters to validate", example = "{\"amount\": 500000, \"currency\": \"VND\"}", required = true)
    @NotNull(message = "Parameters are required")
    @JsonProperty("params")
    private Map<String, Object> params;

    // Getters and setters
    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }
}