package vn.viettel.vds.promotion.validation.adapter.out.external;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Fallback implementation for CampaignServiceFeignClient.
 * Returns true by default to allow operation to proceed when campaign service is unavailable.
 */
@Component
public class CampaignServiceFeignClientFallback implements CampaignServiceFeignClient {

    private static final Logger logger = LoggerFactory.getLogger(CampaignServiceFeignClientFallback.class);

    @Override
    public Boolean existsById(String campaignId) {
        logger.warn("Campaign service unavailable, using fallback for campaign existence check: {}", campaignId);
        // Return true by default to allow operation to proceed
        // This is a graceful degradation - assignment will still be checked
        return true;
    }
}
