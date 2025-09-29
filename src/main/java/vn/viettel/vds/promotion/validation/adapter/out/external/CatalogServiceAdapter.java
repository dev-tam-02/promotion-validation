package vn.viettel.vds.promotion.validation.adapter.out.external;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class CatalogServiceAdapter {
    private static final Logger log = LoggerFactory.getLogger(CatalogServiceAdapter.class);

    private final CatalogServiceFeignClient feignClient;

    public CatalogServiceAdapter(CatalogServiceFeignClient feignClient) {
        this.feignClient = feignClient;
    }

    public CompletableFuture<Map<String, Object>> getProductById(String productId) {
        try {
            return feignClient.getProductById(productId);
        } catch (Exception e) {
            log.error("Failed to fetch product {}: {}", productId, e.getMessage());
            return CompletableFuture.completedFuture(Map.of());
        }
    }

    public CompletableFuture<List<Map<String, Object>>> getProductsByIds(List<String> productIds) {
        try {
            Map<String, List<String>> request = Map.of("ids", productIds);
            return feignClient.getProductsByIds(request);
        } catch (Exception e) {
            log.error("Failed to fetch products batch: {}", e.getMessage());
            return CompletableFuture.completedFuture(List.of());
        }
    }

    public CompletableFuture<Map<String, Object>> getCategoryById(String categoryId) {
        try {
            return feignClient.getCategoryById(categoryId);
        } catch (Exception e) {
            log.error("Failed to fetch category: {}", e.getMessage());
            return CompletableFuture.completedFuture(Map.of());
        }
    }
}