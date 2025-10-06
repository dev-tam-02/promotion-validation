package vn.viettel.vds.promotion.validation.adapter.out.integration.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.util.Map;

@Schema(description = "Candidate data for rule execution")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CandidateDto {

    @Schema(description = "Candidate identifier", example = "SAVE20", required = true)
    @NotBlank(message = "Candidate ID is required")
    @JsonProperty("id")
    private String id;

    @Schema(description = "Candidate type", example = "VOUCHER")
    @JsonProperty("type")
    private String type;

    @Schema(description = "Candidate metadata")
    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    // Constructors
    public CandidateDto() {
    }

    public CandidateDto(String id) {
        this.id = id;
    }

    public CandidateDto(String id, String type) {
        this.id = id;
        this.type = type;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}