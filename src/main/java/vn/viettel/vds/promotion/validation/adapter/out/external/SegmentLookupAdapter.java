package vn.viettel.vds.promotion.validation.adapter.out.external;

import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.application.port.out.RuleOptionsLookupPort;
import vn.viettel.vds.promotion.validation.application.port.out.RuleOptionsPage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Adapter that routes rule-option lookups to the appropriate external service
 * based on {@code dataSourceType}.
 *
 * <p>Routing strategy:
 * <ul>
 *   <li>{@code "SEGMENT"} → pp-segment via {@link SegmentServiceFeignClient}</li>
 *   <li>{@code "PRODUCT"} → pp-product (products + collections merged, tagged
 *       with metadata.type so the UI can render a grouped autocomplete)</li>
 *   <li>Any other value → log warn + return empty page</li>
 * </ul>
 *
 * <p>{@code dataSourceEndpoint} from the DB is metadata/audit only; this adapter
 * does NOT parse it as a dynamic URL. All paths are hardcoded in the Feign client.
 */
@Component
public class SegmentLookupAdapter implements RuleOptionsLookupPort {

    private static final Logger logger = LoggerFactory.getLogger(SegmentLookupAdapter.class);
    private static final String DATA_SOURCE_SEGMENT = "SEGMENT";
    private static final String DATA_SOURCE_PRODUCT = "PRODUCT";

    private final SegmentServiceFeignClient segmentFeignClient;
    private final ProductServiceFeignClient productFeignClient;

    public SegmentLookupAdapter(SegmentServiceFeignClient segmentFeignClient,
                                ProductServiceFeignClient productFeignClient) {
        this.segmentFeignClient = segmentFeignClient;
        this.productFeignClient = productFeignClient;
    }

    @Override
    public RuleOptionsPage lookup(String dataSourceType, String dataSourceEndpoint,
                                  String search, int page, int size, String tenantId) {
        if (dataSourceType == null) {
            logger.warn("data_source_type is null, returning empty page");
            return RuleOptionsPage.empty();
        }

        return switch (dataSourceType) {
            case DATA_SOURCE_SEGMENT -> fetchSegments(search, page, size);
            case DATA_SOURCE_PRODUCT -> fetchProducts(search, page, size);
            default -> {
                logger.warn("data_source_type '{}' not yet implemented, returning empty page", dataSourceType);
                yield RuleOptionsPage.empty();
            }
        };
    }

    @Override
    public RuleOptionsPage lookupByIds(String dataSourceType, String dataSourceEndpoint,
                                       List<String> ids, String tenantId) {
        if (dataSourceType == null || ids == null || ids.isEmpty()) {
            return RuleOptionsPage.empty();
        }
        return switch (dataSourceType) {
            case DATA_SOURCE_PRODUCT -> resolveProductsByIds(ids);
            case DATA_SOURCE_SEGMENT -> resolveSegmentsByIds(ids);
            default -> {
                logger.warn("lookupByIds: data_source_type '{}' not supported, returning empty", dataSourceType);
                yield RuleOptionsPage.empty();
            }
        };
    }

    /**
     * Resolve each segment id directly via {@code GET /segments/{id}} (no paging,
     * no page-size cap). Segments carry no entity sub-type, so no type badge is
     * attached — matching the browse path. Unresolved ids are omitted.
     */
    private RuleOptionsPage resolveSegmentsByIds(List<String> ids) {
        Map<String, RuleOptionsPage.ValueOption> resolved = new LinkedHashMap<>();
        for (String id : ids) {
            Map<String, Object> raw = safeLookup(() -> segmentFeignClient.getSegmentById(id));
            segmentEntity(raw).ifPresent(vo -> resolved.put(vo.value(), vo));
        }
        List<RuleOptionsPage.ValueOption> ordered = ids.stream()
                .map(resolved::get)
                .filter(java.util.Objects::nonNull)
                .toList();
        return new RuleOptionsPage(ordered, ordered.size());
    }

    /** Map a single-segment {@code {data:{segmentId,name}}} envelope to an option (no type badge). */
    @SuppressWarnings("unchecked")
    private java.util.Optional<RuleOptionsPage.ValueOption> segmentEntity(Map<String, Object> raw) {
        if (raw == null) return java.util.Optional.empty();
        Object dataObj = raw.get("data");
        if (!(dataObj instanceof Map<?, ?> dataMap) || dataMap.isEmpty()) return java.util.Optional.empty();
        Map<String, Object> data = (Map<String, Object>) dataMap;
        String id = stringOrEmpty(data.get("segmentId"));
        String name = stringOrEmpty(data.get("name"));
        if (id.isEmpty()) return java.util.Optional.empty();
        return java.util.Optional.of(new RuleOptionsPage.ValueOption(id, name, name));
    }

    /**
     * Resolve each id directly (no paging): a batch SKU lookup first, then a
     * single product lookup, then a single collection lookup for whatever is left.
     * Each resolved option is tagged with its {@code metadata.type} so the UI can
     * show the right badge. Unresolved ids are simply omitted.
     */
    private RuleOptionsPage resolveProductsByIds(List<String> ids) {
        Set<String> remaining = new LinkedHashSet<>(ids);
        Map<String, RuleOptionsPage.ValueOption> resolved = new LinkedHashMap<>();

        // SKUs — batch by ids in one call.
        try {
            Map<String, Object> raw = productFeignClient.listSkus(null, new ArrayList<>(remaining), 0, remaining.size());
            for (RuleOptionsPage.ValueOption vo : extractPage(raw, "id", "name", "SKU").items) {
                resolved.put(vo.value(), vo);
                remaining.remove(vo.value());
            }
        } catch (FeignException e) {
            logger.error("resolveProductsByIds: SKU batch lookup failed: {}", e.getMessage());
        }

        // Products — single lookup per remaining id.
        for (String id : new ArrayList<>(remaining)) {
            singleEntity(safeLookup(() -> productFeignClient.lookupProductById(id)), "PRODUCT")
                    .ifPresent(vo -> { resolved.put(id, vo); remaining.remove(id); });
        }

        // Collections — single lookup per remaining id.
        for (String id : new ArrayList<>(remaining)) {
            singleEntity(safeLookup(() -> productFeignClient.getCollectionById(id)), "COLLECTION")
                    .ifPresent(vo -> { resolved.put(id, vo); remaining.remove(id); });
        }

        // Preserve the caller's id order.
        List<RuleOptionsPage.ValueOption> ordered = ids.stream()
                .map(resolved::get)
                .filter(java.util.Objects::nonNull)
                .toList();
        return new RuleOptionsPage(ordered, ordered.size());
    }

    private Map<String, Object> safeLookup(java.util.function.Supplier<Map<String, Object>> call) {
        try {
            return call.get();
        } catch (FeignException | ExternalServiceFeignErrorDecoder.ExternalServiceException e) {
            // 404 / not-found for a single by-id lookup just means this id is not
            // this entity type — skip it and let the next source try.
            return null;
        }
    }

    /** Map a single-entity {@code {data:{id,name}}} envelope to a type-tagged option. */
    @SuppressWarnings("unchecked")
    private java.util.Optional<RuleOptionsPage.ValueOption> singleEntity(Map<String, Object> raw, String typeTag) {
        if (raw == null) return java.util.Optional.empty();
        Object dataObj = raw.get("data");
        if (!(dataObj instanceof Map<?, ?> dataMap) || dataMap.isEmpty()) return java.util.Optional.empty();
        Map<String, Object> data = (Map<String, Object>) dataMap;
        String id = stringOrEmpty(data.get("id"));
        String name = stringOrEmpty(data.get("name"));
        if (id.isEmpty()) return java.util.Optional.empty();
        return java.util.Optional.of(new RuleOptionsPage.ValueOption(id, name, name, Map.of("type", typeTag)));
    }

    @SuppressWarnings("unchecked")
    private RuleOptionsPage fetchSegments(String search, int page, int size) {
        try {
            Map<String, Object> raw = segmentFeignClient.searchSegments(page, size, search, null);
            Map<String, Object> data = (Map<String, Object>) raw.get("data");
            if (data == null) {
                logger.warn("Segment service returned null data envelope, returning empty page");
                return RuleOptionsPage.empty();
            }

            List<Map<String, Object>> content = (List<Map<String, Object>>) data.get("content");
            Number totalElements = (Number) data.get("totalElements");
            long total = totalElements != null ? totalElements.longValue() : 0L;

            if (content == null || content.isEmpty()) {
                return new RuleOptionsPage(Collections.emptyList(), total);
            }

            List<RuleOptionsPage.ValueOption> items = content.stream()
                    .map(s -> new RuleOptionsPage.ValueOption(
                            (String) s.get("segmentId"),
                            (String) s.get("name"),
                            (String) s.get("name")   // segment has no i18n — use name for both en and vi
                    ))
                    .toList();

            return new RuleOptionsPage(items, total);
        } catch (FeignException e) {
            logger.error("Failed to fetch segments from segment service (page={}, size={}, search={}): {}",
                    page, size, search, e.getMessage());
            return RuleOptionsPage.empty();
        }
    }

    /**
     * Merge products + collections + SKUs from pp-product into a single grouped
     * page. Each item carries {@code metadata.type = "PRODUCT" | "COLLECTION" |
     * "SKU"} so the FE can render a grouped autocomplete. The {@code search}
     * term is forwarded to all three downstream lookups for server-side filtering.
     */
    private RuleOptionsPage fetchProducts(String search, int page, int size) {
        List<RuleOptionsPage.ValueOption> merged = new ArrayList<>();
        long total = 0L;

        // Products group — id from productId, label from productName
        try {
            Map<String, Object> raw = productFeignClient.listProducts(search, null, true, page, size);
            ProductSourcePage products = extractPage(raw, "productId", "productName", DATA_SOURCE_PRODUCT);
            merged.addAll(products.items);
            total += products.total;
        } catch (FeignException e) {
            logger.error("Failed to fetch products from product service: {}", e.getMessage());
        }

        // Collections group — id from id, label from name
        try {
            Map<String, Object> raw = productFeignClient.listCollections("ACTIVE", search, page, size);
            ProductSourcePage collections = extractPage(raw, "id", "name", "COLLECTION");
            merged.addAll(collections.items);
            total += collections.total;
        } catch (FeignException e) {
            logger.error("Failed to fetch collections from product service: {}", e.getMessage());
        }

        // SKUs group — id from id, label from name
        try {
            Map<String, Object> raw = productFeignClient.listSkus(search, null, page, size);
            ProductSourcePage skus = extractPage(raw, "id", "name", "SKU");
            merged.addAll(skus.items);
            total += skus.total;
        } catch (FeignException e) {
            logger.error("Failed to fetch SKUs from product service: {}", e.getMessage());
        }

        return new RuleOptionsPage(merged, total);
    }

    @SuppressWarnings("unchecked")
    private ProductSourcePage extractPage(Map<String, Object> raw, String idField,
                                          String nameField, String typeTag) {
        Map<String, Object> data = (Map<String, Object>) raw.get("data");
        if (data == null) return new ProductSourcePage(List.of(), 0L);

        List<Map<String, Object>> content = (List<Map<String, Object>>) data.get("content");
        Number totalElements = (Number) data.get("totalElements");
        long total = totalElements != null ? totalElements.longValue() : 0L;
        if (content == null || content.isEmpty()) return new ProductSourcePage(List.of(), total);

        List<RuleOptionsPage.ValueOption> items = content.stream()
                .map(row -> {
                    String id = stringOrEmpty(row.get(idField));
                    String name = stringOrEmpty(row.get(nameField));
                    return new RuleOptionsPage.ValueOption(id, name, name, Map.of("type", typeTag));
                })
                .toList();
        return new ProductSourcePage(items, total);
    }

    private String stringOrEmpty(Object o) {
        return o == null ? "" : o.toString();
    }

    private record ProductSourcePage(List<RuleOptionsPage.ValueOption> items, long total) {}
}
