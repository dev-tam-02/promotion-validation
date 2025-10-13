package vn.viettel.vds.promotion.validation.adapter.out.integration.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Warmup response")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WarmupResponse implements OperationResponse {

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

    /**
     * Alias for isSuccess() - checks if warmup was successful.
     * Provided for backward compatibility and semantic clarity.
     *
     * @return true if warmup succeeded
     */
    public boolean isOk() {
        return isSuccess();
    }

    /**
     * Alias setter for setSuccess().
     * Provided for backward compatibility.
     *
     * @param ok the success status
     */
    public void setOk(boolean ok) {
        setSuccess(ok);
    }

    /**
     * Returns empty list as warmup response doesn't track errors.
     * Errors are indicated by the success flag and message field.
     *
     * @return empty list
     */
    public java.util.List<String> getErrors() {
        return java.util.Collections.emptyList();
    }

    /**
     * No-op setter as warmup response doesn't track errors.
     * Errors should be set via the message field.
     *
     * @param errors ignored parameter
     */
    public void setErrors(java.util.List<String> errors) {
        // Intentionally empty - warmup doesn't have errors field
        // Errors should be set via the message field instead
    }
}