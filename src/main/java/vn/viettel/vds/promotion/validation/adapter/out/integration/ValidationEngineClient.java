package vn.viettel.vds.promotion.validation.adapter.out.integration;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.viettel.vds.promotion.validation.adapter.out.integration.dto.*;

import java.util.List;
import java.util.Map;

@FeignClient(
        name = "validation-engine",
        contextId = "validation-engine-client",
        path = "/promotion/promotion-rule-engine",
        fallback = ValidationEngineClientFallback.class
)
public interface ValidationEngineClient {

    @PostMapping("/v1/compiler/compile")
    CompileResponse compile(@RequestBody CompileRequest request);

    @PostMapping("/v1/execute")
    ExecuteResponse execute(@RequestBody ExecuteRequest request);

    @PostMapping("/v1/execute/batch")
    List<ExecuteResponse> executeBatch(@RequestBody List<ExecuteRequest> requests);

    @PostMapping("/v1/compile/warmup")
    WarmupResponse warmup(@RequestBody WarmupRequest request);

    @GetMapping("/v1/compile/bundle/{bundleHash}/status")
    BundleStatusResponse getBundleStatus(@PathVariable("bundleHash") String bundleHash);

    @PostMapping("/v1/rules/deploy")
    DeployResponse deployRuleSet(@RequestParam("ruleSetId") String ruleSetId, @RequestBody Map<String, Object> compiledRules);

    @GetMapping("/v1/operators/supported")
    List<String> getSupportedOperators();

    @GetMapping("/actuator/health")
    ResponseEntity<String> getHealth();
}