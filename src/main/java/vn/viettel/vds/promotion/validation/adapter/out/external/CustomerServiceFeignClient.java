package vn.viettel.vds.promotion.validation.adapter.out.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Feign client for customer service
 */
@FeignClient(
    name = "customer-service",
    url = "${external.customer-service.url:http://customer-service}",
    configuration = ExternalServiceFeignConfig.class
)
public interface CustomerServiceFeignClient {

    /**
     * Get customer by ID
     */
    @GetMapping(value = "/api/v1/customers/{customerId}",
                produces = MediaType.APPLICATION_JSON_VALUE)
    CompletableFuture<Map<String, Object>> getCustomerById(@PathVariable("customerId") String customerId);

    /**
     * Get customer segments
     */
    @GetMapping(value = "/api/v1/customers/{customerId}/segments",
                produces = MediaType.APPLICATION_JSON_VALUE)
    CompletableFuture<Map<String, Object>> getCustomerSegments(@PathVariable("customerId") String customerId);
}