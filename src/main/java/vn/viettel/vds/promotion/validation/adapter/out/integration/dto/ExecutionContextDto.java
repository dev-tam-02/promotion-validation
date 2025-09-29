package vn.viettel.vds.promotion.validation.adapter.out.integration.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Map;

@Schema(description = "Execution context for rule evaluation")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExecutionContextDto {

    @Schema(description = "Current timestamp", example = "2025-09-20T10:00:00Z", required = true)
    @NotNull(message = "Now timestamp is required")
    @JsonProperty("now")
    private Instant now;

    @Schema(description = "Timezone for evaluation", example = "Asia/Bangkok", required = true)
    @NotBlank(message = "Timezone is required")
    @JsonProperty("timezone")
    private String timezone;

    @Schema(description = "Tenant identifier", example = "tenant1")
    @JsonProperty("tenantId")
    private String tenantId;

    @Schema(description = "Session identifier", example = "session123")
    @JsonProperty("sessionId")
    private String sessionId;

    @Schema(description = "Additional variables")
    @JsonProperty("variables")
    private Map<String, Object> variables;

    // Constructors
    public ExecutionContextDto() {}

    public ExecutionContextDto(Instant now, String timezone) {
        this.now = now;
        this.timezone = timezone;
    }

    // Getters and setters
    public Instant getNow() { return now; }
    public void setNow(Instant now) { this.now = now; }

    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public Map<String, Object> getVariables() { return variables; }
    public void setVariables(Map<String, Object> variables) { this.variables = variables; }
}