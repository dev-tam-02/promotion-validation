package vn.viettel.vds.promotion.validation.adapter.out.integration;

import com.promix.platform.web.template.ResponseTemplate;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.viettel.vds.promotion.validation.adapter.out.integration.dto.*;

import java.util.List;
import java.util.Map;

@FeignClient(
        name = "validation-engine-client",
        url = "${integration.validation-engine.url}",
        path = "${integration.validation-engine.service-path:/promotion/promotion-validation-engine}",
        fallback = ValidationEngineClientFallback.class
)
public interface ValidationEngineClient {

    @PostMapping("/v1/compiler/compile")
    ResponseTemplate<CompileResponse> compile(@RequestBody CompileRequest request);

    @PostMapping("/v1/compile/validation")
    ResponseTemplate<CompileResponse> compileValidation(@RequestBody ValidationCompileRequest request);

    @PostMapping("/v1/execute")
    ResponseTemplate<ExecuteResponse> execute(@RequestBody ExecuteRequest request);

    @PostMapping("/v1/execute/batch")
    ResponseTemplate<List<ExecuteResponse>> executeBatch(@RequestBody List<ExecuteRequest> requests);

    @PostMapping("/v1/compile/warmup")
    ResponseTemplate<WarmupResponse> warmup(@RequestBody WarmupRequest request);

    @GetMapping("/v1/compile/bundle/{bundleHash}/status")
    ResponseTemplate<BundleStatusResponse> getBundleStatus(@PathVariable("bundleHash") String bundleHash);

    @PostMapping("/v1/rules/deploy")
    ResponseTemplate<DeployResponse> deployRuleSet(@RequestParam("ruleSetId") String ruleSetId, @RequestBody Map<String, Object> compiledRules);

    @GetMapping("/v1/operators/supported")
    ResponseTemplate<List<String>> getSupportedOperators();

    @GetMapping("/actuator/health")
    ResponseEntity<String> getHealth();
}