package vn.viettel.vds.promotion.validation.adapter.out.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Feign client for order service
 */
@FeignClient(
    name = "order-service",
    url = "${external.order-service.url:http://order-service}",
    configuration = ExternalServiceFeignConfig.class
)
public interface OrderServiceFeignClient {

    /**
     * Get order by ID
     */
    @GetMapping(value = "/api/v1/orders/{orderId}",
                produces = MediaType.APPLICATION_JSON_VALUE)
    CompletableFuture<Map<String, Object>> getOrderById(@PathVariable("orderId") String orderId);

    /**
     * Get order history for customer
     */
    @GetMapping(value = "/api/v1/customers/{customerId}/orders",
                produces = MediaType.APPLICATION_JSON_VALUE)
    CompletableFuture<Map<String, Object>> getOrderHistory(
        @PathVariable("customerId") String customerId,
        @RequestParam("limit") int limit
    );
}