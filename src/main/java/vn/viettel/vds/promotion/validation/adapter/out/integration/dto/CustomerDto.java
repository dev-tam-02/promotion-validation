package vn.viettel.vds.promotion.validation.adapter.out.integration.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.Map;

@Schema(description = "Customer data for rule execution")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomerDto {

    @Schema(description = "Customer identifier", example = "cust123", required = true)
    @NotBlank(message = "Customer ID is required")
    @JsonProperty("id")
    private String id;

    @Schema(description = "Customer segments", example = "[\"VIP\", \"GOLD\"]")
    @JsonProperty("segments")
    private List<String> segments;

    @Schema(description = "Customer region", example = "HCM")
    @JsonProperty("region")
    private String region;

    @Schema(description = "Customer tier", example = "3")
    @JsonProperty("tier")
    private Integer tier;

    @Schema(description = "Customer metadata")
    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    // Constructors
    public CustomerDto() {}

    public CustomerDto(String id) {
        this.id = id;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public List<String> getSegments() { return segments; }
    public void setSegments(List<String> segments) { this.segments = segments; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public Integer getTier() { return tier; }
    public void setTier(Integer tier) { this.tier = tier; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
}