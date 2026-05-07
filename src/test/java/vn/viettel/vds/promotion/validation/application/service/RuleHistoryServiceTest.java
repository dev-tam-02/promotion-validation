package vn.viettel.vds.promotion.validation.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import vn.viettel.vds.promotion.validation.adapter.out.external.WebClientRuleEngineAdapter;
import vn.viettel.vds.promotion.validation.application.port.out.OperatorPersistencePort;
import vn.viettel.vds.promotion.validation.application.port.out.RuleEngineClient;
import vn.viettel.vds.promotion.validation.application.port.out.RuleHistoryPersistencePort;
import vn.viettel.vds.promotion.validation.application.port.out.RulePersistencePort;
import vn.viettel.vds.promotion.validation.domain.model.Operator;
import vn.viettel.vds.promotion.validation.domain.model.Rule;
import vn.viettel.vds.promotion.validation.domain.model.RuleHistoryEntry;
import vn.viettel.vds.promotion.validation.domain.model.RuleNode;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link RuleHistoryService}, in particular the restore flow.
 *
 * <p>Uses WireMock to stub pp-rule-engine so the full compile→register pipeline
 * executes end-to-end. The key assertion is that after a restore, the rule's
 * {@code bundleHash} reflects the new bundle returned by pp-rule-engine, not the
 * stale hash from before the restore.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RuleHistoryService — restore recompile")
class RuleHistoryServiceTest {

    private static WireMockServer wireMock;

    @Mock
    private RulePersistencePort rulePort;

    @Mock
    private RuleHistoryPersistencePort historyPort;

    @Mock
    private OperatorPersistencePort operatorPort;

    private RuleHistoryService historyService;
    private ObjectMapper objectMapper;

    @BeforeAll
    static void startWireMock() {
        wireMock = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMock.start();
    }

    @AfterAll
    static void stopWireMock() {
        wireMock.stop();
    }

    @BeforeEach
    void setUp() {
        wireMock.resetAll();
        objectMapper = new ObjectMapper();

        WebClient.Builder webClientBuilder = WebClient.builder();
        RuleEngineClient ruleEngineClient = new TestWebClientRuleEngineAdapter(
                webClientBuilder,
                "http://localhost:" + wireMock.port(),
                ""
        );

        RuleTreeAssembler assembler = new RuleTreeAssembler();
        RuleValidator validator = new RuleValidator(objectMapper);
        DslGenerator dslGenerator = new DslGenerator(objectMapper);
        DrlCompiler drlCompiler = new DrlCompiler();

        RuleManagementService ruleManagementService = new RuleManagementService(
                rulePort, assembler, validator, dslGenerator, drlCompiler,
                ruleEngineClient, operatorPort, Optional.of(historyPort));

        historyService = new RuleHistoryService(rulePort, historyPort, ruleManagementService);

        when(rulePort.save(any(Rule.class))).thenAnswer(inv -> inv.getArgument(0));
        when(operatorPort.findGlobalOperatorsByStatus(any())).thenReturn(List.of());
    }

    @Test
    @DisplayName("restoreRecompilesAndBundleHashUpdates: after restore, bundleHash matches engine response")
    void restoreRecompilesAndBundleHashUpdates() {
        // Arrange: WireMock stubs PUT /v1/rules/{id} (update path, because current has bundleHash)
        // and POST /v1/rules (register path). We test the update path since the current rule has an existing hash.
        wireMock.stubFor(put(urlPathMatching("/v1/rules/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":200,\"success\":true,\"data\":{\"ruleId\":\"rule-restore-01\",\"bundleHash\":\"new-bundle-hash-after-restore\"}}")));

        // Arrange: operator required for DRL compilation
        Operator orderOp = Operator.builder()
                .id("op1")
                .name("order.total.gte")
                .compilerId("tpl_order_total_gte_v1")
                .status(Operator.OperatorStatus.ACTIVE)
                .build();
        when(operatorPort.findGlobalOperatorsByStatus(Operator.OperatorStatus.ACTIVE))
                .thenReturn(List.of(orderOp));

        // Arrange: current rule with an existing bundleHash and nodes
        RuleNode cond = RuleNode.builder()
                .nodeId("c1")
                .type(RuleNode.NodeType.COND)
                .operatorName("order.total.gte")
                .params(Map.of("amount", 500_000, "currency", "VND"))
                .reasonCode("MIN_ORDER_NOT_MET")
                .build();
        RuleNode group = RuleNode.builder()
                .nodeId("g1")
                .type(RuleNode.NodeType.GROUP)
                .groupLogic(Rule.LogicType.ALL)
                .children(List.of(cond))
                .build();

        Rule currentRule = Rule.builder()
                .id("rule-restore-01")
                .name("My Rule")
                .logic(Rule.LogicType.ALL)
                .state(Rule.RuleState.PUBLISHED)
                .ruleVersion(3L)
                .bundleHash("old-stale-bundle-hash")
                .nodes(List.of(group))
                .active(true)
                .build();

        // Arrange: snapshot at version 1 (what we're restoring to)
        Map<String, Object> snapshotDsl = Map.of("ruleId", "rule-restore-01", "version", 1);
        RuleHistoryEntry snapshot = new RuleHistoryEntry(
                "hist-001",
                "rule-restore-01",
                1L,
                RuleHistoryEntry.ChangeType.CREATE,
                "admin",
                Instant.now().minusSeconds(3600),
                snapshotDsl,
                "bundle-hash-v1",
                "PUBLISHED",
                null
        );

        when(rulePort.findById("rule-restore-01")).thenReturn(Optional.of(currentRule));
        when(historyPort.findByRuleIdAndVersion("rule-restore-01", 1L)).thenReturn(Optional.of(snapshot));

        // Act
        Rule result = historyService.restore("rule-restore-01", 1L, "operator");

        // Assert: bundleHash is updated to the new hash from pp-rule-engine (not the stale old one)
        assertThat(result.getBundleHash()).isEqualTo("new-bundle-hash-after-restore");
        assertThat(result.getBundleHash()).isNotEqualTo("old-stale-bundle-hash");

        // Assert: rule is still PUBLISHED after recompile
        assertThat(result.getState()).isEqualTo(Rule.RuleState.PUBLISHED);

        // Assert: WireMock received the PUT call (engine update, since rule had existing bundleHash)
        wireMock.verify(putRequestedFor(urlPathMatching("/v1/rules/.*")));

        // Assert: a RESTORE history entry was recorded (the pre-restore snapshot)
        ArgumentCaptor<RuleHistoryEntry> historyCaptor = ArgumentCaptor.forClass(RuleHistoryEntry.class);
        verify(historyPort, atLeastOnce()).save(historyCaptor.capture());
        boolean hasRestoreEntry = historyCaptor.getAllValues().stream()
                .anyMatch(e -> e.getChangeType() == RuleHistoryEntry.ChangeType.RESTORE);
        assertThat(hasRestoreEntry).as("A RESTORE history entry should have been recorded").isTrue();
    }

    @Test
    @DisplayName("restore: pre-restore history entry uses ChangeType.RESTORE (not UPDATE)")
    void restore_preRestoreEntry_usesRestoreChangeType() {
        // Arrange: stub PUT endpoint so compile pipeline completes
        wireMock.stubFor(put(urlPathMatching("/v1/rules/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":200,\"success\":true,\"data\":{\"ruleId\":\"r2\",\"bundleHash\":\"hash-xyz\"}}")));

        Operator op = Operator.builder()
                .id("op2").name("order.total.gte").compilerId("tpl_order_total_gte_v1")
                .status(Operator.OperatorStatus.ACTIVE).build();
        when(operatorPort.findGlobalOperatorsByStatus(any())).thenReturn(List.of(op));

        RuleNode cond = RuleNode.builder()
                .nodeId("c2").type(RuleNode.NodeType.COND)
                .operatorName("order.total.gte")
                .params(Map.of("amount", 100_000, "currency", "VND"))
                .reasonCode("RC").build();
        RuleNode group = RuleNode.builder()
                .nodeId("g2").type(RuleNode.NodeType.GROUP)
                .groupLogic(Rule.LogicType.ALL).children(List.of(cond)).build();

        Rule current = Rule.builder()
                .id("r2").name("Rule 2").logic(Rule.LogicType.ALL)
                .state(Rule.RuleState.PUBLISHED).ruleVersion(5L)
                .bundleHash("old-hash").nodes(List.of(group)).active(true).build();

        RuleHistoryEntry snapshot = new RuleHistoryEntry(
                "h2", "r2", 2L, RuleHistoryEntry.ChangeType.UPDATE,
                "dev", Instant.now().minusSeconds(1800), Map.of(), "hash-v2", "PUBLISHED", null);

        when(rulePort.findById("r2")).thenReturn(Optional.of(current));
        when(historyPort.findByRuleIdAndVersion("r2", 2L)).thenReturn(Optional.of(snapshot));

        // Act
        historyService.restore("r2", 2L, "admin");

        // Assert: the pre-restore history entry recorded has ChangeType.RESTORE, not UPDATE
        ArgumentCaptor<RuleHistoryEntry> captor = ArgumentCaptor.forClass(RuleHistoryEntry.class);
        verify(historyPort, atLeastOnce()).save(captor.capture());
        RuleHistoryEntry preRestoreEntry = captor.getAllValues().get(0);
        assertThat(preRestoreEntry.getChangeType())
                .as("Pre-restore history entry must use ChangeType.RESTORE")
                .isEqualTo(RuleHistoryEntry.ChangeType.RESTORE);
    }

    /**
     * Subclass that bypasses Spring @CircuitBreaker AOP proxy for unit tests.
     */
    private static class TestWebClientRuleEngineAdapter extends WebClientRuleEngineAdapter {
        public TestWebClientRuleEngineAdapter(WebClient.Builder builder, String url, String path) {
            super(builder, url, path);
        }
    }
}
