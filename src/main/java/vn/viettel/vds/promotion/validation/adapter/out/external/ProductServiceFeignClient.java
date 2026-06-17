package vn.viettel.vds.promotion.validation.adapter.out.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * Feign client for pp-product.
 *
 * <p>Used by RuleBuilder Products rules to populate a grouped option list
 * (PRODUCT + COLLECTION). pp-product does not currently expose a SKU search
 * endpoint, so the SKU group is left empty for now (tracked as a follow-up
 * task in the pp-product backlog).
 *
 * <p>Base URL comes from {@code external.services.product.url}
 * ({@code host:port}); full context-path inlined per existing convention.
 */
@FeignClient(
        name = "product-service",
        url = "${external.services.product.url}",
        configuration = ExternalServiceFeignConfig.class,
        fallback = ProductServiceFeignClientFallback.class
)
public interface ProductServiceFeignClient {

    /**
     * GET /promotion/promotion-product/api/v1/products
     * Returns paginated products. Set onlyProduct=false to include SKUs flattened.
     */
    @GetMapping(
            value = "/promotion/promotion-product/api/v1/products",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    Map<String, Object> listProducts(
            @RequestParam(value = "productName", required = false) String productName,
            @RequestParam(value = "skuName", required = false) String skuName,
            @RequestParam(value = "onlyProduct", required = false, defaultValue = "false") Boolean onlyProduct,
            @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "20") Integer size
    );

    /**
     * GET /promotion/promotion-product/api/v1/collections
     * Returns paginated collections filtered by status (default ACTIVE).
     */
    @GetMapping(
            value = "/promotion/promotion-product/api/v1/collections",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    Map<String, Object> listCollections(
            @RequestParam(value = "status", required = false, defaultValue = "ACTIVE") String status,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "20") Integer size
    );

    /**
     * GET /promotion/promotion-product/api/v1/skus
     * Returns paginated SKUs with case-sensitive LIKE %name% search.
     */
    @GetMapping(
            value = "/promotion/promotion-product/api/v1/skus",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    Map<String, Object> listSkus(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "ids", required = false) List<String> ids,
            @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "20") Integer size
    );

    /**
     * GET /promotion/promotion-product/api/v1/products/lookup?id={id}
     * Resolve a single product by id. Response {@code data} carries {@code id} + {@code name}.
     * Used by the by-id value-resolution path (display/edit), not the browse dropdown.
     */
    @GetMapping(
            value = "/promotion/promotion-product/api/v1/products/lookup",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    Map<String, Object> lookupProductById(@RequestParam(value = "id") String id);

    /**
     * GET /promotion/promotion-product/api/v1/collections/{id}
     * Resolve a single collection by id. Response {@code data} carries {@code id} + {@code name}.
     */
    @GetMapping(
            value = "/promotion/promotion-product/api/v1/collections/{id}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    Map<String, Object> getCollectionById(@PathVariable("id") String id);
}
