package vn.viettel.vds.promotion.validation.adapter.out.integration;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.viettel.vds.promotion.validation.adapter.out.integration.dto.*;

import java.util.List;

@FeignClient(
    name = "validation-engine-service",
    url = "${integration.validation-engine.url:http://localhost:8082}",
    fallback = ValidationEngineClientFallback.class
)
public interface ValidationEngineClient {

    @PostMapping("/v1/compile")
    CompileResponse compileRule(@RequestBody CompileRequest request);

    @PostMapping("/v1/execute")
    ExecuteResponse executeRule(@RequestBody ExecuteRequest request);

    @PostMapping("/v1/execute/batch")
    List<ExecuteResponse> executeBatch(@RequestBody List<ExecuteRequest> requests);

    @PostMapping("/v1/compile/warmup")
    void warmupBundle(@RequestBody WarmupRequest request);

    @GetMapping("/v1/compile/bundle/{bundleHash}/status")
    BundleStatusResponse getBundleStatus(@PathVariable("bundleHash") String bundleHash);

    // Removed obsolete /api/rules/deploy endpoint - use /v1/compile instead

    @GetMapping("/actuator/health")
    ResponseEntity<String> getHealth();
}