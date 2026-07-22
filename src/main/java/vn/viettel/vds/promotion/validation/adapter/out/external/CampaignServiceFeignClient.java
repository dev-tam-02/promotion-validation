package vn.viettel.vds.promotion.validation.adapter.out.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import vn.viettel.vds.promotion.validation.adapter.out.external.dto.CampaignStartTimesDto;

import java.util.List;

/**
 * Feign client for Campaign service.
 *
 * <p>pp-campaign chỉ còn phục vụ {@code /api/v2/campaigns/**} dưới context path
 * {@code /promotion/promotion-campaign} — path {@code /campaign/v1/...} cũ đã bị gỡ
 * (trả 404), nên khai báo ở đây bám theo contract v2 thật.</p>
 */
@FeignClient(
        name = "campaign-service",
        url = "${external.services.campaign.url}",
        path = "/promotion/promotion-campaign",
        configuration = ExternalServiceFeignConfig.class,
        fallback = CampaignServiceFeignClientFallback.class
)
public interface CampaignServiceFeignClient {

    /**
     * Check if campaign exists by ID.
     *
     * @param campaignId the campaign ID to check
     * @return true if campaign exists, false otherwise
     */
    @GetMapping("/api/v2/campaigns/{campaignId}/exists")
    Boolean existsById(@PathVariable("campaignId") String campaignId);

    /**
     * Batch lookup of campaign effective start times (PROM-1112).
     * IDs không tồn tại được pp-campaign bỏ qua, không báo lỗi.
     *
     * @param ids campaign IDs (tối đa 200 theo giới hạn của pp-campaign)
     * @return payload đã bọc {@code data} của pp-campaign
     */
    @GetMapping("/api/v2/campaigns/start-times")
    CampaignStartTimesDto getStartTimes(@RequestParam("ids") List<String> ids);
}
