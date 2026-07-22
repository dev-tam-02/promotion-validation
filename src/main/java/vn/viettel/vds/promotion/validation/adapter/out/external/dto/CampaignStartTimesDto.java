package vn.viettel.vds.promotion.validation.adapter.out.external.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;

/**
 * DTO for {@code GET /api/v2/campaigns/start-times?ids=...} (pp-campaign response).
 *
 * <p>pp-campaign bọc payload trong {@code data} (@ResponseWrapper), nên lớp ngoài
 * chỉ giữ đúng field đó.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CampaignStartTimesDto(
        @JsonProperty("data") List<Item> data
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(
            @JsonProperty("id") String id,
            @JsonProperty("startFrom") Instant startFrom
    ) {
    }
}
