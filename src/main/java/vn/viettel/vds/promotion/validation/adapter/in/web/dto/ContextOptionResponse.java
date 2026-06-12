package vn.viettel.vds.promotion.validation.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(description = "Context option for validation rules")
public record ContextOptionResponse(
        @Schema(description = "Context code", example = "GENERAL_USAGE")
        @JsonProperty("value")
        String value,

        @Schema(description = "Localized display labels keyed by BCP-47 locale tag",
                example = "{\"vi\":\"Sử dụng chung\",\"en\":\"General usage\"}")
        @JsonProperty("labels")
        Map<String, String> labels
) {
}
