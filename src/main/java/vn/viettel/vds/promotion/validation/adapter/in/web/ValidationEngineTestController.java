package vn.viettel.vds.promotion.validation.adapter.in.web;

import com.promix.platform.web.annotation.ResponseWrapper;
import org.springframework.web.bind.annotation.*;
import vn.viettel.vds.promotion.validation.adapter.out.integration.ValidationEngineClient;
import vn.viettel.vds.promotion.validation.adapter.out.integration.dto.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Test controller to manually test validation-engine integration
 * <p>
 * Usage:
 * 1. Start validation-engine on port 8082
 * 2. Start validation service
 * 3. Call these endpoints to test integration
 * <p>
 * Available endpoints:
 * - POST /api/test/compile - Test rule compilation
 * - POST /api/test/execute - Test rule execution
 * - GET /api/test/status/{bundleHash} - Test bundle status
 */
@RestController
@ResponseWrapper
@RequestMapping("/api/test")
public class ValidationEngineTestController {

    private static final String BUNDLE_HASH_KEY = "bundleHash";
    private static final String STATUS_KEY = "status";

    private final ValidationEngineClient validationEngineClient;

    public ValidationEngineTestController(ValidationEngineClient validationEngineClient) {
        this.validationEngineClient = validationEngineClient;
    }

    @PostMapping("/compile")
    public CompileResponse testCompile() {
        CompileRequest request = createSampleCompileRequest();
        return validationEngineClient.compile(request);
    }

    @PostMapping("/execute")
    public Map<String, Object> testExecute(@RequestParam(required = false) String bundleHash) {
        // First compile if no bundle hash provided
        if (bundleHash == null) {
            CompileRequest compileRequest = createSampleCompileRequest();
            CompileResponse compileResponse = validationEngineClient.compile(compileRequest);
            bundleHash = compileResponse.getBundleHash();
        }

        // Test both ALLOW and DENY scenarios
        ExecuteRequest allowRequest = createAllowExecuteRequest(bundleHash);
        ExecuteRequest denyRequest = createDenyExecuteRequest(bundleHash);

        ExecuteResponse allowResponse = validationEngineClient.execute(allowRequest);
        ExecuteResponse denyResponse = validationEngineClient.execute(denyRequest);

        return Map.of(
                BUNDLE_HASH_KEY, bundleHash,
                "vipCustomer", Map.of(
                        "decision", allowResponse.getDecision(),
                        "reasonCodes", allowResponse.getReasonCodes(),
                        "executionTimeMs", allowResponse.getEngine() != null ? allowResponse.getEngine().getLatencyMs() : null
                ),
                "standardCustomer", Map.of(
                        "decision", denyResponse.getDecision(),
                        "reasonCodes", denyResponse.getReasonCodes(),
                        "executionTimeMs", denyResponse.getEngine() != null ? denyResponse.getEngine().getLatencyMs() : null
                )
        );
    }

    @GetMapping("/status/{bundleHash}")
    public BundleStatusResponse testStatus(@PathVariable String bundleHash) {
        return validationEngineClient.getBundleStatus(bundleHash);
    }

    @PostMapping("/batch")
    public List<ExecuteResponse> testBatch(@RequestParam(required = false) String bundleHash) {
        // First compile if no bundle hash provided
        if (bundleHash == null) {
            CompileRequest compileRequest = createSampleCompileRequest();
            CompileResponse compileResponse = validationEngineClient.compile(compileRequest);
            bundleHash = compileResponse.getBundleHash();
        }

        List<ExecuteRequest> requests = List.of(
                createAllowExecuteRequest(bundleHash),
                createDenyExecuteRequest(bundleHash)
        );

        return validationEngineClient.executeBatch(requests);
    }

    @PostMapping("/warmup")
    public Map<String, String> testWarmup(@RequestParam(required = false) String bundleHash) {
        // First compile if no bundle hash provided
        if (bundleHash == null) {
            CompileRequest compileRequest = createSampleCompileRequest();
            CompileResponse compileResponse = validationEngineClient.compile(compileRequest);
            bundleHash = compileResponse.getBundleHash();
        }

        WarmupRequest request = new WarmupRequest();
        request.setBundleHash(bundleHash);

        validationEngineClient.warmup(request);

        return Map.of(
                STATUS_KEY, "success",
                BUNDLE_HASH_KEY, bundleHash,
                "message", "Bundle warmed up successfully"
        );
    }

    @GetMapping("/health-check")
    public Map<String, Object> healthCheck() {
        try {
            // Try a simple compile to test connectivity
            CompileRequest request = createSampleCompileRequest();
            CompileResponse response = validationEngineClient.compile(request);

            return Map.of(
                    STATUS_KEY, "healthy",
                    "validationEngine", "connected",
                    "drools", "working",
                    "lastTest", Instant.now(),
                    BUNDLE_HASH_KEY, response.getBundleHash()
            );
        } catch (Exception e) {
            return Map.of(
                    STATUS_KEY, "unhealthy",
                    "validationEngine", "disconnected",
                    "error", e.getMessage(),
                    "lastTest", Instant.now()
            );
        }
    }

    // Helper methods

    private CompileRequest createSampleCompileRequest() {
        CompileRequest request = new CompileRequest();
        request.setTenantId("default");
        request.setRuleId("vip_weekend_rule");
        request.setVersion(1);
        request.setLogic("ALL");

        // Create rule nodes as RuleNodeDto objects
        RuleNodeDto groupNode = new RuleNodeDto();
        groupNode.setId("n1");
        groupNode.setType("GROUP");
        groupNode.setGroupLogic("ALL");
        groupNode.setChildren(List.of("n2", "n3"));

        RuleNodeDto segmentNode = new RuleNodeDto();
        segmentNode.setId("n2");
        segmentNode.setType("COND");
        segmentNode.setOperatorName("customer.segment.in");
        segmentNode.setParams(Map.of("segments", List.of("VIP")));
        segmentNode.setReasonCode("CUSTOMER_SEGMENT_VIP");

        RuleNodeDto amountNode = new RuleNodeDto();
        amountNode.setId("n3");
        amountNode.setType("COND");
        amountNode.setOperatorName("order.amount.gte");
        amountNode.setParams(Map.of("amount", 500000, "currency", "VND"));
        amountNode.setReasonCode("ORDER_AMOUNT_MIN");

        request.setNodes(List.of(groupNode, segmentNode, amountNode));
        request.setOperatorsFingerprint("sha256:abc123def456");

        return request;
    }

    private ExecuteRequest createAllowExecuteRequest(String bundleHash) {
        ExecuteRequest request = new ExecuteRequest();
        request.setBundleHash(bundleHash);

        // VIP customer context - using record constructor
        CustomerDto customer = new CustomerDto(
                "CUST_VIP_001",
                List.of("VIP"),
                null,  // region
                null,  // tier
                null   // metadata
        );
        request.setCustomer(customer);

        // Order with 600K (above 500K minimum) - using record constructor
        OrderDto order = new OrderDto(
                "ORDER_001",
                BigDecimal.valueOf(600000),
                "VND",
                null,  // items
                null   // metadata
        );
        request.setOrder(order);

        // Candidate for VIP customer test - using record constructor
        CandidateDto candidate = new CandidateDto(
                "VIP_WEEKEND_500K",
                "VOUCHER",
                null  // metadata
        );
        request.setCandidate(candidate);

        ExecutionContextDto context = new ExecutionContextDto();
        context.setNow(Instant.now());
        context.setTimezone("Asia/Ho_Chi_Minh");
        request.setExecutionContext(context);

        return request;
    }

    private ExecuteRequest createDenyExecuteRequest(String bundleHash) {
        ExecuteRequest request = new ExecuteRequest();
        request.setBundleHash(bundleHash);

        // STANDARD customer (not VIP) - using record constructor
        CustomerDto customer = new CustomerDto(
                "CUST_STANDARD_001",
                List.of("STANDARD"),
                null,  // region
                null,  // tier
                null   // metadata
        );
        request.setCustomer(customer);

        // Order with 600K (meets amount but wrong segment) - using record constructor
        OrderDto order = new OrderDto(
                "ORDER_002",
                BigDecimal.valueOf(600000),
                "VND",
                null,  // items
                null   // metadata
        );
        request.setOrder(order);

        // Candidate for standard customer test - using record constructor
        CandidateDto candidate = new CandidateDto(
                "VIP_WEEKEND_500K",
                "VOUCHER",
                null  // metadata
        );
        request.setCandidate(candidate);

        ExecutionContextDto context = new ExecutionContextDto();
        context.setNow(Instant.now());
        context.setTimezone("Asia/Ho_Chi_Minh");
        request.setExecutionContext(context);

        return request;
    }
}
