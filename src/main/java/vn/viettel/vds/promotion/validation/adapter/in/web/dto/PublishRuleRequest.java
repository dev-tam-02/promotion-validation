package vn.viettel.vds.promotion.validation.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request to publish a rule")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PublishRuleRequest {

    @Schema(description = "Force republish even if already published", example = "false")
    @JsonProperty("force")
    private Boolean force = false;

    @Schema(description = "Skip verification step", example = "false")
    @JsonProperty("skipVerification")
    private Boolean skipVerification = false;

    @Schema(description = "Additional publishing options")
    @JsonProperty("options")
    private PublishOptions options;

    // Constructors
    public PublishRuleRequest() {}

    public PublishRuleRequest(Boolean force, Boolean skipVerification) {
        this.force = force;
        this.skipVerification = skipVerification;
    }

    // Getters and setters
    public Boolean getForce() { return force; }
    public void setForce(Boolean force) { this.force = force; }

    public Boolean getSkipVerification() { return skipVerification; }
    public void setSkipVerification(Boolean skipVerification) { this.skipVerification = skipVerification; }

    public PublishOptions getOptions() { return options; }
    public void setOptions(PublishOptions options) { this.options = options; }

    public static class PublishOptions {
        @Schema(description = "Timeout for compilation in seconds", example = "60")
        @JsonProperty("compilationTimeout")
        private Integer compilationTimeout;

        @Schema(description = "Timeout for verification in seconds", example = "30")
        @JsonProperty("verificationTimeout")
        private Integer verificationTimeout;

        // Constructors
        public PublishOptions() {}

        // Getters and setters
        public Integer getCompilationTimeout() { return compilationTimeout; }
        public void setCompilationTimeout(Integer compilationTimeout) { this.compilationTimeout = compilationTimeout; }

        public Integer getVerificationTimeout() { return verificationTimeout; }
        public void setVerificationTimeout(Integer verificationTimeout) { this.verificationTimeout = verificationTimeout; }
    }
}