package vn.viettel.vds.promotion.validation.adapter.in.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.campaign.archive.CampaignArchiveCommand;
import vn.viettel.vds.promotion.campaign.archive.CampaignArchiveCommandPayload;
import vn.viettel.vds.promotion.validation.application.service.archive.ArchiveCampaignRuleBindingService;

/**
 * Nhận lệnh lưu trữ chiến dịch từ {@code promotion_campaign_archive_command} (PRM_KBNV_EVT_ARCH_002 Bước 4),
 * consumer group riêng của Validation Service.
 */
@ConditionalOnProperty(prefix = "promix.messaging", name = "enabled", havingValue = "true")
@Component
public class CampaignArchiveCommandConsumer {

    private static final Logger logger = LoggerFactory.getLogger(CampaignArchiveCommandConsumer.class);

    private final ArchiveCampaignRuleBindingService archiveService;

    public CampaignArchiveCommandConsumer(ArchiveCampaignRuleBindingService archiveService) {
        this.archiveService = archiveService;
    }

    @KafkaListener(
            topics = "${kafka.topics.campaign-archive-command:promotion_campaign_archive_command}",
            groupId = "${kafka.consumer.campaign-archive-group-id:validation-campaign-archive}",
            containerFactory = "kafkaListenerContainerFactory",
            properties = {
                    "spring.json.value.default.type=vn.viettel.vds.promotion.campaign.archive.CampaignArchiveCommand",
                    "spring.json.use.type.headers=false"
            }
    )
    public void consume(@Payload CampaignArchiveCommand command, Acknowledgment acknowledgment) {
        CampaignArchiveCommandPayload payload = command != null ? command.getPayload() : null;
        if (payload == null || payload.commandId() == null || payload.campaignId() == null) {
            logger.error("Bỏ qua lệnh lưu trữ thiếu command_id/campaign_id: {}", command);
            acknowledgment.acknowledge();
            return;
        }
        logger.info("Nhận lệnh lưu trữ chiến dịch: commandId={}, campaignId={}, sentAt={}",
                payload.commandId(), payload.campaignId(), payload.sentAt());
        archiveService.archive(payload.commandId(), payload.campaignId(), payload.sentAt());
        acknowledgment.acknowledge();
    }
}
