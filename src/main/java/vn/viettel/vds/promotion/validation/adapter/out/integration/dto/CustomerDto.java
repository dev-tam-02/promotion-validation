package vn.viettel.vds.promotion.validation.adapter.out.integration.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.Map;

@Schema(description = "Customer data for rule execution")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CustomerDto(
        @Schema(description = "Customer identifier", example = "cust123", required = true)
        @NotBlank(message = "Customer ID is required")
        @JsonProperty("id")
        String id,

        @Schema(description = "Customer segments", example = "[\"VIP\", \"GOLD\"]")
        @JsonProperty("segments")
        List<String> segments,

        @Schema(description = "Customer region", example = "HCM")
        @JsonProperty("region")
        String region,

        @Schema(description = "Customer tier", example = "3")
        @JsonProperty("tier")
        Integer tier,

        @Schema(description = "Customer metadata")
        @JsonProperty("metadata")
        Map<String, Object> metadata
) {
}
