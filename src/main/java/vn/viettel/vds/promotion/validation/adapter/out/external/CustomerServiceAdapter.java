package vn.viettel.vds.promotion.validation.adapter.out.external;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class CustomerServiceAdapter {
    private static final Logger log = LoggerFactory.getLogger(CustomerServiceAdapter.class);

    private final CustomerServiceFeignClient feignClient;

    public CustomerServiceAdapter(CustomerServiceFeignClient feignClient) {
        this.feignClient = feignClient;
    }

    public CompletableFuture<Map<String, Object>> getCustomerById(String customerId) {
        try {
            return feignClient.getCustomerById(customerId);
        } catch (Exception e) {
            log.error("Failed to fetch customer {}: {}", customerId, e.getMessage());
            return CompletableFuture.completedFuture(Map.of());
        }
    }

    public CompletableFuture<Map<String, Object>> getCustomerSegments(String customerId) {
        try {
            return feignClient.getCustomerSegments(customerId);
        } catch (Exception e) {
            log.error("Failed to fetch customer segments: {}", e.getMessage());
            return CompletableFuture.completedFuture(Map.of());
        }
    }
}