package vn.viettel.vds.promotion.validation.adapter.out.integration.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Bundle warmup request")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WarmupRequest {

    @Schema(description = "Bundle hash to warm up", example = "sha256:abc123...", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Bundle hash is required")
    @JsonProperty("bundleHash")
    private String bundleHash;

    @Schema(description = "Artifact bytes for warmup")
    @JsonProperty("artifactBytes")
    private byte[] artifactBytes;

    // Constructors
    public WarmupRequest() {
    }

    public WarmupRequest(String bundleHash) {
        this.bundleHash = bundleHash;
    }

    public WarmupRequest(String bundleHash, byte[] artifactBytes) {
        this.bundleHash = bundleHash;
        this.artifactBytes = artifactBytes;
    }

    // Getters and setters
    public String getBundleHash() {
        return bundleHash;
    }

    public void setBundleHash(String bundleHash) {
        this.bundleHash = bundleHash;
    }

    public byte[] getArtifactBytes() {
        return artifactBytes;
    }

    public void setArtifactBytes(byte[] artifactBytes) {
        this.artifactBytes = artifactBytes;
    }
}
