package vn.viettel.vds.promotion.validation.adapter.out.external;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import vn.viettel.vds.promotion.validation.adapter.out.external.dto.CampaignStartTimesDto;
import vn.viettel.vds.promotion.validation.adapter.out.external.dto.CampaignStatusesDto;
import vn.viettel.vds.promotion.validation.application.port.out.CampaignSchedulePort;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads campaign start times from pp-campaign for the rule-editability check
 * (PROM-1112).
 *
 * <p>Chia lô theo giới hạn 200 id/lần của endpoint {@code /api/v2/campaigns/start-times}.
 * Lỗi gọi service không được ném ra ngoài: màn danh sách quy tắc vẫn phải trả về
 * được, chỉ là mọi quy tắc đã gán sẽ bị coi là không sửa được (fail closed).</p>
 */
@Service
public class CampaignScheduleAdapter implements CampaignSchedulePort {

    private static final Logger log = LoggerFactory.getLogger(CampaignScheduleAdapter.class);

    /** Mirrors the {@code @Size(max = 200)} cap on the pp-campaign endpoint. */
    private static final int BATCH_SIZE = 200;

    private final CampaignServiceFeignClient campaignClient;

    public CampaignScheduleAdapter(CampaignServiceFeignClient campaignClient) {
        this.campaignClient = campaignClient;
    }

    @Override
    public Map<String, Instant> findStartTimes(Collection<String> campaignIds) {
        if (campaignIds == null || campaignIds.isEmpty()) {
            return Map.of();
        }

        List<String> ids = campaignIds.stream()
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Map.of();
        }

        Map<String, Instant> startTimes = new HashMap<>();
        for (int from = 0; from < ids.size(); from += BATCH_SIZE) {
            List<String> batch = ids.subList(from, Math.min(from + BATCH_SIZE, ids.size()));
            collectBatch(batch, startTimes);
        }
        return startTimes;
    }

    private void collectBatch(List<String> batch, Map<String, Instant> target) {
        try {
            CampaignStartTimesDto response = campaignClient.getStartTimes(batch);
            if (response == null || response.data() == null) {
                return;
            }
            for (CampaignStartTimesDto.Item item : response.data()) {
                if (item != null && item.id() != null && item.startFrom() != null) {
                    target.put(item.id(), item.startFrom());
                }
            }
        } catch (Exception e) {
            // Fail closed — an unresolved campaign is treated as already effective.
            log.warn("Failed to fetch campaign start times for {} id(s): {}", batch.size(), e.getMessage());
        }
    }

    @Override
    public Map<String, String> findStatuses(Collection<String> campaignIds) {
        if (campaignIds == null || campaignIds.isEmpty()) {
            return Map.of();
        }

        List<String> ids = campaignIds.stream()
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Map.of();
        }

        Map<String, String> statuses = new HashMap<>();
        for (int from = 0; from < ids.size(); from += BATCH_SIZE) {
            List<String> batch = ids.subList(from, Math.min(from + BATCH_SIZE, ids.size()));
            collectStatusBatch(batch, statuses);
        }
        return statuses;
    }

    private void collectStatusBatch(List<String> batch, Map<String, String> target) {
        try {
            CampaignStatusesDto response = campaignClient.getStatuses(batch);
            if (response == null || response.data() == null) {
                return;
            }
            for (CampaignStatusesDto.Item item : response.data()) {
                if (item != null && item.id() != null && item.status() != null) {
                    target.put(item.id(), item.status());
                }
            }
        } catch (Exception e) {
            // Fail closed — an unresolved status is treated as "already live / locked".
            log.warn("Failed to fetch campaign statuses for {} id(s): {}", batch.size(), e.getMessage());
        }
    }
}
