package vn.viettel.vds.promotion.validation.adapter.out.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Feign client for catalog service
 */
@FeignClient(
    name = "catalog-service",
    url = "${external.catalog-service.url:http://catalog-service}",
    configuration = ExternalServiceFeignConfig.class
)
public interface CatalogServiceFeignClient {

    /**
     * Get product by ID
     */
    @GetMapping(value = "/api/v1/products/{productId}",
                produces = MediaType.APPLICATION_JSON_VALUE)
    CompletableFuture<Map<String, Object>> getProductById(@PathVariable("productId") String productId);

    /**
     * Get products by batch IDs
     */
    @PostMapping(value = "/api/v1/products/batch",
                 consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    CompletableFuture<List<Map<String, Object>>> getProductsByIds(@RequestBody Map<String, List<String>> request);

    /**
     * Get category by ID
     */
    @GetMapping(value = "/api/v1/categories/{categoryId}",
                produces = MediaType.APPLICATION_JSON_VALUE)
    CompletableFuture<Map<String, Object>> getCategoryById(@PathVariable("categoryId") String categoryId);
}