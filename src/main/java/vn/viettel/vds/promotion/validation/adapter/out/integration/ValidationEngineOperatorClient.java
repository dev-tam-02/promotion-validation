package vn.viettel.vds.promotion.validation.adapter.out.integration;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import vn.viettel.vds.promotion.validation.adapter.out.integration.dto.SupportedOperatorDto;
import vn.viettel.vds.promotion.validation.adapter.out.integration.dto.ValidateEngineOperatorsRequest;
import vn.viettel.vds.promotion.validation.adapter.out.integration.dto.ValidateEngineOperatorsResponse;

import java.util.List;

@FeignClient(
        name = "validation-engine-operator-client",
        url = "${integration.validation-engine.url}",
        path = "/promotion/promotion-rule-engine",
        fallback = ValidationEngineOperatorClientFallback.class
)
public interface ValidationEngineOperatorClient {

    @GetMapping("/v1/operators/supported")
    List<SupportedOperatorDto> getSupportedOperators();

    @GetMapping("/v1/operators/supported/names")
    List<String> getSupportedOperatorNames();

    @PostMapping("/v1/operators/validate")
    ValidateEngineOperatorsResponse validateOperators(@RequestBody ValidateEngineOperatorsRequest request);

    @GetMapping("/v1/operators/supported/{operatorName}")
    Boolean isOperatorSupported(@PathVariable("operatorName") String operatorName,
                                @RequestParam(value = "version", required = false) Integer version);
}