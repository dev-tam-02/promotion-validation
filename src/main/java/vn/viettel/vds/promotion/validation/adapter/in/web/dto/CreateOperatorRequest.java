package vn.viettel.vds.promotion.validation.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.Map;

@Schema(description = "Request to create a new operator")
public class CreateOperatorRequest {

    @Schema(description = "Operator name", example = "order.total.gte", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Operator name is required")
    @JsonProperty("name")
    private String name;

    @Schema(description = "Operator version", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Version is required")
    @Positive(message = "Version must be positive")
    @JsonProperty("version")
    private Integer version;

    @Schema(description = "Context/domain", example = "order", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Context is required")
    @JsonProperty("context")
    private String context;

    @Schema(description = "JSON Schema for parameter validation", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "JSON Schema is required")
    @JsonProperty("jsonSchema")
    private Map<String, Object> jsonSchema;

    @Schema(description = "Compiler ID for code generation", example = "tpl_order_total_gte_v2", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Compiler ID is required")
    @JsonProperty("compilerId")
    private String compilerId;

    @Schema(description = "Operator status", example = "active", allowableValues = {"active", "deprecated"})
    @JsonProperty("status")
    private String status = "active";

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

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public Map<String, Object> getJsonSchema() {
        return jsonSchema;
    }

    public void setJsonSchema(Map<String, Object> jsonSchema) {
        this.jsonSchema = jsonSchema;
    }

    public String getCompilerId() {
        return compilerId;
    }

    public void setCompilerId(String compilerId) {
        this.compilerId = compilerId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}