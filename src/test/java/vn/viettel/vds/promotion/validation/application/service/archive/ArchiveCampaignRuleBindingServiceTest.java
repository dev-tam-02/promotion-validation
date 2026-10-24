package vn.viettel.vds.promotion.validation.application.service.archive;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.viettel.vds.promotion.campaign.archive.CampaignArchiveResult;
import vn.viettel.vds.promotion.campaign.archive.CampaignArchiveResultEventPayload;
import vn.viettel.vds.promotion.campaign.archive.CampaignArchiveService;
import vn.viettel.vds.promotion.validation.application.port.out.CampaignArchivePort;
import vn.viettel.vds.promotion.validation.application.port.out.CampaignArchiveResultPublisherPort;
import vn.viettel.vds.promotion.validation.application.service.SettingValidationRuleEventPublisher;
import vn.viettel.vds.promotion.validation.domain.model.RuleBinding;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PRM_KBNV_EVT_ARCH_002 Bước 4, 5 — Validation Service xoá mềm rule binding của chiến dịch hết hạn.
 */
@DisplayName("ArchiveCampaignRuleBindingService — lưu trữ rule binding của chiến dịch")
class ArchiveCampaignRuleBindingServiceTest {

    private static final String COMMAND_ID = "cmd-1";
    private static final String CAMPAIGN_ID = "019df1ed-6984-72e1-966b-7c430236ad6f";
    private static final Instant SENT_AT = Instant.parse("2026-12-31T19:00:00Z");

    private CampaignArchivePort port;
    private CampaignArchiveResultPublisherPort publisher;
    private SettingValidationRuleEventPublisher ruleEvents;
    private ArchiveCampaignRuleBindingService sut;

    @BeforeEach
    void setUp() {
        port = mock(CampaignArchivePort.class);
        publisher = mock(CampaignArchiveResultPublisherPort.class);
        ruleEvents = mock(SettingValidationRuleEventPublisher.class);
        sut = new ArchiveCampaignRuleBindingService(port, publisher, ruleEvents, 2);
        when(port.findResult(anyString())).thenReturn(Optional.empty());
    }

    private static RuleBinding binding(String id, String objectType) {
        RuleBinding b = new RuleBinding();
        b.setId(id);
        b.setObjectType(objectType);
        b.setObjectId(CAMPAIGN_ID);
        return b;
    }

    @Test
    @DisplayName("Bước 5: xoá mềm theo lô (deleted_at = sent_at), phát ValidationRuleDeletedEvent cho từng binding, phát SUCCESS kèm số bản ghi")
    void archivesInBatchesAndPublishes() {
        List<RuleBinding> bindings = List.of(binding("b1", "CAMPAIGN"), binding("b2", "DISCOUNT_COUPON"),
                binding("b3", "DISCOUNT_COUPON"));
        when(port.findBindingsOfCampaign(CAMPAIGN_ID)).thenReturn(bindings);
        when(port.softDeleteBindings(anyList(), any())).thenAnswer(inv -> ((List<?>) inv.getArgument(0)).size());

        CampaignArchiveResultEventPayload payload = sut.archive(COMMAND_ID, CAMPAIGN_ID, SENT_AT);

        verify(port).softDeleteBindings(List.of("b1", "b2"), SENT_AT);
        verify(port).softDeleteBindings(List.of("b3"), SENT_AT);
        // deletedAt của sự kiện = sent_at của lệnh lưu trữ, không phải thời điểm phát.
        verify(ruleEvents, times(3)).publishDeleteSuccessEvent(eq(COMMAND_ID), eq(CAMPAIGN_ID), any(), eq("SYSTEM_ARCHIVE"), eq(SENT_AT));
        assertThat(payload.service()).isEqualTo(CampaignArchiveService.VALIDATION);
        assertThat(payload.result()).isEqualTo(CampaignArchiveResult.SUCCESS);
        assertThat(payload.softDeletedCounts()).containsExactly(Map.entry("rule_bindings", 3L));
        verify(publisher).publish(payload);
    }

    @Test
    @DisplayName("Bước 4: nhận lại cùng ID lệnh ⇒ phát lại kết quả đã có, không xoá mềm lại")
    void duplicateCommandReplays() {
        when(port.findResult(COMMAND_ID)).thenReturn(Optional.of(new CampaignArchivePort.StoredResult(
                COMMAND_ID, CAMPAIGN_ID, "SUCCESS", Map.of("rule_bindings", 3L), null)));

        CampaignArchiveResultEventPayload payload = sut.archive(COMMAND_ID, CAMPAIGN_ID, SENT_AT);

        assertThat(payload.softDeletedCounts()).containsEntry("rule_bindings", 3L);
        verify(port, never()).findBindingsOfCampaign(anyString());
        verify(port, never()).softDeleteBindings(anyList(), any());
        verify(publisher).publish(payload);
    }

    @Test
    @DisplayName("Bước 4: không còn binding ⇒ SUCCESS với số bản ghi 0")
    void noBindingsIsSuccess() {
        when(port.findBindingsOfCampaign(CAMPAIGN_ID)).thenReturn(List.of());

        CampaignArchiveResultEventPayload payload = sut.archive(COMMAND_ID, CAMPAIGN_ID, SENT_AT);

        assertThat(payload.result()).isEqualTo(CampaignArchiveResult.SUCCESS);
        assertThat(payload.softDeletedCounts()).containsEntry("rule_bindings", 0L);
    }

    @Test
    @DisplayName("Bước 5 (TB/TO): lỗi khi xoá mềm ⇒ FAILED kèm mô tả lỗi, giữ số bản ghi của các lô đã xong")
    void failureKeepsCompletedBatches() {
        when(port.findBindingsOfCampaign(CAMPAIGN_ID)).thenReturn(List.of(binding("b1", "CAMPAIGN"),
                binding("b2", "CAMPAIGN"), binding("b3", "CAMPAIGN")));
        when(port.softDeleteBindings(eq(List.of("b1", "b2")), any())).thenReturn(2);
        when(port.softDeleteBindings(eq(List.of("b3")), any())).thenThrow(new IllegalStateException("Lock wait timeout"));

        CampaignArchiveResultEventPayload payload = sut.archive(COMMAND_ID, CAMPAIGN_ID, SENT_AT);

        assertThat(payload.result()).isEqualTo(CampaignArchiveResult.FAILED);
        assertThat(payload.errorMessage()).contains("Lock wait timeout");
        assertThat(payload.softDeletedCounts()).containsEntry("rule_bindings", 2L);
    }

    @Test
    @DisplayName("Bước 6 không tính vào kết quả: lỗi phát ValidationRuleDeletedEvent không làm hỏng lệnh")
    void ruleEventFailureDoesNotFailArchive() {
        when(port.findBindingsOfCampaign(CAMPAIGN_ID)).thenReturn(List.of(binding("b1", "CAMPAIGN")));
        when(port.softDeleteBindings(anyList(), any())).thenReturn(1);
        doThrow(new RuntimeException("kafka down")).when(ruleEvents).publishDeleteSuccessEvent(any(), any(), any(), any(), any());

        assertThat(sut.archive(COMMAND_ID, CAMPAIGN_ID, SENT_AT).result()).isEqualTo(CampaignArchiveResult.SUCCESS);
    }

    @Test
    @DisplayName("Mục 2.4.1: số bản ghi mỗi lô xoá mềm phải từ 1 đến 10.000")
    void batchSizeBounds() {
        assertThatThrownBy(() -> new ArchiveCampaignRuleBindingService(port, publisher, ruleEvents, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ArchiveCampaignRuleBindingService(port, publisher, ruleEvents, 10_001))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
