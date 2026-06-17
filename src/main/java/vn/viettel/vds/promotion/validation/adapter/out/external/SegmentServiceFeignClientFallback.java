package vn.viettel.vds.promotion.validation.adapter.out.external;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

/**
 * Fallback implementation for SegmentServiceFeignClient.
 * Returns an empty-content envelope when pp-segment is unavailable,
 * allowing graceful degradation in the rule builder UI.
 */
@Component
public class SegmentServiceFeignClientFallback implements SegmentServiceFeignClient {

    private static final Logger logger = LoggerFactory.getLogger(SegmentServiceFeignClientFallback.class);

    @Override
    public Map<String, Object> searchSegments(Integer page, Integer size, String name, String code) {
        logger.warn("Segment service unavailable, returning empty list (page={}, size={}, name={}, code={})",
                page, size, name, code);
        // Return envelope-shaped response with empty content for clean unwrapping in adapter
        return Map.of(
                "data", Map.of(
                        "content", Collections.emptyList(),
                        "totalElements", 0
                )
        );
    }

    @Override
    public Map<String, Object> getSegmentById(String segmentId) {
        logger.warn("Segment service unavailable (getSegmentById id={}); returning empty data", segmentId);
        return Map.of("data", Collections.emptyMap());
    }
}
