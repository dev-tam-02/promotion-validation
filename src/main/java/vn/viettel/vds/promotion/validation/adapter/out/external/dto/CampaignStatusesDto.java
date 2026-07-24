package vn.viettel.vds.promotion.validation.adapter.out.external.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * DTO for {@code GET /api/v2/campaigns/statuses?ids=...} (pp-campaign response).
 *
 * <p>pp-campaign bọc payload trong {@code data} (@ResponseWrapper), nên lớp ngoài
 * chỉ giữ đúng field đó. Mỗi phần tử là {id, status, version}; ở đây chỉ cần
 * {@code id} và {@code status} cho bài toán editability (PROM-1369).</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CampaignStatusesDto(
        @JsonProperty("data") List<Item> data
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(
            @JsonProperty("id") String id,
            @JsonProperty("status") String status
    ) {
    }
}
