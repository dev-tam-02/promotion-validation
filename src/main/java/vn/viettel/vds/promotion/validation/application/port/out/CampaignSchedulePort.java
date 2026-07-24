package vn.viettel.vds.promotion.validation.application.port.out;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;

/**
 * Output port for reading campaign scheduling data owned by pp-campaign.
 *
 * <p>VRUL001 (PROM-1112): quy tắc chỉ gán với chiến dịch <em>chưa tới thời gian bắt
 * đầu hiệu lực</em> thì vẫn được chỉnh sửa, nên pp-validation cần biết
 * {@code start_from} của các chiến dịch đang gán.</p>
 */
public interface CampaignSchedulePort {

    /**
     * Batch lookup of campaign effective start times.
     *
     * @param campaignIds campaign IDs to resolve
     * @return map campaignId → startFrom. Chỉ chứa chiến dịch tồn tại VÀ có
     *         {@code start_from}; chiến dịch không tồn tại, chưa cấu hình mốc bắt đầu,
     *         hoặc không tra được (service lỗi) đều vắng mặt — caller phải coi đó là
     *         "đã hiệu lực" (fail closed).
     */
    Map<String, Instant> findStartTimes(Collection<String> campaignIds);

    /**
     * Batch lookup of campaign lifecycle status.
     *
     * <p>VRUL001 (PROM-1369): {@code start_from} một mình không đủ để quyết định
     * editability — một chiến dịch từng RUNNING rồi bị <em>Tạm dừng (PAUSED)</em> có
     * thể bị sửa lại {@code start_from} về tương lai; lúc đó {@code startsInFuture}
     * hiểu nhầm là "chưa từng chạy → còn sửa được". Trạng thái chiến dịch mới phân biệt
     * được: chỉ các chiến dịch <em>chưa vào vòng đời chạy</em>
     * ({@code INITIALIZING}/{@code ACTIVE}) mới cho sửa quy tắc.</p>
     *
     * @param campaignIds campaign IDs to resolve
     * @return map campaignId → tên trạng thái ({@code CampaignStatus}). Chiến dịch không
     *         tồn tại hoặc không tra được (service lỗi) sẽ vắng mặt — caller phải coi đó
     *         là "đã hiệu lực / đã khoá" (fail closed).
     */
    Map<String, String> findStatuses(Collection<String> campaignIds);
}
