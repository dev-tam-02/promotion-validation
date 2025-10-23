package vn.viettel.vds.promotion.validation.adapter.out.integration;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(
        name = "validation-engine",
        contextId = "validation-engine-proxy-client",
        path = "/promotion/promotion-rule-engine",
        fallback = ValidationProxyFeignClientFallback.class
)
public interface ValidationProxyFeignClient {

    @PostMapping("/v1/fast-check")
    Map<String, Object> performFastCheck(@RequestBody Map<String, Object> request);

    @PostMapping("/v1/execute")
    Map<String, Object> performExecution(@RequestBody Map<String, Object> request);
}