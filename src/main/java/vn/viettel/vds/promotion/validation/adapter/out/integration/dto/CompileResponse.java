package vn.viettel.vds.promotion.validation.adapter.out.integration.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Rule compilation response")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CompileResponse {

    @Schema(description = "Compilation success status", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("ok")
    private boolean ok;

    @Schema(description = "Bundle hash identifier", example = "sha256:abc123...")
    @JsonProperty("bundleHash")
    private String bundleHash;

    @Schema(description = "Compiled artifact bytes")
    @JsonProperty("artifactBytes")
    private byte[] artifactBytes;

    @Schema(description = "Artifact size in bytes", example = "1024")
    @JsonProperty("artifactSize")
    private Long artifactSize;

    @Schema(description = "Compilation logs")
    @JsonProperty("logs")
    private List<String> logs;

    @Schema(description = "Engine version", example = "drools-10.1.0")
    @JsonProperty("engineVersion")
    private String engineVersion;

    @Schema(description = "Compilation errors (if any)")
    @JsonProperty("errors")
    private List<String> errors;

    // The following two fields mirror pp-rule-engine's CompileResponse wire contract
    // ({bundleHash, engine, size, logs, drlContent}). They are modelled explicitly so the
    // strict (fail-on-unknown) ObjectMapper can decode the engine's response instead of failing.
    @Schema(description = "Rule engine descriptor")
    @JsonProperty("engine")
    private Engine engine;

    @Schema(description = "Compiled bundle size in bytes", example = "3404")
    @JsonProperty("size")
    private Long size;

    private String drlContent;

    // Constructors
    public CompileResponse() {
    }

    public CompileResponse(boolean ok) {
        this.ok = ok;
    }

    // Getters and setters
    public boolean isOk() {
        return ok;
    }

    public void setOk(boolean ok) {
        this.ok = ok;
    }

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

    public Long getArtifactSize() {
        return artifactSize;
    }

    public void setArtifactSize(Long artifactSize) {
        this.artifactSize = artifactSize;
    }

    public List<String> getLogs() {
        return logs;
    }

    public void setLogs(List<String> logs) {
        this.logs = logs;
    }

    public String getEngineVersion() {
        return engineVersion;
    }

    public void setEngineVersion(String engineVersion) {
        this.engineVersion = engineVersion;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }


    public String getDrlContent() {
        return drlContent;
    }

    public void setDrlContent(String drlContent) {
        this.drlContent = drlContent;
    }

    public Engine getEngine() {
        return engine;
    }

    public void setEngine(Engine engine) {
        this.engine = engine;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    @Schema(description = "Rule engine descriptor (type + version)")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Engine {

        @JsonProperty("type")
        private String type;

        @JsonProperty("droolsVersion")
        private String droolsVersion;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getDroolsVersion() {
            return droolsVersion;
        }

        public void setDroolsVersion(String droolsVersion) {
            this.droolsVersion = droolsVersion;
        }
    }
}
