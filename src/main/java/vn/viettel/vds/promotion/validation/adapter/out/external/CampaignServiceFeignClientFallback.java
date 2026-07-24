package vn.viettel.vds.promotion.validation.adapter.out.external;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.adapter.out.external.dto.CampaignStartTimesDto;
import vn.viettel.vds.promotion.validation.adapter.out.external.dto.CampaignStatusesDto;

import java.util.List;

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

    @Override
    public CampaignStartTimesDto getStartTimes(List<String> ids) {
        logger.warn("Campaign service unavailable, using fallback for start-time lookup of {} id(s)", ids.size());
        // Fail closed: with no start time known the caller must assume every bound
        // campaign is already effective, so an assigned rule stays locked from editing.
        return new CampaignStartTimesDto(List.of());
    }

    @Override
    public CampaignStatusesDto getStatuses(List<String> ids) {
        logger.warn("Campaign service unavailable, using fallback for status lookup of {} id(s)", ids.size());
        // Fail closed: with no status known the caller must assume every bound campaign
        // is already live, so an assigned rule stays locked from editing.
        return new CampaignStatusesDto(List.of());
    }
}
