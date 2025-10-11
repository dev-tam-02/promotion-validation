package vn.viettel.vds.promotion.validation.adapter.out.integration.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

@Schema(description = "Rule execution response")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExecuteResponse {

    @Schema(description = "Execution success status", example = "true", required = true)
    @JsonProperty("ok")
    private boolean ok;

    @Schema(description = "Validation decision", example = "ALLOW", required = true, allowableValues = {"ALLOW", "DENY"})
    @JsonProperty("decision")
    private String decision;

    @Schema(description = "Reason codes")
    @JsonProperty("reasonCodes")
    private List<String> reasonCodes;

    @Schema(description = "Explanation details")
    @JsonProperty("explain")
    private List<String> explain;

    @Schema(description = "Engine execution details")
    @JsonProperty("engine")
    private Engine engine;

    @Schema(description = "Additional metadata")
    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    // Constructors
    public ExecuteResponse() {
    }

    public ExecuteResponse(boolean ok, String decision) {
        this.ok = ok;
        this.decision = decision;
    }

    // Getters and setters
    public boolean isOk() {
        return ok;
    }

    public void setOk(boolean ok) {
        this.ok = ok;
    }

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public List<String> getReasonCodes() {
        return reasonCodes;
    }

    public void setReasonCodes(List<String> reasonCodes) {
        this.reasonCodes = reasonCodes;
    }

    public List<String> getExplain() {
        return explain;
    }

    public void setExplain(List<String> explain) {
        this.explain = explain;
    }

    public Engine getEngine() {
        return engine;
    }

    public void setEngine(Engine engine) {
        this.engine = engine;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public static class Engine {

        @Schema(description = "Engine version", example = "drools-10.1.0")
        @JsonProperty("version")
        private String version;

        @Schema(description = "Execution latency in milliseconds", example = "50")
        @JsonProperty("latencyMs")
        private Long latencyMs;

        @Schema(description = "Cache hit status", example = "true")
        @JsonProperty("cacheHit")
        private Boolean cacheHit;

        // Constructors
        public Engine() {
            // Default constructor for JSON deserialization
        }

        // Getters and setters
        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public Long getLatencyMs() {
            return latencyMs;
        }

        public void setLatencyMs(Long latencyMs) {
            this.latencyMs = latencyMs;
        }

        public Boolean getCacheHit() {
            return cacheHit;
        }

        public void setCacheHit(Boolean cacheHit) {
            this.cacheHit = cacheHit;
        }
    }
}
