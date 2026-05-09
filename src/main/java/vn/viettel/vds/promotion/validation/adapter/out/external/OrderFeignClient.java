package vn.viettel.vds.promotion.validation.adapter.out.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * Feign client for communicating with Order microservice.
 * Uses direct URL for service resolution.
 * <p>
 * Configuration:
 * - name: Client name for identification
 * - url: Direct URL to order service
 * - path: Base path for all order API endpoints
 *
 * @author Validation Module Team
 * @since 1.0.0
 */
@FeignClient(
        name = "order-feign-client",
        url = "${promix.validation.external.order.url}",
        path = "${promix.validation.external.order.path:/api/v1/orders}"
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
