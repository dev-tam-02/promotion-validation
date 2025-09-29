package vn.viettel.vds.promotion.validation.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

@Schema(description = "Operator response")
public class OperatorResponse {

    @Schema(description = "Operator ID", example = "order.total.gte@2")
    @JsonProperty("operatorId")
    private String operatorId;

    @Schema(description = "Tenant ID", example = "t1")
    @JsonProperty("tenantId")
    private String tenantId;

    @Schema(description = "Operator name", example = "order.total.gte")
    @JsonProperty("name")
    private String name;

    @Schema(description = "Operator version", example = "2")
    @JsonProperty("version")
    private Integer version;

    @Schema(description = "Context/domain", example = "order")
    @JsonProperty("context")
    private String context;

    @Schema(description = "JSON Schema for parameter validation")
    @JsonProperty("jsonSchema")
    private Map<String, Object> jsonSchema;

    @Schema(description = "Compiler ID for code generation", example = "tpl_order_total_gte_v2")
    @JsonProperty("compilerId")
    private String compilerId;

    @Schema(description = "Operator status", example = "active", allowableValues = {"active", "deprecated"})
    @JsonProperty("status")
    private String status;

    @Schema(description = "Creation timestamp")
    @JsonProperty("createdAt")
    private Instant createdAt;

    @Schema(description = "Last update timestamp")
    @JsonProperty("updatedAt")
    private Instant updatedAt;

    // Getters and setters
    public String getOperatorId() { return operatorId; }
    public void setOperatorId(String operatorId) { this.operatorId = operatorId; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    public String getContext() { return context; }
    public void setContext(String context) { this.context = context; }

    public Map<String, Object> getJsonSchema() { return jsonSchema; }
    public void setJsonSchema(Map<String, Object> jsonSchema) { this.jsonSchema = jsonSchema; }

    public String getCompilerId() { return compilerId; }
    public void setCompilerId(String compilerId) { this.compilerId = compilerId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}