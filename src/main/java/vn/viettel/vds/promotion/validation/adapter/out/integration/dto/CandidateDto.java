package vn.viettel.vds.promotion.validation.adapter.out.integration.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.util.Map;

@Schema(description = "Candidate data for rule execution")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CandidateDto(
        @Schema(description = "Candidate identifier", example = "SAVE20", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Candidate ID is required")
        @JsonProperty("id")
        String id,

        @Schema(description = "Candidate type", example = "VOUCHER")
        @JsonProperty("type")
        String type,

        @Schema(description = "Candidate metadata")
        @JsonProperty("metadata")
        Map<String, Object> metadata
) {
}
