package vn.viettel.vds.promotion.validation.adapter.out.external;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class OrderServiceAdapter {
    private static final Logger log = LoggerFactory.getLogger(OrderServiceAdapter.class);

    private final OrderServiceFeignClient feignClient;

    public OrderServiceAdapter(OrderServiceFeignClient feignClient) {
        this.feignClient = feignClient;
    }

    public CompletableFuture<Map<String, Object>> getOrderById(String orderId) {
        try {
            return feignClient.getOrderById(orderId);
        } catch (Exception e) {
            log.error("Failed to fetch order {}: {}", orderId, e.getMessage());
            return CompletableFuture.completedFuture(Map.of());
        }
    }

    public CompletableFuture<Map<String, Object>> getOrderHistory(String customerId, int limit) {
        try {
            return feignClient.getOrderHistory(customerId, limit);
        } catch (Exception e) {
            log.error("Failed to fetch order history: {}", e.getMessage());
            return CompletableFuture.completedFuture(Map.of());
        }
    }
}