package vn.viettel.vds.promotion.validation.adapter.out.messaging;

import com.promix.platform.core.util.IdGenerator;
import com.promix.platform.messaging.autoconfigure.utils.KafkaUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.campaign.archive.CampaignArchiveResultEvent;
import vn.viettel.vds.promotion.campaign.archive.CampaignArchiveResultEventPayload;
import vn.viettel.vds.promotion.validation.application.port.out.CampaignArchiveResultPublisherPort;

import java.time.Instant;
import java.util.Map;

/**
 * Phát kết quả lưu trữ của Validation Service lên {@code promotion_campaign_archive_result}
 * (PRM_KBNV_EVT_ARCH_002 Mục 4.3); partition key = mã chiến dịch.
 */
@Slf4j
@ConditionalOnProperty(prefix = "promix.messaging", name = "enabled", havingValue = "true")
@Component
@RequiredArgsConstructor
public class CampaignArchiveResultPublisher implements CampaignArchiveResultPublisherPort {

    private final KafkaUtils kafkaUtils;

    @Value("${kafka.topics.campaign-archive-result:" + CampaignArchiveResultEvent.TOPIC + "}")
    private String topic;

    @Value("${spring.application.name:validation}")
    private String source;

    @Override
    public void publish(CampaignArchiveResultEventPayload payload) {
        CampaignArchiveResultEvent event = CampaignArchiveResultEvent.builder()
                .id(IdGenerator.generateId())
                .aggregate(CampaignArchiveResultEvent.AGGREGATE_TYPE)
                .type(CampaignArchiveResultEvent.EVENT_TYPE)
                .source(source)
                .subject(payload.campaignId())
                .occurredAt(Instant.now())
                .version(1)
                .payload(payload)
                .metadata(Map.of("command_id", payload.commandId()))
                .build();
        kafkaUtils.send(topic, payload.campaignId(), event).whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Đã phát kết quả lưu trữ: commandId={}, campaignId={}, result={}",
                        payload.commandId(), payload.campaignId(), payload.result());
            } else {
                log.error("Phát kết quả lưu trữ lỗi: commandId={}, campaignId={}",
                        payload.commandId(), payload.campaignId(), ex);
            }
        });
    }
}
