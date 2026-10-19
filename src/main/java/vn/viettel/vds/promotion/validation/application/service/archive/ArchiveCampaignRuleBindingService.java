package vn.viettel.vds.promotion.validation.application.service.archive;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import vn.viettel.vds.promotion.campaign.archive.CampaignArchiveResult;
import vn.viettel.vds.promotion.campaign.archive.CampaignArchiveResultEventPayload;
import vn.viettel.vds.promotion.campaign.archive.CampaignArchiveService;
import vn.viettel.vds.promotion.validation.application.port.out.CampaignArchivePort;
import vn.viettel.vds.promotion.validation.application.port.out.CampaignArchiveResultPublisherPort;
import vn.viettel.vds.promotion.validation.application.service.SettingValidationRuleEventPublisher;
import vn.viettel.vds.promotion.validation.domain.model.RuleBinding;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Xoá mềm rule binding của chiến dịch hết hạn khi nhận lệnh lưu trữ — PRM_KBNV_EVT_ARCH_002 Bước 4, 5.
 *
 * <p>Rule binding có loại đối tượng CAMPAIGN hoặc DISCOUNT_COUPON và mã đối tượng là chiến dịch được xoá mềm theo
 * lô (mỗi lô một transaction); sau mỗi lô phát {@code ValidationRuleDeletedEvent} cho từng binding để Rule Engine
 * Service xoá mềm assignment, cấu hình Fast Check (Bước 6). Quy tắc kiểm tra ({@code validation_rules}) dùng chung
 * nên giữ nguyên.
 *
 * <p>Chống trùng theo ID lệnh: nhận lại cùng ID thì phát lại kết quả đã có, không xoá mềm lại. Không còn binding
 * của chiến dịch thì trả thành công với số bản ghi 0.
 */
// Cùng điều kiện với SettingValidationRuleEventPublisher (phát ValidationRuleDeletedEvent) và Kafka consumer.
@ConditionalOnProperty(prefix = "promix.messaging", name = "enabled", havingValue = "true")
@Service
public class ArchiveCampaignRuleBindingService {

    private static final Logger logger = LoggerFactory.getLogger(ArchiveCampaignRuleBindingService.class);

    static final String RULE_BINDINGS = "rule_bindings";

    private final CampaignArchivePort archivePort;
    private final CampaignArchiveResultPublisherPort resultPublisher;
    private final SettingValidationRuleEventPublisher ruleEventPublisher;
    private final int batchSize;

    public ArchiveCampaignRuleBindingService(CampaignArchivePort archivePort,
                                             CampaignArchiveResultPublisherPort resultPublisher,
                                             SettingValidationRuleEventPublisher ruleEventPublisher,
                                             @Value("${validation.campaign-archive.soft-delete-batch-size:1000}") int batchSize) {
        if (batchSize < 1 || batchSize > 10_000) {
            // Tham số "Số bản ghi mỗi lô xoá mềm": số nguyên từ 1 đến 10.000 (ARCH_002 Mục 2.4.1).
            throw new IllegalArgumentException("validation.campaign-archive.soft-delete-batch-size phải từ 1 đến 10000");
        }
        this.archivePort = archivePort;
        this.resultPublisher = resultPublisher;
        this.ruleEventPublisher = ruleEventPublisher;
        this.batchSize = batchSize;
    }

    /**
     * @param commandId  ID lệnh lưu trữ
     * @param campaignId chiến dịch cần lưu trữ
     * @param sentAt     thời điểm gửi lệnh — dùng làm {@code deleted_at} (thống nhất giữa các dịch vụ); thiếu thì
     *                   lấy thời điểm xử lý
     */
    public CampaignArchiveResultEventPayload archive(String commandId, String campaignId, Instant sentAt) {
        Optional<CampaignArchivePort.StoredResult> previous = archivePort.findResult(commandId);
        if (previous.isPresent()) {
            logger.info("Lệnh lưu trữ commandId={} đã xử lý trước đó ({}), phát lại kết quả cũ",
                    commandId, previous.get().result());
            CampaignArchiveResultEventPayload replay = toPayload(previous.get());
            resultPublisher.publish(replay);
            return replay;
        }

        Instant deletedAt = sentAt != null ? sentAt : Instant.now();
        long deleted = 0;
        CampaignArchiveResult result = CampaignArchiveResult.SUCCESS;
        String error = null;
        try {
            List<RuleBinding> bindings = archivePort.findBindingsOfCampaign(campaignId);
            for (int from = 0; from < bindings.size(); from += batchSize) {
                List<RuleBinding> batch = bindings.subList(from, Math.min(from + batchSize, bindings.size()));
                deleted += archivePort.softDeleteBindings(batch.stream().map(RuleBinding::getId).toList(), deletedAt);
                batch.forEach(binding -> publishRuleDeleted(commandId, campaignId, binding, deletedAt));
            }
            logger.info("Lưu trữ rule binding của chiến dịch {} xong (commandId={}): {} bản ghi",
                    campaignId, commandId, deleted);
        } catch (RuntimeException e) {
            logger.error("Lưu trữ rule binding của chiến dịch {} thất bại (commandId={})", campaignId, commandId, e);
            result = CampaignArchiveResult.FAILED;
            error = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        }

        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put(RULE_BINDINGS, deleted);
        CampaignArchivePort.StoredResult stored =
                new CampaignArchivePort.StoredResult(commandId, campaignId, result.name(), counts, error);
        archivePort.saveResult(stored);
        CampaignArchiveResultEventPayload payload = toPayload(stored);
        resultPublisher.publish(payload);
        return payload;
    }

    /**
     * Bước 6 không tính vào kết quả lưu trữ (Bước 12): binding đã xoá mềm nên assignment còn sót không được dùng để
     * đánh giá. Lỗi phát sự kiện chỉ ghi log, không làm hỏng kết quả của lệnh.
     */
    private void publishRuleDeleted(String commandId, String campaignId, RuleBinding binding, Instant deletedAt) {
        try {
            ruleEventPublisher.publishDeleteSuccessEvent(commandId, campaignId, binding, CampaignArchivePort.ARCHIVE_ACTOR,
                    deletedAt);
        } catch (RuntimeException e) {
            logger.error("Không phát được ValidationRuleDeletedEvent cho binding {} (commandId={})",
                    binding.getId(), commandId, e);
        }
    }

    private static CampaignArchiveResultEventPayload toPayload(CampaignArchivePort.StoredResult stored) {
        return new CampaignArchiveResultEventPayload(
                stored.commandId(),
                stored.campaignId(),
                CampaignArchiveService.VALIDATION,
                CampaignArchiveResult.valueOf(stored.result()),
                stored.softDeletedCounts() != null ? stored.softDeletedCounts() : Map.of(),
                stored.errorMessage());
    }
}
