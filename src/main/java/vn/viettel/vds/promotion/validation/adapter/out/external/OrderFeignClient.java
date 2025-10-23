package vn.viettel.vds.promotion.validation.adapter.out.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * Feign client for communicating with Order microservice.
 * Uses Eureka service discovery for dynamic service resolution.
 *
 * Configuration:
 * - name: Eureka service name from application.yml
 * - path: Base path for all order API endpoints
 *
 * @author Validation Module Team
 * @since 1.0.0
 */
@FeignClient(
        name = "${promix.validation.external.order.name}",
        path = "${promix.validation.external.order.path}"
)
public interface OrderFeignClient {

    /**
     * Fetch order data by order ID.
     *
     * @param orderId Order ID
     * @return Order data as Map
     */
    @GetMapping("/{orderId}")
    Map<String, Object> getOrderById(@PathVariable("orderId") String orderId);
}
