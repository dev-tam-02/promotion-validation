package vn.viettel.vds.promotion.validation.application.service;

import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import vn.viettel.vds.promotion.validation.adapter.in.messaging.mapper.SettingValidationRuleCommandDTOMapper;
import vn.viettel.vds.promotion.validation.application.port.out.OperatorPersistencePort;
import vn.viettel.vds.promotion.validation.application.port.out.RuleBindingPersistencePort;
import vn.viettel.vds.promotion.validation.application.port.out.RuleEngineClient;
import vn.viettel.vds.promotion.validation.application.port.out.RuleHistoryPersistencePort;
import vn.viettel.vds.promotion.validation.application.port.out.ValidationRuleRepositoryPort;

import jakarta.validation.Validator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link SettingValidationRuleCommandHandler#parseFreqAndInterval(String)}.
 * <p>
 * Covers F3 (BUG-007): ISO 8601 unit semantics — P1D/P1W/P1M/P1Y/PT1H edge cases.
 */
@ExtendWith(MockitoExtension.class)
class ParseFreqAndIntervalTest {

    @Mock
    private RuleBindingPersistencePort ruleBindingPort;
    @Mock
    private ValidationRuleRepositoryPort validationRulePort;
    @Mock
    private SettingValidationRuleEventPublisher eventPublisher;
    @Mock
    private IdempotencyService idempotencyService;
    @Mock
    private RulePublishingService rulePublishingService;
    @Mock
    private Validator validator;
    @Mock
    private SettingValidationRuleCommandDTOMapper dtoMapper;
    @Mock
    private PlatformTransactionManager transactionManager;
    @Mock
    private RuleEngineClient ruleEngineClient;
    @Mock
    private DrlCompiler drlCompiler;
    @Mock
    private OperatorPersistencePort operatorPort;
    @Mock
    private RuleHistoryPersistencePort historyPort;

    private SettingValidationRuleCommandHandler handler;

    @BeforeEach
    void setUp() {
        // TransactionTemplate just stores the manager reference — no stub needed
        handler = new SettingValidationRuleCommandHandler(
                ruleBindingPort, validationRulePort, eventPublisher, idempotencyService,
                rulePublishingService, validator, dtoMapper, transactionManager,
                ruleEngineClient, drlCompiler, operatorPort, historyPort);
    }

    // ====== Happy-path: period strings ======

    @Test
    @DisplayName("P1D → FREQ=DAILY;INTERVAL=1")
    void givenP1D_returnsDailyInterval1() {
        assertThat(handler.parseFreqAndInterval("P1D")).isEqualTo("FREQ=DAILY;INTERVAL=1");
    }

    @Test
    @DisplayName("P7D → FREQ=WEEKLY;INTERVAL=1 (7 days = 1 week)")
    void givenP7D_returnsWeeklyInterval1() {
        assertThat(handler.parseFreqAndInterval("P7D")).isEqualTo("FREQ=WEEKLY;INTERVAL=1");
    }

    @Test
    @DisplayName("P1W → FREQ=WEEKLY;INTERVAL=1")
    void givenP1W_returnsWeeklyInterval1() {
        assertThat(handler.parseFreqAndInterval("P1W")).isEqualTo("FREQ=WEEKLY;INTERVAL=1");
    }

    @Test
    @DisplayName("P2W → FREQ=WEEKLY;INTERVAL=2")
    void givenP2W_returnsWeeklyInterval2() {
        assertThat(handler.parseFreqAndInterval("P2W")).isEqualTo("FREQ=WEEKLY;INTERVAL=2");
    }

    @Test
    @DisplayName("P1M → FREQ=MONTHLY;INTERVAL=1")
    void givenP1M_returnsMonthlyInterval1() {
        assertThat(handler.parseFreqAndInterval("P1M")).isEqualTo("FREQ=MONTHLY;INTERVAL=1");
    }

    @Test
    @DisplayName("P3M → FREQ=MONTHLY;INTERVAL=3")
    void givenP3M_returnsMonthlyInterval3() {
        assertThat(handler.parseFreqAndInterval("P3M")).isEqualTo("FREQ=MONTHLY;INTERVAL=3");
    }

    @Test
    @DisplayName("P1Y → FREQ=YEARLY;INTERVAL=1")
    void givenP1Y_returnsYearlyInterval1() {
        assertThat(handler.parseFreqAndInterval("P1Y")).isEqualTo("FREQ=YEARLY;INTERVAL=1");
    }

    @Test
    @DisplayName("P2Y → FREQ=YEARLY;INTERVAL=2")
    void givenP2Y_returnsYearlyInterval2() {
        assertThat(handler.parseFreqAndInterval("P2Y")).isEqualTo("FREQ=YEARLY;INTERVAL=2");
    }

    // ====== Edge: non-weekly day counts ======

    @Test
    @DisplayName("P3D → FREQ=DAILY;INTERVAL=3")
    void givenP3D_returnsDailyInterval3() {
        assertThat(handler.parseFreqAndInterval("P3D")).isEqualTo("FREQ=DAILY;INTERVAL=3");
    }

    @Test
    @DisplayName("P14D → FREQ=WEEKLY;INTERVAL=2 (14 days = 2 weeks)")
    void givenP14D_returnsWeeklyInterval2() {
        assertThat(handler.parseFreqAndInterval("P14D")).isEqualTo("FREQ=WEEKLY;INTERVAL=2");
    }

    // ====== Edge: PT duration strings ======

    @Test
    @DisplayName("PT1H → FREQ=HOURLY;INTERVAL=1 (duration mapped, not rejected)")
    void givenPT1H_returnsHourlyInterval1() {
        assertThat(handler.parseFreqAndInterval("PT1H")).isEqualTo("FREQ=HOURLY;INTERVAL=1");
    }

    @Test
    @DisplayName("PT4H → FREQ=HOURLY;INTERVAL=4")
    void givenPT4H_returnsHourlyInterval4() {
        assertThat(handler.parseFreqAndInterval("PT4H")).isEqualTo("FREQ=HOURLY;INTERVAL=4");
    }

    // ====== Edge: null / blank / garbage ======

    @Test
    @DisplayName("null → FREQ=DAILY;INTERVAL=1 (safe default)")
    void givenNull_returnsDefault() {
        assertThat(handler.parseFreqAndInterval(null)).isEqualTo("FREQ=DAILY;INTERVAL=1");
    }

    @Test
    @DisplayName("blank string → FREQ=DAILY;INTERVAL=1 (safe default)")
    void givenBlank_returnsDefault() {
        assertThat(handler.parseFreqAndInterval("   ")).isEqualTo("FREQ=DAILY;INTERVAL=1");
    }

    @Test
    @DisplayName("garbage 'XYZ' → FREQ=DAILY;INTERVAL=1 (safe default on parse failure)")
    void givenGarbage_returnsDefault() {
        assertThat(handler.parseFreqAndInterval("XYZ")).isEqualTo("FREQ=DAILY;INTERVAL=1");
    }
}
