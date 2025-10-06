package vn.viettel.vds.promotion.validation.adapter.out.integration;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(
        name = "validation-proxy",
        url = "${validation.engine.base-url:http://localhost:8094}",
        fallback = ValidationProxyFeignClientFallback.class
)
public interface ValidationProxyFeignClient {

    @PostMapping("/v1/fast-check")
    Map<String, Object> performFastCheck(@RequestBody Map<String, Object> request);

    @PostMapping("/v1/execute")
    Map<String, Object> performExecution(@RequestBody Map<String, Object> request);
}