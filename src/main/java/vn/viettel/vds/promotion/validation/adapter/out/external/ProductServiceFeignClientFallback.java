package vn.viettel.vds.promotion.validation.adapter.out.external;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Fallback for {@link ProductServiceFeignClient}.
 * Returns empty page envelopes when pp-product is unavailable so the rule
 * builder UI degrades gracefully (Products rules show empty list, not error).
 */
@Component
public class ProductServiceFeignClientFallback implements ProductServiceFeignClient {

    private static final Logger logger = LoggerFactory.getLogger(ProductServiceFeignClientFallback.class);

    @Override
    public Map<String, Object> listProducts(String productName, String skuName, Boolean onlyProduct,
                                            Integer page, Integer size) {
        logger.warn("Product service unavailable (listProducts name={}, sku={}); returning empty page",
                productName, skuName);
        return emptyPage();
    }

    @Override
    public Map<String, Object> listCollections(String status, String search, Integer page, Integer size) {
        logger.warn("Product service unavailable (listCollections status={}, search={}); returning empty page",
                status, search);
        return emptyPage();
    }

    @Override
    public Map<String, Object> listSkus(String name, List<String> ids, Integer page, Integer size) {
        logger.warn("Product service unavailable (listSkus name={}, ids={}); returning empty page",
                name, ids != null ? ids.size() : 0);
        return emptyPage();
    }

    @Override
    public Map<String, Object> lookupProductById(String id) {
        logger.warn("Product service unavailable (lookupProductById id={}); returning empty data", id);
        return Map.of("data", Collections.emptyMap());
    }

    @Override
    public Map<String, Object> getCollectionById(String id) {
        logger.warn("Product service unavailable (getCollectionById id={}); returning empty data", id);
        return Map.of("data", Collections.emptyMap());
    }

    private Map<String, Object> emptyPage() {
        return Map.of(
                "data", Map.of(
                        "content", Collections.emptyList(),
                        "totalElements", 0
                )
        );
    }
}
