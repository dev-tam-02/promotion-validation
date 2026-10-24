package vn.viettel.vds.promotion.validation.application.port.out;

import vn.viettel.vds.promotion.campaign.archive.CampaignArchiveResultEventPayload;

/**
 * Phát kết quả lưu trữ lên topic {@code promotion_campaign_archive_result} (PRM_KBNV_EVT_ARCH_002 Mục 4.3).
 */
public interface CampaignArchiveResultPublisherPort {

    void publish(CampaignArchiveResultEventPayload payload);
}
