package vn.viettel.vds.promotion.validation.adapter.out.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * Feign client for pp-segment service.
 * Used to fetch paginated segment list for rule builder option lookup.
 *
 * <p>Full path is inlined in {@code @GetMapping} because context-path
 * ({@code /promotion/promotion-segment}) is not exposed via a {@code path=} attribute.
 * Base URL ({@code host:port}) comes from {@code external.services.segment.url}.
 */
@FeignClient(
        name = "segment-service",
        url = "${external.services.segment.url}",
        configuration = ExternalServiceFeignConfig.class,
        fallback = SegmentServiceFeignClientFallback.class
)
public interface SegmentServiceFeignClient {

    /**
     * GET /promotion/promotion-segment/api/segments
     * Response is wrapped in {@code ResponseTemplate<Page<SearchSegmentResponse>>}.
     * The caller must unwrap {@code raw.get("data")} manually.
     */
    @GetMapping(
            value = "/promotion/promotion-segment/api/segments",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    Map<String, Object> searchSegments(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "code", required = false) String code
    );

    /**
     * GET /promotion/promotion-segment/api/segments/{segmentId}
     * Resolve a single segment by id. Response {@code data} carries
     * {@code segmentId} + {@code name}. Used by the by-id value-resolution path.
     */
    @GetMapping(
            value = "/promotion/promotion-segment/api/segments/{segmentId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    Map<String, Object> getSegmentById(@PathVariable("segmentId") String segmentId);
}
