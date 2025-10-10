package vn.viettel.vds.promotion.validation.adapter.out.integration.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Engine information from compilation response.
 * Contains details about the rules engine used for compilation.
 */
@Schema(description = "Rules engine information")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EngineInfo {

    @Schema(description = "Engine type", example = "drools")
    @JsonProperty("type")
    private String type;

    @Schema(description = "Drools version", example = "10.1.0")
    @JsonProperty("droolsVersion")
    private String droolsVersion;

    // Constructors
    public EngineInfo() {
    }

    public EngineInfo(String type, String droolsVersion) {
        this.type = type;
        this.droolsVersion = droolsVersion;
    }

    // Getters and setters
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

    @Override
    public String toString() {
        return "EngineInfo{" +
                "type='" + type + '\'' +
                ", droolsVersion='" + droolsVersion + '\'' +
                '}';
    }
}
