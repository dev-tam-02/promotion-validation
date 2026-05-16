package vn.viettel.vds.promotion.validation.adapter.out.external;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.viettel.vds.promotion.validation.application.port.out.RuleOptionsPage;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SegmentLookupAdapter.
 *
 * Covers:
 *  - SEGMENT happy path: Feign returns envelope with items → correct RuleOptionsPage
 *  - Unknown dataSourceType (e.g. LOYALTY_TIER) → empty page, no Feign call
 *  - Null dataSourceType → empty page
 *  - FeignException from Feign client → graceful empty page (no re-throw)
 *  - Null data envelope in response → empty page
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SegmentLookupAdapter unit tests")
class SegmentLookupAdapterTest {

    @Mock
    private SegmentServiceFeignClient segmentFeignClient;

    private SegmentLookupAdapter sut;

    private static final String TENANT_ID = "tenant-001";
    private static final String SEGMENT_ENDPOINT = "/promotion/promotion-segment/api/segments";

    @BeforeEach
    void setUp() {
        sut = new SegmentLookupAdapter(segmentFeignClient);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Build a raw Feign response envelope matching the shape that SegmentLookupAdapter unwraps:
     * { "data": { "content": [...], "totalElements": N } }
     */
    private static Map<String, Object> envelope(List<Map<String, Object>> content, int totalElements) {
        return Map.of(
                "data", Map.of(
                        "content", content,
                        "totalElements", totalElements
                )
        );
    }

    private static Map<String, Object> segmentEntry(String segmentId, String name) {
        return Map.of(
                "segmentId", segmentId,
                "name", name,
                "code", "CODE_" + segmentId.toUpperCase(),
                "version", 1
        );
    }

    /**
     * Construct a real FeignException.ServiceUnavailable (a concrete subclass of FeignException)
     * so Mockito doesn't need to mock a checked exception hierarchy.
     */
    private static FeignException feignException(String message) {
        Request request = Request.create(
                Request.HttpMethod.GET,
                "/promotion/promotion-segment/api/segments",
                Collections.emptyMap(),
                null,
                new RequestTemplate()
        );
        return new FeignException.ServiceUnavailable(message, request, null, Collections.emptyMap());
    }

    // =========================================================================
    // SEGMENT — happy path
    // =========================================================================

    @Nested
    @DisplayName("SEGMENT — happy path")
    class SegmentHappyPath {

        @Test
        @DisplayName("Should return page with 2 items when Feign returns 2 segments")
        void shouldReturnTwoItems_whenFeignReturnsTwoSegments() {
            List<Map<String, Object>> content = List.of(
                    segmentEntry("seg-001", "VIP Customers"),
                    segmentEntry("seg-002", "Silver Members")
            );
            when(segmentFeignClient.searchSegments(0, 10, "vip", null))
                    .thenReturn(envelope(content, 2));

            RuleOptionsPage page = sut.lookup("SEGMENT", SEGMENT_ENDPOINT, "vip", 0, 10, TENANT_ID);

            assertThat(page).isNotNull();
            assertThat(page.items()).hasSize(2);
            assertThat(page.totalElements()).isEqualTo(2L);
        }

        @Test
        @DisplayName("Should map segmentId → value correctly")
        void shouldMapSegmentId_toValue() {
            List<Map<String, Object>> content = List.of(
                    segmentEntry("seg-001", "VIP Customers")
            );
            when(segmentFeignClient.searchSegments(0, 10, null, null))
                    .thenReturn(envelope(content, 1));

            RuleOptionsPage page = sut.lookup("SEGMENT", SEGMENT_ENDPOINT, null, 0, 10, TENANT_ID);

            assertThat(page.items().get(0).value()).isEqualTo("seg-001");
        }

        @Test
        @DisplayName("Should map name → labelEn AND labelVi (same value, no i18n)")
        void shouldMapName_toBothLabelEnAndLabelVi() {
            List<Map<String, Object>> content = List.of(
                    segmentEntry("seg-001", "VIP Customers")
            );
            when(segmentFeignClient.searchSegments(0, 10, null, null))
                    .thenReturn(envelope(content, 1));

            RuleOptionsPage page = sut.lookup("SEGMENT", SEGMENT_ENDPOINT, null, 0, 10, TENANT_ID);

            RuleOptionsPage.ValueOption item = page.items().get(0);
            assertThat(item.labelEn()).isEqualTo("VIP Customers");
            assertThat(item.labelVi()).isEqualTo("VIP Customers");   // same — no i18n yet
        }

        @Test
        @DisplayName("Should pass page and size parameters to Feign client")
        void shouldPassPageAndSize_toFeignClient() {
            when(segmentFeignClient.searchSegments(2, 5, "gold", null))
                    .thenReturn(envelope(List.of(), 0));

            sut.lookup("SEGMENT", SEGMENT_ENDPOINT, "gold", 2, 5, TENANT_ID);

            verify(segmentFeignClient).searchSegments(2, 5, "gold", null);
        }

        @Test
        @DisplayName("Should pass null search as name param when no filter")
        void shouldPassNullSearch_whenNoFilter() {
            when(segmentFeignClient.searchSegments(0, 20, null, null))
                    .thenReturn(envelope(List.of(), 0));

            sut.lookup("SEGMENT", SEGMENT_ENDPOINT, null, 0, 20, TENANT_ID);

            verify(segmentFeignClient).searchSegments(0, 20, null, null);
        }

        @Test
        @DisplayName("Should return correct totalElements from envelope")
        void shouldReturnCorrectTotalElements() {
            List<Map<String, Object>> content = List.of(
                    segmentEntry("seg-001", "VIP"),
                    segmentEntry("seg-002", "Gold")
            );
            when(segmentFeignClient.searchSegments(0, 2, null, null))
                    .thenReturn(envelope(content, 100));  // total=100, but page content has 2

            RuleOptionsPage page = sut.lookup("SEGMENT", SEGMENT_ENDPOINT, null, 0, 2, TENANT_ID);

            assertThat(page.items()).hasSize(2);
            assertThat(page.totalElements()).isEqualTo(100L);
        }

        @Test
        @DisplayName("Should return empty page with total=0 when content list is empty")
        void shouldReturnEmptyItems_whenContentIsEmpty() {
            when(segmentFeignClient.searchSegments(0, 10, null, null))
                    .thenReturn(envelope(List.of(), 0));

            RuleOptionsPage page = sut.lookup("SEGMENT", SEGMENT_ENDPOINT, null, 0, 10, TENANT_ID);

            assertThat(page.items()).isEmpty();
            assertThat(page.totalElements()).isZero();
        }
    }

    // =========================================================================
    // LOYALTY_TIER — not yet implemented
    // =========================================================================

    @Nested
    @DisplayName("Unknown / not-yet-implemented dataSourceType")
    class UnknownDataSourceType {

        @Test
        @DisplayName("Should return empty page and not call Feign for LOYALTY_TIER")
        void shouldReturnEmpty_andNotCallFeign_forLoyaltyTier() {
            RuleOptionsPage page = sut.lookup(
                    "LOYALTY_TIER", "/loyalty/api/tiers", null, 0, 10, TENANT_ID);

            assertThat(page.items()).isEmpty();
            assertThat(page.totalElements()).isZero();
            verifyNoInteractions(segmentFeignClient);
        }

        @Test
        @DisplayName("Should return empty page and not call Feign for completely unknown type")
        void shouldReturnEmpty_forCompletelyUnknownType() {
            RuleOptionsPage page = sut.lookup(
                    "PRODUCT_CATEGORY", "/some/endpoint", null, 0, 10, TENANT_ID);

            assertThat(page.items()).isEmpty();
            verifyNoInteractions(segmentFeignClient);
        }
    }

    // =========================================================================
    // Null dataSourceType
    // =========================================================================

    @Nested
    @DisplayName("Null dataSourceType guard")
    class NullDataSourceType {

        @Test
        @DisplayName("Should return empty page and not call Feign when dataSourceType is null")
        void shouldReturnEmpty_whenDataSourceTypeIsNull() {
            RuleOptionsPage page = sut.lookup(null, SEGMENT_ENDPOINT, null, 0, 10, TENANT_ID);

            assertThat(page.items()).isEmpty();
            assertThat(page.totalElements()).isZero();
            verifyNoInteractions(segmentFeignClient);
        }
    }

    // =========================================================================
    // FeignException → graceful empty page
    // =========================================================================

    @Nested
    @DisplayName("FeignException → graceful degradation")
    class FeignExceptionHandling {

        @Test
        @DisplayName("Should return empty page when FeignException is thrown (segment service down)")
        void shouldReturnEmptyPage_whenFeignExceptionThrown() {
            when(segmentFeignClient.searchSegments(anyInt(), anyInt(), any(), any()))
                    .thenThrow(feignException("Service unavailable"));

            RuleOptionsPage page = sut.lookup("SEGMENT", SEGMENT_ENDPOINT, null, 0, 10, TENANT_ID);

            assertThat(page.items()).isEmpty();
            assertThat(page.totalElements()).isZero();
        }

        @Test
        @DisplayName("Should not propagate FeignException to caller")
        void shouldNotPropagateException_toCallerOnFeignError() {
            when(segmentFeignClient.searchSegments(anyInt(), anyInt(), any(), any()))
                    .thenThrow(feignException("503 Service Unavailable"));

            // Must NOT throw
            RuleOptionsPage page = sut.lookup("SEGMENT", SEGMENT_ENDPOINT, "vip", 0, 10, TENANT_ID);
            assertThat(page).isNotNull();
        }
    }

    // =========================================================================
    // Null data envelope guard
    // =========================================================================

    @Nested
    @DisplayName("Defensive: null data envelope in Feign response")
    class NullDataEnvelope {

        @Test
        @DisplayName("Should return empty page when Feign returns map without 'data' key")
        void shouldReturnEmpty_whenDataKeyMissingFromEnvelope() {
            // Response has no "data" key — adapter should guard and return empty
            when(segmentFeignClient.searchSegments(anyInt(), anyInt(), any(), any()))
                    .thenReturn(Map.of("status", 200));   // no "data" key

            RuleOptionsPage page = sut.lookup("SEGMENT", SEGMENT_ENDPOINT, null, 0, 10, TENANT_ID);

            assertThat(page.items()).isEmpty();
            assertThat(page.totalElements()).isZero();
        }
    }
}
