package vn.viettel.vds.promotion.validation.application.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.viettel.vds.promotion.validation.application.port.out.RuleBindingPersistencePort;
import vn.viettel.vds.promotion.validation.domain.exception.BindingNotFoundException;
import vn.viettel.vds.promotion.validation.domain.exception.BindingNotRedeployableException;
import vn.viettel.vds.promotion.validation.domain.model.RuleBinding;

/**
 * PROM-1437 — biên dịch lại bundle của một rule binding ĐÃ TỒN TẠI, không đi qua saga.
 *
 * <p><b>Vì sao cần:</b> binding tạo trước bản vá PROM-1437 đang pin bundle
 * {@code temporal_check_allow_24_7} (ALLOW vô điều kiện) dù có khung thời gian, nên
 * chiến dịch giới hạn theo ngày/theo khoảng ngày vẫn được API Find Eligible Campaigns
 * trả về ngoài phạm vi. Bản vá chỉ chặn phát sinh mới; dữ liệu cũ không tự chữa.
 *
 * <p>Đường sửa "vào CMS lưu lại chiến dịch" KHÔNG dùng được cho lớp dữ liệu này: các
 * campaign đó đều ở trạng thái RUNNING và pp-campaign từ chối cập nhật
 * ({@code CAMPAIGN_STATUS_INVALID}). Vì vậy phải có một đường biên dịch lại trực tiếp
 * theo binding.
 *
 * <p>Hai vế phải làm ĐỦ, thiếu vế nào cũng vô nghĩa:
 * <ol>
 *   <li>biên dịch lại + ghi {@code bundleHash} mới xuống binding (phía pp-validation);</li>
 *   <li>phát sự kiện để pp-rule-engine cập nhật {@code assignments.timeframe_bundle_hash} —
 *       chính cột này mới quyết định bundle nào được fire lúc discovery.</li>
 * </ol>
 */
@Slf4j
@Service
@Transactional
public class RuleBindingRedeployService {

    private final RuleBindingPersistencePort ruleBindingPort;
    private final RulePublishingService rulePublishingService;
    private final ObjectProvider<SettingValidationRuleEventPublisher> eventPublisherProvider;

    /**
     * @param eventPublisherProvider tra chậm — publisher chỉ tồn tại khi bật
     *                               {@code promix.messaging.enabled}; thiếu nó thì
     *                               redeploy chỉ dừng ở pp-validation nên phải báo lỗi rõ.
     */
    public RuleBindingRedeployService(RuleBindingPersistencePort ruleBindingPort,
                                      RulePublishingService rulePublishingService,
                                      ObjectProvider<SettingValidationRuleEventPublisher> eventPublisherProvider) {
        this.ruleBindingPort = ruleBindingPort;
        this.rulePublishingService = rulePublishingService;
        this.eventPublisherProvider = eventPublisherProvider;
    }

    /**
     * Biên dịch lại bundle cho binding và đồng bộ hash mới sang pp-rule-engine.
     *
     * @param bindingId id của rule binding cần biên dịch lại
     * @return binding kèm {@code bundleHash} mới
     * @throws BindingNotFoundException       khi không có binding tương ứng
     * @throws BindingNotRedeployableException khi binding không active, không có gì để biên dịch,
     *                                         publish thất bại, hoặc không có kênh sự kiện để
     *                                         đồng bộ hash sang rule-engine
     */
    public RuleBinding redeploy(String bindingId) {
        log.info("Redeploying rule binding: bindingId={}", bindingId);

        RuleBinding binding = ruleBindingPort.findById(bindingId)
                .orElseThrow(() -> new BindingNotFoundException(bindingId));

        if (!Boolean.TRUE.equals(binding.getActive())) {
            throw new BindingNotRedeployableException(bindingId, "binding is not active");
        }

        SettingValidationRuleEventPublisher eventPublisher = eventPublisherProvider.getIfAvailable();
        if (eventPublisher == null) {
            // Ghi hash mới mà không báo được cho rule-engine thì assignment vẫn pin bundle cũ:
            // dừng sớm còn hơn để lại ảo giác "đã sửa".
            throw new BindingNotRedeployableException(bindingId,
                    "messaging is disabled, rule-engine cannot be notified of the new bundle");
        }

        RulePublishingService.RulePublishResult result = publish(binding);
        if (!result.isSuccess()) {
            throw new BindingNotRedeployableException(bindingId, result.getErrorMessage());
        }

        String previousHash = binding.getBundleHash();
        RuleBinding updated = ruleBindingPort.save(binding.toBuilder()
                .bundleHash(result.getBundleHash())
                .build());

        eventPublisher.publishEnableSuccessEvent("redeploy-" + bindingId, binding.getObjectId(), updated);

        log.info("Rule binding redeployed: bindingId={}, objectId={}, previousBundleHash={}, bundleHash={}",
                bindingId, binding.getObjectId(), previousHash, result.getBundleHash());
        return updated;
    }

    /**
     * Chọn đúng kiểu bundle: có rule nghiệp vụ thì publish rule bundle, không thì
     * publish assignment bundle (binding chỉ mang khung thời gian / phạm vi áp dụng).
     * Binding luôn được truyền thẳng vào để {@code timeLinks} không phụ thuộc vào lượt
     * tra DB — cùng lý do với bản vá gốc PROM-1437.
     */
    private RulePublishingService.RulePublishResult publish(RuleBinding binding) {
        String ruleId = binding.getRuleId();
        if (ruleId != null && !ruleId.isBlank()) {
            return rulePublishingService.publishRule(ruleId, binding.getId(), binding, null);
        }
        if (!binding.hasTemporalConstraints()) {
            throw new BindingNotRedeployableException(binding.getId(),
                    "binding has no rule and no temporal policy");
        }
        return rulePublishingService.publishAssignmentBundle(binding.getId(), binding, null, true);
    }
}
