package vn.viettel.vds.promotion.validation.adapter.out.integration.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Bundle status response")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BundleStatusResponse {

    @Schema(description = "Bundle hash", example = "sha256:abc123...", required = true)
    @JsonProperty("bundleHash")
    private String bundleHash;

    @Schema(description = "Bundle loaded status", example = "true", required = true)
    @JsonProperty("loaded")
    private boolean loaded;

    @Schema(description = "Bundle health status", example = "HEALTHY")
    @JsonProperty("health")
    private String health;

    @Schema(description = "Additional status information")
    @JsonProperty("info")
    private String info;

    // Constructors
    public BundleStatusResponse() {}

    public BundleStatusResponse(String bundleHash, boolean loaded) {
        this.bundleHash = bundleHash;
        this.loaded = loaded;
    }

    // Getters and setters
    public String getBundleHash() { return bundleHash; }
    public void setBundleHash(String bundleHash) { this.bundleHash = bundleHash; }

    public boolean isLoaded() { return loaded; }
    public void setLoaded(boolean loaded) { this.loaded = loaded; }

    public String getHealth() { return health; }
    public void setHealth(String health) { this.health = health; }

    public String getInfo() { return info; }
    public void setInfo(String info) { this.info = info; }
}