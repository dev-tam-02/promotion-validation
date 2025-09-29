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
 *
 * Usage:
 * 1. Start validation-engine on port 8082
 * 2. Start validation service
 * 3. Call these endpoints to test integration
 *
 * Available endpoints:
 * - POST /api/test/compile - Test rule compilation
 * - POST /api/test/execute - Test rule execution
 * - GET /api/test/status/{bundleHash} - Test bundle status
 */
@RestController
@ResponseWrapper
@RequestMapping("/api/test")
public class ValidationEngineTestController {

    private final ValidationEngineClient validationEngineClient;

    public ValidationEngineTestController(ValidationEngineClient validationEngineClient) {
        this.validationEngineClient = validationEngineClient;
    }

    @PostMapping("/compile")
    public CompileResponse testCompile() {
        CompileRequest request = createSampleCompileRequest();
        return validationEngineClient.compileRule(request);
    }

    @PostMapping("/execute")
    public Map<String, Object> testExecute(@RequestParam(required = false) String bundleHash) {
        // First compile if no bundle hash provided
        if (bundleHash == null) {
            CompileRequest compileRequest = createSampleCompileRequest();
            CompileResponse compileResponse = validationEngineClient.compileRule(compileRequest);
            bundleHash = compileResponse.getBundleHash();
        }

        // Test both ALLOW and DENY scenarios
        ExecuteRequest allowRequest = createAllowExecuteRequest(bundleHash);
        ExecuteRequest denyRequest = createDenyExecuteRequest(bundleHash);

        ExecuteResponse allowResponse = validationEngineClient.executeRule(allowRequest);
        ExecuteResponse denyResponse = validationEngineClient.executeRule(denyRequest);

        return Map.of(
            "bundleHash", bundleHash,
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
            CompileResponse compileResponse = validationEngineClient.compileRule(compileRequest);
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
            CompileResponse compileResponse = validationEngineClient.compileRule(compileRequest);
            bundleHash = compileResponse.getBundleHash();
        }

        WarmupRequest request = new WarmupRequest();
        request.setBundleHash(bundleHash);

        validationEngineClient.warmupBundle(request);

        return Map.of(
            "status", "success",
            "bundleHash", bundleHash,
            "message", "Bundle warmed up successfully"
        );
    }

    @GetMapping("/health-check")
    public Map<String, Object> healthCheck() {
        try {
            // Try a simple compile to test connectivity
            CompileRequest request = createSampleCompileRequest();
            CompileResponse response = validationEngineClient.compileRule(request);

            return Map.of(
                "status", "healthy",
                "validationEngine", "connected",
                "drools", "working",
                "lastTest", Instant.now(),
                "bundleHash", response.getBundleHash()
            );
        } catch (Exception e) {
            return Map.of(
                "status", "unhealthy",
                "validationEngine", "disconnected",
                "error", e.getMessage(),
                "lastTest", Instant.now()
            );
        }
    }

    // Helper methods

    private CompileRequest createSampleCompileRequest() {
        CompileRequest request = new CompileRequest();
        request.setTenantId("DEFAULT");
        request.setRuleId("vip_weekend_rule");
        request.setVersion(1);
        request.setLogic("ALL");

        // Create rule nodes matching our sample data
        RuleNodeDto groupNode = new RuleNodeDto();
        groupNode.setId("n1");
        groupNode.setType("GROUP");
        groupNode.setGroupLogic("ALL");

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

        groupNode.setChildren(List.of("n2", "n3"));
        request.setNodes(List.of(groupNode, segmentNode, amountNode));


        request.setOperatorsFingerprint("sha256:abc123def456");

        return request;
    }

    private ExecuteRequest createAllowExecuteRequest(String bundleHash) {
        ExecuteRequest request = new ExecuteRequest();
        request.setBundleHash(bundleHash);

        // VIP customer context
        CustomerDto customer = new CustomerDto();
        customer.setId("CUST_VIP_001");
        customer.setSegments(List.of("VIP"));
        request.setCustomer(customer);

        // Order with 600K (above 500K minimum)
        OrderDto order = new OrderDto();
        order.setTotal(BigDecimal.valueOf(600000));
        order.setCurrency("VND");
        request.setOrder(order);

        // Candidate for VIP customer test
        CandidateDto candidate = new CandidateDto();
        candidate.setId("VIP_WEEKEND_500K");
        candidate.setType("VOUCHER");
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

        // STANDARD customer (not VIP)
        CustomerDto customer = new CustomerDto();
        customer.setId("CUST_STANDARD_001");
        customer.setSegments(List.of("STANDARD"));
        request.setCustomer(customer);

        // Order with 600K (meets amount but wrong segment)
        OrderDto order = new OrderDto();
        order.setTotal(BigDecimal.valueOf(600000));
        order.setCurrency("VND");
        request.setOrder(order);

        // Candidate for standard customer test
        CandidateDto candidate = new CandidateDto();
        candidate.setId("VIP_WEEKEND_500K");
        candidate.setType("VOUCHER");
        request.setCandidate(candidate);

        ExecutionContextDto context = new ExecutionContextDto();
        context.setNow(Instant.now());
        context.setTimezone("Asia/Ho_Chi_Minh");
        request.setExecutionContext(context);

        return request;
    }
}