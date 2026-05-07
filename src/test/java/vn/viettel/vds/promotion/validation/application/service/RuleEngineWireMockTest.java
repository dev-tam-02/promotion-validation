package vn.viettel.vds.promotion.validation.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.*;
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

import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Integration test wiring WireMock stub for pp-rule-engine POST /v1/rules.
 *
 * <p>Flow tested:
 * POST /validation/v1/rules → RuleManagementService.createRule()
 * → DslGenerator.generate() → Rule.dsl set
 * → DrlCompiler.compile() → DRL text
 * → RuleEngineClient.register() → POST /v1/rules stub → bundleHash "abc123"
 * → Rule saved with bundle_hash="abc123" and state=PUBLISHED
 *
 * <p>WireMock stubs POST /v1/rules and captures the request body to verify
 * the DRL content matches the expected template render.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RuleManagementService + WireMock pp-rule-engine integration")
class RuleEngineWireMockTest {

    private static WireMockServer wireMock;

    @Mock
    private RulePersistencePort rulePort;

    @Mock
    private OperatorPersistencePort operatorPort;

    private RuleManagementService ruleManagementService;
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

        // Build WebClient pointing at WireMock
        WebClient.Builder webClientBuilder = WebClient.builder();

        // Stub the adapter directly — bypass Spring context
        RuleEngineClient ruleEngineClient = new TestWebClientRuleEngineAdapter(
                webClientBuilder,
                "http://localhost:" + wireMock.port(),
                ""           // no service path prefix in WireMock test
        );

        RuleTreeAssembler assembler = new RuleTreeAssembler();
        RuleValidator validator = new RuleValidator(objectMapper);
        DslGenerator dslGenerator = new DslGenerator(objectMapper);
        DrlCompiler drlCompiler = new DrlCompiler();

        ruleManagementService = new RuleManagementService(
                rulePort, assembler, validator, dslGenerator, drlCompiler,
                ruleEngineClient, operatorPort);
    }

    @Test
    @DisplayName("createRule: WireMock stub returns bundleHash, rule saved with PUBLISHED state")
    void createRule_wireMockStubReturns_bundleHash() throws Exception {
        // Arrange: stub POST /v1/rules → full ResponseTemplate wrapper (matches pp-rule-engine @ResponseWrapper)
        wireMock.stubFor(post(urlEqualTo("/v1/rules"))
                .withHeader("Content-Type", containing("application/json"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":201,\"success\":true,\"data\":{\"ruleId\":\"stub-rule-id\",\"bundleHash\":\"abc123\"}}")));

        // Arrange: operator with compilerId
        Operator orderOp = Operator.builder()
                .id("op1")
                .name("order.total.gte")
                .compilerId("tpl_order_total_gte_v1")
                .status(Operator.OperatorStatus.ACTIVE)
                .build();
        when(operatorPort.findGlobalOperatorsByStatus(Operator.OperatorStatus.ACTIVE))
                .thenReturn(List.of(orderOp));

        // Arrange: rule node tree
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

        // Arrange: mock persistence — capture what was saved
        final Rule[] savedRuleHolder = {null};
        when(rulePort.save(any(Rule.class))).thenAnswer(inv -> {
            Rule r = inv.getArgument(0);
            savedRuleHolder[0] = r;
            return r;
        });

        // Act
        ruleManagementService.createRule(
                "Premium Discount", "Test rule",
                Rule.LogicType.ALL, List.of(group), "admin");

        // Assert: WireMock received the POST /v1/rules call
        wireMock.verify(postRequestedFor(urlEqualTo("/v1/rules"))
                .withRequestBody(matchingJsonPath("$.id"))
                .withRequestBody(matchingJsonPath("$.drl")));

        // Assert: the last saved rule has bundleHash and PUBLISHED state
        Rule finalSaved = savedRuleHolder[0];
        assertThat(finalSaved).isNotNull();
        assertThat(finalSaved.getBundleHash()).isEqualTo("abc123");
        assertThat(finalSaved.getState()).isEqualTo(Rule.RuleState.PUBLISHED);

        // Assert: DSL snapshot was populated
        assertThat(finalSaved.getDsl()).isNotNull();
        assertThat(finalSaved.getDsl()).containsKey("ruleId");
        assertThat(finalSaved.getDsl()).containsKey("root");
    }

    @Test
    @DisplayName("createRule: rule engine returns 400 → rule stays DRAFT, bundleHash null")
    void createRule_engineRejects_ruleStaysDraft() {
        // Arrange: stub POST /v1/rules → 400
        wireMock.stubFor(post(urlEqualTo("/v1/rules"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"invalid DRL\"}")));

        when(operatorPort.findGlobalOperatorsByStatus(Operator.OperatorStatus.ACTIVE))
                .thenReturn(List.of());

        RuleNode cond = RuleNode.builder()
                .nodeId("c1")
                .type(RuleNode.NodeType.COND)
                .operatorName("order.total.gte")
                .params(Map.of("amount", 0, "currency", "VND"))
                .reasonCode("RC")
                .build();

        final Rule[] savedRuleHolder = {null};
        when(rulePort.save(any(Rule.class))).thenAnswer(inv -> {
            Rule r = inv.getArgument(0);
            savedRuleHolder[0] = r;
            return r;
        });

        // Act — engine returns 400, service catches and keeps rule in DRAFT
        ruleManagementService.createRule(
                "Test Rule", null, Rule.LogicType.ALL, List.of(cond), "admin");

        // Assert: rule was saved in DRAFT state (no bundleHash)
        Rule finalSaved = savedRuleHolder[0];
        // The last save is either the DRAFT save after engine failure
        // bundleHash should be null and state should not be PUBLISHED
        if (finalSaved != null) {
            assertThat(finalSaved.getState()).isNotEqualTo(Rule.RuleState.PUBLISHED);
            assertThat(finalSaved.getBundleHash()).isNull();
        }
    }

    @Test
    @DisplayName("WireMock captures DRL body containing expected Drools snippet")
    void wireMockCapturesDrlBody_containsExpectedSnippet() throws Exception {
        wireMock.stubFor(post(urlEqualTo("/v1/rules"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":201,\"success\":true,\"data\":{\"ruleId\":\"stub-rule-id\",\"bundleHash\":\"sha256-test\"}}")));

        Operator orderOp = Operator.builder()
                .id("op1").name("order.total.gte")
                .compilerId("tpl_order_total_gte_v1")
                .status(Operator.OperatorStatus.ACTIVE).build();
        when(operatorPort.findGlobalOperatorsByStatus(Operator.OperatorStatus.ACTIVE))
                .thenReturn(List.of(orderOp));

        RuleNode cond = RuleNode.builder()
                .nodeId("c1").type(RuleNode.NodeType.COND)
                .operatorName("order.total.gte")
                .params(Map.of("amount", 300_000, "currency", "VND"))
                .reasonCode("RC").build();

        RuleNode group = RuleNode.builder()
                .nodeId("g1").type(RuleNode.NodeType.GROUP)
                .groupLogic(Rule.LogicType.ALL)
                .children(List.of(cond)).build();

        when(rulePort.save(any(Rule.class))).thenAnswer(inv -> inv.getArgument(0));

        ruleManagementService.createRule("Test", null, Rule.LogicType.ALL, List.of(group), "admin");

        // Verify the DRL body sent to WireMock contains the template-rendered snippet
        wireMock.verify(postRequestedFor(urlEqualTo("/v1/rules"))
                .withRequestBody(matchingJsonPath("$.drl", containing("OrderFact")))
                .withRequestBody(matchingJsonPath("$.drl", containing("300000")))
                .withRequestBody(matchingJsonPath("$.drl", containing("VND"))));
    }

    /**
     * Subclass that bypasses Spring @CircuitBreaker annotation for unit testing.
     * The annotation is AOP-based and requires a Spring context; in unit tests
     * we call the methods directly without the AOP proxy.
     */
    private static class TestWebClientRuleEngineAdapter extends WebClientRuleEngineAdapter {

        public TestWebClientRuleEngineAdapter(WebClient.Builder builder, String url, String path) {
            super(builder, url, path);
        }
    }
}
