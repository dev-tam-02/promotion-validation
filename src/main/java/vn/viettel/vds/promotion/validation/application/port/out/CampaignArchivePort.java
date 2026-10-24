package vn.viettel.vds.promotion.validation.application.port.out;

import vn.viettel.vds.promotion.validation.domain.model.RuleBinding;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Cổng ra cho việc lưu trữ (xoá mềm) rule binding của chiến dịch hết hạn — PRM_KBNV_EVT_ARCH_002 Bước 5.
 */
public interface CampaignArchivePort {

    /** Người xoá ghi vào bảng xoá mềm, phân biệt với bản ghi do người dùng CMS xoá (Mục 4.1). */
    String ARCHIVE_ACTOR = "SYSTEM_ARCHIVE";

    /** Kết quả đã xử lý của một lệnh lưu trữ, để phát lại khi nhận trùng ID lệnh. */
    record StoredResult(String commandId, String campaignId, String result, Map<String, Long> softDeletedCounts,
                        String errorMessage) {
    }

    /** Rule binding còn ở bảng gốc có object_type CAMPAIGN hoặc DISCOUNT_COUPON và object_id là chiến dịch. */
    List<RuleBinding> findBindingsOfCampaign(String campaignId);

    /**
     * Xoá mềm các rule binding theo id trong MỘT transaction: chép sang {@code rule_bindings_deleted} rồi xoá
     * khỏi {@code rule_bindings}.
     *
     * @return số bản ghi đã xoá mềm
     */
    int softDeleteBindings(List<String> bindingIds, Instant deletedAt);

    Optional<StoredResult> findResult(String commandId);

    void saveResult(StoredResult result);
}
