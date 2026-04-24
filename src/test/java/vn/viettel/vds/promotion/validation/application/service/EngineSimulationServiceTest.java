package vn.viettel.vds.promotion.validation.application.service;

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
import vn.viettel.vds.promotion.validation.adapter.in.web.RuleSimulationController;
import vn.viettel.vds.promotion.validation.adapter.in.web.dto.RuleEngineSimulateRequest;
import vn.viettel.vds.promotion.validation.adapter.in.web.dto.RuleEngineSimulateResponse;
import vn.viettel.vds.promotion.validation.adapter.out.external.WebClientRuleEngineAdapter;
import vn.viettel.vds.promotion.validation.application.port.out.RuleEngineClient;
import vn.viettel.vds.promotion.validation.application.port.out.RulePersistencePort;
import vn.viettel.vds.promotion.validation.domain.model.Rule;

import java.util.Map;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link EngineSimulationService} and {@link RuleSimulationController}.
 *
 * <p>Uses WireMock to stub {@code POST /v1/rules/evaluate} on pp-rule-engine,
 * verifying that the service correctly maps the engine response to the application's
 * {@link RuleEngineClient.SimulateResponse}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EngineSimulationService — WireMock stub for /v1/rules/evaluate")
class EngineSimulationServiceTest {

    private static WireMockServer wireMock;

    @Mock
    private RulePersistencePort rulePort;

    private EngineSimulationService engineSimulationService;
    private RuleSimulationController controller;

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
                ""
        );

        engineSimulationService = new EngineSimulationService(rulePort, ruleEngineClient);
        controller = new RuleSimulationController(engineSimulationService);
    }

    // -----------------------------------------------------------------------
    // Stub helpers
    // -----------------------------------------------------------------------

    private static final String ALLOW_RESPONSE_BODY = """
            {
              "status": 200,
              "success": true,
              "data": {
                "verdict": "ALLOW",
                "trace": [
                  {"nodeId": "$order",    "type": "COND", "operator": "order",    "result": true, "reason": null},
                  {"nodeId": "$customer", "type": "COND", "operator": "customer", "result": true, "reason": null}
                ],
                "matchedNodes": ["$order", "$customer"],
                "unmatchedNodes": [],
                "reasonCodes": []
              }
            }
            """;

    private static final String DENY_RESPONSE_BODY = """
            {
              "status": 200,
              "success": true,
              "data": {
                "verdict": "DENY",
                "trace": [],
                "matchedNodes": [],
                "unmatchedNodes": ["spec-4-4-4-example"],
                "reasonCodes": ["RULE_NOT_REGISTERED"]
              }
            }
            """;

    private Rule buildPublishedRule(String id) {
        Rule rule = new Rule();
        rule.setId(id);
        rule.setName("Test Rule");
        rule.setBundleHash("hash-abc");
        rule.setState(Rule.RuleState.PUBLISHED);
        return rule;
    }

    // -----------------------------------------------------------------------
    // Tests: EngineSimulationService
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("simulate: engine returns ALLOW + 2 matched nodes → service maps correctly")
    void simulate_engineAllows_twoBoundDeclarations() {
        wireMock.stubFor(post(urlEqualTo("/v1/rules/evaluate"))
                .withHeader("Content-Type", containing("application/json"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(ALLOW_RESPONSE_BODY)));

        String ruleId = "rule-sim-01";
        when(rulePort.findById(ruleId)).thenReturn(Optional.of(buildPublishedRule(ruleId)));

        Map<String, Object> facts = Map.of(
                "order", Map.of("total", 600_000, "currency", "VND"),
                "customer", Map.of("id", "c1", "segments", java.util.List.of("seg_vip"))
        );

        RuleEngineClient.SimulateResponse response = engineSimulationService.simulate(ruleId, facts);

        assertThat(response.verdict()).isEqualTo("ALLOW");
        assertThat(response.matchedNodes()).hasSize(2).containsExactlyInAnyOrder("$order", "$customer");
        assertThat(response.trace()).hasSize(2);
        assertThat(response.unmatchedNodes()).isEmpty();
        assertThat(response.reasonCodes()).isEmpty();

        // Verify engine received correct payload
        wireMock.verify(postRequestedFor(urlEqualTo("/v1/rules/evaluate"))
                .withRequestBody(matchingJsonPath("$.ruleIds[0]", equalTo(ruleId)))
                .withRequestBody(matchingJsonPath("$.mode", equalTo("SIMULATE"))));
    }

    @Test
    @DisplayName("simulate: engine returns DENY + reasonCodes → service maps correctly")
    void simulate_engineDenies_reasonCodeMapped() {
        wireMock.stubFor(post(urlEqualTo("/v1/rules/evaluate"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(DENY_RESPONSE_BODY)));

        String ruleId = "rule-sim-deny";
        when(rulePort.findById(ruleId)).thenReturn(Optional.of(buildPublishedRule(ruleId)));

        RuleEngineClient.SimulateResponse response = engineSimulationService.simulate(
                ruleId, Map.of("order", Map.of("total", 100_000, "currency", "VND")));

        assertThat(response.verdict()).isEqualTo("DENY");
        assertThat(response.matchedNodes()).isEmpty();
        assertThat(response.reasonCodes()).contains("RULE_NOT_REGISTERED");
    }

    @Test
    @DisplayName("simulate: rule not found → IllegalArgumentException")
    void simulate_ruleNotFound_throws() {
        String ruleId = "non-existent";
        when(rulePort.findById(ruleId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> engineSimulationService.simulate(ruleId, Map.of()));
    }

    // -----------------------------------------------------------------------
    // Tests: RuleSimulationController
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("controller: ALLOW response → verdict=ALLOW, 2 matched nodes in response")
    void controller_allowResponse_mappedToDto() {
        wireMock.stubFor(post(urlEqualTo("/v1/rules/evaluate"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(ALLOW_RESPONSE_BODY)));

        String ruleId = "rule-ctrl-01";
        when(rulePort.findById(ruleId)).thenReturn(Optional.of(buildPublishedRule(ruleId)));

        RuleEngineSimulateRequest request = new RuleEngineSimulateRequest();
        request.setFacts(Map.of(
                "order", Map.of("total", 600_000, "currency", "VND"),
                "customer", Map.of("id", "c1", "segments", java.util.List.of("seg_vip"))
        ));

        var httpResponse = controller.simulate(ruleId, request);
        RuleEngineSimulateResponse body = httpResponse.getBody();

        assertThat(body).isNotNull();
        assertThat(body.verdict()).isEqualTo("ALLOW");
        assertThat(body.matchedNodes()).hasSize(2).containsExactlyInAnyOrder("$order", "$customer");
        assertThat(body.trace()).hasSize(2);
    }

    // -----------------------------------------------------------------------
    // Test infrastructure
    // -----------------------------------------------------------------------

    private static class TestWebClientRuleEngineAdapter extends WebClientRuleEngineAdapter {
        public TestWebClientRuleEngineAdapter(WebClient.Builder builder, String url, String path) {
            super(builder, url, path);
        }
    }
}
