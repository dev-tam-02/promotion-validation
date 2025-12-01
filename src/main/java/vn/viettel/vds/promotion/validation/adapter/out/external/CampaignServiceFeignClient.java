package vn.viettel.vds.promotion.validation.adapter.out.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign client for Campaign service.
 * Used to verify campaign existence for delete assignment operation (SRS PRM_KBNV_API_VALD008).
 */
@FeignClient(
        name = "campaign-service",
        url = "${external.services.campaign.url:http://localhost:8083}",
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
    @GetMapping("/campaign/v1/campaigns/{campaignId}/exists")
    Boolean existsById(@PathVariable("campaignId") String campaignId);
}
