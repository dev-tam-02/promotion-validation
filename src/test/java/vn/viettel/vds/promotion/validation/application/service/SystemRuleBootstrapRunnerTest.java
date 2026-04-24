package vn.viettel.vds.promotion.validation.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import vn.viettel.vds.promotion.validation.adapter.out.external.WebClientRuleEngineAdapter;
import vn.viettel.vds.promotion.validation.application.port.out.OperatorPersistencePort;
import vn.viettel.vds.promotion.validation.application.port.out.RuleEngineClient;
import vn.viettel.vds.promotion.validation.application.port.out.RulePersistencePort;
import vn.viettel.vds.promotion.validation.domain.model.Operator;
import vn.viettel.vds.promotion.validation.domain.model.Rule;
import vn.viettel.vds.promotion.validation.domain.model.RuleNode;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Verifies that {@link RuleManagementService#republishSystemRules()} populates
 * {@code bundle_hash} for PUBLISHED rules that were seeded with a NULL bundle.
 *
 * <p>Uses WireMock to simulate pp-rule-engine POST /v1/rules returning a bundleHash.
 * The test confirms the rule's {@code bundleHash} is set and state is PUBLISHED
 * after the bootstrap run — which is the behaviour driven by changelog 031 +
 * {@link vn.viettel.vds.promotion.validation.adapter.config.SystemRuleBootstrapRunner}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SystemRuleBootstrapRunner — bundle_hash populated after startup")
class SystemRuleBootstrapRunnerTest {

    private static WireMockServer wireMock;

    @Mock
    private RulePersistencePort rulePort;

    @Mock
    private OperatorPersistencePort operatorPort;

    private RuleManagementService ruleManagementService;

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

        RuleEngineClient ruleEngineClient = new TestWebClientRuleEngineAdapter(
                WebClient.builder(),
                "http://localhost:" + wireMock.port(),
                "");

        ObjectMapper objectMapper = new ObjectMapper();
        RuleTreeAssembler assembler = new RuleTreeAssembler();
        RuleValidator validator = new RuleValidator(objectMapper);
        DslGenerator dslGenerator = new DslGenerator(objectMapper);
        DrlCompiler drlCompiler = new DrlCompiler();

        ruleManagementService = new RuleManagementService(
                rulePort, assembler, validator, dslGenerator, drlCompiler,
                ruleEngineClient, operatorPort);
    }

    @Test
    @DisplayName("seeded rule-sys-owner-only (bundle_hash=NULL) gets bundleHash after republish")
    void republishSystemRules_populatesBundleHash() {
        // Stub pp-rule-engine POST /v1/rules → returns bundleHash
        wireMock.stubFor(post(urlEqualTo("/v1/rules"))
                .withHeader("Content-Type", containing("application/json"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":201,\"success\":true,\"data\":{\"ruleId\":\"rule-sys-owner-only\",\"bundleHash\":\"sha256-bootstrap-01\"}}")));

        // Stub operator lookup (no-params operator, uses fallback compilerId)
        when(operatorPort.findGlobalOperatorsByStatus(Operator.OperatorStatus.ACTIVE))
                .thenReturn(List.of(
                        Operator.builder()
                                .id("op-owner")
                                .name("customer.is_owner")
                                .compilerId("tpl_customer_is_owner_v1")
                                .status(Operator.OperatorStatus.ACTIVE)
                                .build()
                ));

        // Seeded rule has bundle_hash = null (matches changelog 031)
        RuleNode condNode = RuleNode.builder()
                .nodeId("n1")
                .type(RuleNode.NodeType.COND)
                .operatorName("customer.is_owner")
                .params(Map.of())
                .reasonCode("VOUCHER_NOT_OWNED_BY_CUSTOMER")
                .build();

        Rule seededRule = Rule.builder()
                .id("rule-sys-owner-only")
                .code("rule-sys-owner-only")
                .name("System — Owner Only")
                .state(Rule.RuleState.PUBLISHED)
                .logic(Rule.LogicType.ALL)
                .nodes(List.of(condNode))
                .bundleHash(null)         // null — the condition for bootstrap
                .isSystem(true)
                .ruleVersion(1L)
                .version(0L)
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .updatedAt(Instant.parse("2026-01-01T00:00:00Z"))
                .build();

        // findPublishedWithNullBundleHash returns the stale rule
        when(rulePort.findPublishedWithNullBundleHash()).thenReturn(List.of(seededRule));
        // findById returns the full rule with nodes
        when(rulePort.findById("rule-sys-owner-only")).thenReturn(Optional.of(seededRule));

        // Capture the saved rule
        final Rule[] savedRule = {null};
        when(rulePort.save(any(Rule.class))).thenAnswer(inv -> {
            savedRule[0] = inv.getArgument(0);
            return savedRule[0];
        });

        // Act — simulates what SystemRuleBootstrapRunner.run() does
        ruleManagementService.republishSystemRules();

        // Assert: WireMock received a POST to register the rule
        wireMock.verify(postRequestedFor(urlEqualTo("/v1/rules")));

        // Assert: rule was saved with bundleHash populated and state PUBLISHED
        assertThat(savedRule[0]).isNotNull();
        assertThat(savedRule[0].getBundleHash()).isEqualTo("sha256-bootstrap-01");
        assertThat(savedRule[0].getState()).isEqualTo(Rule.RuleState.PUBLISHED);
        assertThat(savedRule[0].getDsl()).isNotNull();
    }

    @Test
    @DisplayName("republishSystemRules is a no-op when no rules have null bundle_hash")
    void republishSystemRules_noOp_whenAllRulesCompiled() {
        when(rulePort.findPublishedWithNullBundleHash()).thenReturn(List.of());

        // Should not throw, should not contact pp-rule-engine
        ruleManagementService.republishSystemRules();

        wireMock.verify(0, postRequestedFor(urlEqualTo("/v1/rules")));
    }

    @Test
    @DisplayName("engine failure during republish is swallowed — does not abort startup")
    void republishSystemRules_engineFailure_doesNotPropagateException() {
        wireMock.stubFor(post(urlEqualTo("/v1/rules"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("{\"error\":\"internal error\"}")));

        when(operatorPort.findGlobalOperatorsByStatus(Operator.OperatorStatus.ACTIVE))
                .thenReturn(List.of());

        RuleNode condNode = RuleNode.builder()
                .nodeId("n1")
                .type(RuleNode.NodeType.COND)
                .operatorName("customer.is_owner")
                .params(Map.of())
                .reasonCode("VOUCHER_NOT_OWNED_BY_CUSTOMER")
                .build();

        Rule seededRule = Rule.builder()
                .id("rule-sys-owner-only")
                .code("rule-sys-owner-only")
                .name("System — Owner Only")
                .state(Rule.RuleState.PUBLISHED)
                .logic(Rule.LogicType.ALL)
                .nodes(List.of(condNode))
                .bundleHash(null)
                .ruleVersion(1L)
                .version(0L)
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .updatedAt(Instant.parse("2026-01-01T00:00:00Z"))
                .build();

        when(rulePort.findPublishedWithNullBundleHash()).thenReturn(List.of(seededRule));
        when(rulePort.findById("rule-sys-owner-only")).thenReturn(Optional.of(seededRule));
        when(rulePort.save(any(Rule.class))).thenAnswer(inv -> inv.getArgument(0));

        // Must not throw — engine error is swallowed
        ruleManagementService.republishSystemRules();
    }

    // ─── helpers ────────────────────────────────────────────────────────────

    private static class TestWebClientRuleEngineAdapter extends WebClientRuleEngineAdapter {
        public TestWebClientRuleEngineAdapter(WebClient.Builder builder, String url, String path) {
            super(builder, url, path);
        }
    }
}
