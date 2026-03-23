package vn.viettel.vds.promotion.validation.application.fact.resolver;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import java.time.Instant;
import vn.viettel.vds.promotion.validation.domain.exception.FactResolutionException;
import vn.viettel.vds.promotion.validation.domain.fact.FactRequest;
import vn.viettel.vds.promotion.validation.domain.fact.SegmentsFact;

import java.util.*;

/**
 * Fact resolver for customer segment information.
 * <p>
 * Resolves customer segment data with:
 * - Circuit breaker protection
 * - Automatic retry with exponential backoff
 * - Redis caching with 30-minute TTL
 * - Fallback to empty segments on failure
 */
@Component
public class SegmentsFactResolver extends AbstractFactResolver<SegmentsFact> {

    private static final String CACHE_NAME = "segments-fact";
    private static final String CONTEXT_NAME = "SegmentsFact";

    @Autowired
    public SegmentsFactResolver(
            CircuitBreaker segmentsCircuitBreaker,
            Retry segmentsRetry) {
        super(segmentsCircuitBreaker, segmentsRetry);
    }

    @Override
    public String getContextName() {
        return CONTEXT_NAME;
    }

    @Override
    @Cacheable(value = CACHE_NAME, key = "#request.customerId()", unless = "#result == null")
    public SegmentsFact resolveFromIds(FactRequest request) {
        String customerId = request.customerId();

        if (customerId == null || customerId.isEmpty()) {
            if (log.isWarnEnabled()) {
                log.warn("Customer ID is null or empty, returning empty segments");
            }
            return getPartialResult(request);
        }

        try {
            if (log.isDebugEnabled()) {
                log.debug("Resolving segments for customer: {}", customerId);
            }

            // Segment service call with circuit breaker and retry protection
            // Integration point for external segment service
            Set<String> segmentIds = fetchSegmentsFromService(customerId);

            // Convert Set<String> to List<String> for segmentIds
            List<String> segmentIdsList = new ArrayList<>(segmentIds);

            return SegmentsFact.builder()
                    .segmentIds(segmentIdsList)
                    .segments(null) // Segment details can be populated later if needed
                    .evaluatedAt(Instant.now())
                    .evaluationContext("SegmentsFact resolution for customer: " + customerId)
                    .build();

        } catch (Exception e) {
            throw new FactResolutionException("Failed to resolve segments for customer: " + customerId + " - " + e.getMessage(), e);
        }
    }

    @Override
    public boolean supportsEmbeddedPayload() {
        return true;
    }

    @Override
    public SegmentsFact resolveFromEmbeddedPayload(FactRequest request, Map<String, Object> embeddedData) {
        try {
            String customerId = request.customerId();

            // Extract segments from embedded payload
            Object segmentsObj = embeddedData.get("segments");
            List<String> segmentIdsList = new ArrayList<>();

            if (segmentsObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> segmentList = (List<String>) segmentsObj;
                segmentIdsList.addAll(segmentList);
            } else if (segmentsObj instanceof Set) {
                @SuppressWarnings("unchecked")
                Set<String> segmentSet = (Set<String>) segmentsObj;
                segmentIdsList.addAll(segmentSet);
            }

            if (log.isDebugEnabled()) {
                log.debug("Resolved segments from embedded payload for customer {}: {}", customerId, segmentIdsList);
            }

            return SegmentsFact.builder()
                    .segmentIds(segmentIdsList)
                    .segments(null) // Segment details can be populated later if needed
                    .evaluatedAt(Instant.now())
                    .evaluationContext("SegmentsFact resolution from embedded payload for customer: " + customerId)
                    .build();

        } catch (Exception e) {
            throw new FactResolutionException("Failed to resolve segments from embedded payload: " + e.getMessage(), e);
        }
    }

    @Override
    protected SegmentsFact getPartialResult(FactRequest request) {
        if (log.isWarnEnabled()) {
            log.warn("Returning fallback empty segments for customer: {}", request.customerId());
        }

        return SegmentsFact.builder()
                .segmentIds(new ArrayList<>())
                .segments(null)
                .evaluatedAt(Instant.now())
                .evaluationContext("Fallback empty segments for customer: " + request.customerId())
                .build();
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public int getTimeoutMs() {
        return 3000; // 3 seconds timeout
    }

    @Override
    public int getPriority() {
        return 80; // Higher priority for segment resolution
    }

    /**
     * Fetch segments from external segment service.
     * This method is protected by circuit breaker and retry mechanisms.
     *
     * @param customerId the customer ID
     * @return set of segment IDs
     */
    private Set<String> fetchSegmentsFromService(String customerId) {

        // Mock implementation
        if (log.isDebugEnabled()) {
            log.debug("Fetching segments for customer {} (mock implementation)", customerId);
        }

        Set<String> segments = new HashSet<>();

        // Simulate segment assignment based on customer ID patterns
        if (customerId.contains("VIP")) {
            segments.add("VIP");
            segments.add("PREMIUM");
        } else if (customerId.contains("NEW")) {
            segments.add("NEW_CUSTOMER");
        } else {
            segments.add("STANDARD");
        }

        return segments;
    }
}
