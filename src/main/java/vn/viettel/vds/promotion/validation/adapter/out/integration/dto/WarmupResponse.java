package vn.viettel.vds.promotion.validation.adapter.out.integration.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Warmup response")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WarmupResponse {

    @Schema(description = "Warmup success status", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("success")
    private boolean success;

    @Schema(description = "Response message", example = "Warmup completed successfully")
    @JsonProperty("message")
    private String message;

    @Schema(description = "Warmup duration in milliseconds", example = "1500")
    @JsonProperty("durationMs")
    private Long durationMs;

    // Constructors
    public WarmupResponse() {
    }

    public WarmupResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    // Getters and setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    public boolean isOk() {
        return success;
    }

    public void setOk(boolean ok) {
        this.success = ok;
    }

    public java.util.List<String> getErrors() {
        return java.util.Collections.emptyList();
    }

    public void setErrors(java.util.List<String> errors) { /* Ignored */ }
}