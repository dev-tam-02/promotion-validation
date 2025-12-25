package vn.viettel.vds.promotion.validation.adapter.out.integration;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(
        name = "validation-engine-proxy-client",
        url = "${integration.validation-engine.url:http://validation-engine:8080}",
        path = "${integration.validation-engine.service-path:/promotion/promotion-validation-engine}",
        fallback = ValidationProxyFeignClientFallback.class
)
public interface ValidationProxyFeignClient {

    @PostMapping("/v1/fast-check")
    Map<String, Object> performFastCheck(@RequestBody Map<String, Object> request);

    @PostMapping("/v1/execute")
    Map<String, Object> performExecution(@RequestBody Map<String, Object> request);
}