package vn.viettel.vds.promotion.validation.adapter.out.external;

import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.application.port.out.RuleOptionsLookupPort;
import vn.viettel.vds.promotion.validation.application.port.out.RuleOptionsPage;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Adapter that routes rule-option lookups to the appropriate external service
 * based on {@code dataSourceType}.
 *
 * <p>Routing strategy:
 * <ul>
 *   <li>{@code "SEGMENT"} → pp-segment via {@link SegmentServiceFeignClient}</li>
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

    private final SegmentServiceFeignClient segmentFeignClient;

    public SegmentLookupAdapter(SegmentServiceFeignClient segmentFeignClient) {
        this.segmentFeignClient = segmentFeignClient;
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
            default -> {
                logger.warn("data_source_type '{}' not yet implemented, returning empty page", dataSourceType);
                yield RuleOptionsPage.empty();
            }
        };
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
}
