package vn.viettel.vds.promotion.validation.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.viettel.vds.promotion.validation.adapter.in.web.dto.rulebuilder.RuleOptionsResponse;
import vn.viettel.vds.promotion.validation.application.port.out.RuleOptionsLookupPort;
import vn.viettel.vds.promotion.validation.application.port.out.RuleOptionsPage;
import vn.viettel.vds.promotion.validation.domain.model.OperatorCategory;
import vn.viettel.vds.promotion.validation.domain.model.OperatorOption;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RuleBuilderService.getRuleOptions() — covers 3 routing branches:
 *  1. STATIC (dataSourceType null or "STATIC") — in-memory filter + pagination
 *  2. SEGMENT (dataSourceType "SEGMENT") — delegates to RuleOptionsLookupPort
 *  3. NOT_IMPLEMENTED (e.g. "LOYALTY_TIER") — port returns empty page
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RuleBuilderService — getRuleOptions() branch tests")
@SuppressWarnings("deprecation") // Test fixtures intentionally populate the legacy label BC field
class RuleBuilderServiceTest {

    @Mock
    private OperatorConfigService operatorConfigService;

    @Mock
    private RuleOptionsLookupPort ruleOptionsLookupPort;

    @Mock
    private vn.viettel.vds.promotion.validation.adapter.out.external.MetadataServiceFeignClient metadataServiceFeignClient;

    private RuleBuilderService sut;

    private static final String TENANT_ID = "tenant-001";

    @BeforeEach
    void setUp() {
        sut = new RuleBuilderService(operatorConfigService, ruleOptionsLookupPort, metadataServiceFeignClient);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static OperatorCategory categoryWith(OperatorOption option) {
        return OperatorCategory.builder()
                .id("cat-1")
                .code("audience")
                .name("Audience")
                .active(true)
                .options(List.of(option))
                .build();
    }

    private static OperatorOption.ValueOption vo(String value, String label) {
        // Also populate i18n fields so getStaticOptions can use getLabelEn()/getLabelVi()
        return OperatorOption.ValueOption.builder().value(value).label(label).labelEn(label).labelVi(label).build();
    }

    private static OperatorOption.ValueOption voI18n(String value, String labelEn, String labelVi) {
        return OperatorOption.ValueOption.builder()
                .value(value).label(labelEn).labelEn(labelEn).labelVi(labelVi).build();
    }

    private static OperatorOption staticOption(String code) {
        return OperatorOption.builder()
                .id("opt-" + code)
                .categoryId("cat-1")
                .code(code)
                .name("Acquisition Channel")
                .active(true)
                .dataSourceType(null)   // null → STATIC branch
                .valueOptions(List.of(
                        vo("paid", "Paid"),
                        vo("organic", "Organic"),
                        vo("referral", "Referral"),
                        vo("social", "Social"),
                        vo("email", "Email")
                ))
                .build();
    }

    private static OperatorOption staticOptionWithType(String code) {
        return OperatorOption.builder()
                .id("opt-" + code)
                .categoryId("cat-1")
                .code(code)
                .name("Acquisition Channel")
                .active(true)
                .dataSourceType("STATIC")   // explicit "STATIC" → same branch
                .valueOptions(List.of(
                        vo("paid", "Paid"),
                        vo("organic", "Organic")
                ))
                .build();
    }

    private static OperatorOption segmentOption(String code) {
        return OperatorOption.builder()
                .id("opt-" + code)
                .categoryId("cat-1")
                .code(code)
                .name("Customer Segment")
                .active(true)
                .dataSourceType("SEGMENT")
                .dataSourceEndpoint("/promotion/promotion-segment/api/segments")
                .build();
    }

    private static OperatorOption loyaltyTierOption(String code) {
        return OperatorOption.builder()
                .id("opt-" + code)
                .categoryId("cat-1")
                .code(code)
                .name("Loyalty Tier")
                .active(true)
                .dataSourceType("LOYALTY_TIER")
                .dataSourceEndpoint("/promotion/promotion-loyalty/api/tiers")
                .build();
    }

    // =========================================================================
    // Branch 1 — STATIC (dataSourceType = null)
    // =========================================================================

    @Nested
    @DisplayName("Branch STATIC — dataSourceType null")
    class StaticBranchNullType {

        @Test
        @DisplayName("Should return all value options when no search filter")
        void shouldReturnAllOptions_whenNoSearch() {
            String ruleId = "acquisition_channel";
            when(operatorConfigService.getAllCategoriesWithOptions(TENANT_ID))
                    .thenReturn(List.of(categoryWith(staticOption(ruleId))));

            RuleOptionsResponse response = sut.getRuleOptions(ruleId, null, 0, 10, TENANT_ID);

            assertThat(response.ruleId()).isEqualTo(ruleId);
            assertThat(response.options()).hasSize(5);
            assertThat(response.totalElements()).isEqualTo(5L);
            assertThat(response.page()).isZero();
            assertThat(response.size()).isEqualTo(10);
            verifyNoInteractions(ruleOptionsLookupPort);
        }

        @Test
        @DisplayName("Should filter options case-insensitively when search is provided")
        void shouldFilterOptions_whenSearchProvided() {
            String ruleId = "acquisition_channel";
            when(operatorConfigService.getAllCategoriesWithOptions(TENANT_ID))
                    .thenReturn(List.of(categoryWith(staticOption(ruleId))));

            RuleOptionsResponse response = sut.getRuleOptions(ruleId, "orga", 0, 10, TENANT_ID);

            assertThat(response.options()).hasSize(1);
            assertThat(response.options().get(0).value()).isEqualTo("organic");
            verifyNoInteractions(ruleOptionsLookupPort);
        }

        @Test
        @DisplayName("Should paginate in-memory options correctly")
        void shouldPaginateOptions_correctlyInMemory() {
            String ruleId = "acquisition_channel";
            when(operatorConfigService.getAllCategoriesWithOptions(TENANT_ID))
                    .thenReturn(List.of(categoryWith(staticOption(ruleId))));

            // Page 0, size 2 of 5 options
            RuleOptionsResponse page0 = sut.getRuleOptions(ruleId, null, 0, 2, TENANT_ID);
            assertThat(page0.options()).hasSize(2);
            assertThat(page0.totalElements()).isEqualTo(5L);
            assertThat(page0.options().get(0).value()).isEqualTo("paid");
            assertThat(page0.options().get(1).value()).isEqualTo("organic");
            verifyNoInteractions(ruleOptionsLookupPort);
        }

        @Test
        @DisplayName("Should NOT delegate to RuleOptionsLookupPort for STATIC type")
        void shouldNotCallPort_forNullDataSourceType() {
            String ruleId = "acquisition_channel";
            when(operatorConfigService.getAllCategoriesWithOptions(TENANT_ID))
                    .thenReturn(List.of(categoryWith(staticOption(ruleId))));

            sut.getRuleOptions(ruleId, null, 0, 20, TENANT_ID);

            verifyNoInteractions(ruleOptionsLookupPort);
        }

        @Test
        @DisplayName("Should return distinct labelEn and labelVi when option has i18n labels")
        void shouldReturnDistinctLabelEnAndLabelVi_whenI18nValuesSet() {
            String ruleId = "acquisition_channel";
            OperatorOption i18nOption = OperatorOption.builder()
                    .id("opt-" + ruleId)
                    .categoryId("cat-1")
                    .code(ruleId)
                    .name("Acquisition Channel")
                    .active(true)
                    .dataSourceType(null)
                    .valueOptions(List.of(
                            voI18n("paid", "Paid", "Trả phí"),
                            voI18n("organic", "Organic", "Tự nhiên"),
                            voI18n("referral", "Referral", "Giới thiệu")
                    ))
                    .build();
            when(operatorConfigService.getAllCategoriesWithOptions(TENANT_ID))
                    .thenReturn(List.of(categoryWith(i18nOption)));

            RuleOptionsResponse response = sut.getRuleOptions(ruleId, null, 0, 10, TENANT_ID);

            assertThat(response.options()).hasSize(3);
            // First option: i18n labels differ
            assertThat(response.options().get(0).value()).isEqualTo("paid");
            assertThat(response.options().get(0).label().en()).isEqualTo("Paid");
            assertThat(response.options().get(0).label().vi()).isEqualTo("Trả phí");
            // Second option
            assertThat(response.options().get(1).label().en()).isEqualTo("Organic");
            assertThat(response.options().get(1).label().vi()).isEqualTo("Tự nhiên");
            verifyNoInteractions(ruleOptionsLookupPort);
        }

        @Test
        @DisplayName("Should search against labelEn when i18n labels set")
        void shouldSearchAgainstLabelEn_whenI18nLabelsSet() {
            String ruleId = "acquisition_channel";
            OperatorOption i18nOption = OperatorOption.builder()
                    .id("opt-" + ruleId)
                    .categoryId("cat-1")
                    .code(ruleId)
                    .name("Acquisition Channel")
                    .active(true)
                    .dataSourceType(null)
                    .valueOptions(List.of(
                            voI18n("paid", "Paid", "Trả phí"),
                            voI18n("organic", "Organic", "Tự nhiên")
                    ))
                    .build();
            when(operatorConfigService.getAllCategoriesWithOptions(TENANT_ID))
                    .thenReturn(List.of(categoryWith(i18nOption)));

            // Search by English label "orga" → matches "Organic"
            RuleOptionsResponse response = sut.getRuleOptions(ruleId, "orga", 0, 10, TENANT_ID);

            assertThat(response.options()).hasSize(1);
            assertThat(response.options().get(0).value()).isEqualTo("organic");
            verifyNoInteractions(ruleOptionsLookupPort);
        }
    }

    @Nested
    @DisplayName("Branch STATIC — explicit dataSourceType='STATIC'")
    class StaticBranchExplicitType {

        @Test
        @DisplayName("Should return value options when dataSourceType is 'STATIC' string")
        void shouldReturnOptions_whenDataSourceTypeIsStaticString() {
            String ruleId = "some_static_rule";
            when(operatorConfigService.getAllCategoriesWithOptions(TENANT_ID))
                    .thenReturn(List.of(categoryWith(staticOptionWithType(ruleId))));

            RuleOptionsResponse response = sut.getRuleOptions(ruleId, null, 0, 10, TENANT_ID);

            assertThat(response.options()).hasSize(2);
            assertThat(response.options().get(0).value()).isEqualTo("paid");
            verifyNoInteractions(ruleOptionsLookupPort);
        }
    }

    // =========================================================================
    // Branch 2 — SEGMENT (delegates to RuleOptionsLookupPort)
    // =========================================================================

    @Nested
    @DisplayName("Branch SEGMENT — delegates to RuleOptionsLookupPort")
    class SegmentBranch {

        @Test
        @DisplayName("Should delegate to port and map items correctly when dataSourceType=SEGMENT")
        void shouldDelegateToPortAndMapItems_whenSegment() {
            String ruleId = "customer_segment";
            String endpoint = "/promotion/promotion-segment/api/segments";

            when(operatorConfigService.getAllCategoriesWithOptions(TENANT_ID))
                    .thenReturn(List.of(categoryWith(segmentOption(ruleId))));

            List<RuleOptionsPage.ValueOption> items = List.of(
                    new RuleOptionsPage.ValueOption("seg-001", "VIP Customers", "VIP Customers"),
                    new RuleOptionsPage.ValueOption("seg-002", "Silver Members", "Silver Members"),
                    new RuleOptionsPage.ValueOption("seg-003", "New Users", "New Users")
            );
            RuleOptionsPage portPage = new RuleOptionsPage(items, 3L);
            when(ruleOptionsLookupPort.lookup("SEGMENT", endpoint, "vip", 0, 10, TENANT_ID))
                    .thenReturn(portPage);

            RuleOptionsResponse response = sut.getRuleOptions(ruleId, "vip", 0, 10, TENANT_ID);

            assertThat(response.ruleId()).isEqualTo(ruleId);
            assertThat(response.options()).hasSize(3);
            assertThat(response.totalElements()).isEqualTo(3L);
            assertThat(response.page()).isZero();
            assertThat(response.size()).isEqualTo(10);

            // Verify value mapping
            assertThat(response.options().get(0).value()).isEqualTo("seg-001");
            assertThat(response.options().get(0).label().en()).isEqualTo("VIP Customers");
            assertThat(response.options().get(0).label().vi()).isEqualTo("VIP Customers");

            assertThat(response.options().get(1).value()).isEqualTo("seg-002");
            assertThat(response.options().get(2).value()).isEqualTo("seg-003");

            verify(ruleOptionsLookupPort).lookup("SEGMENT", endpoint, "vip", 0, 10, TENANT_ID);
        }

        @Test
        @DisplayName("Should pass correct page/size to port")
        void shouldPassCorrectPaginationToPort() {
            String ruleId = "customer_segment";
            String endpoint = "/promotion/promotion-segment/api/segments";

            when(operatorConfigService.getAllCategoriesWithOptions(TENANT_ID))
                    .thenReturn(List.of(categoryWith(segmentOption(ruleId))));
            when(ruleOptionsLookupPort.lookup(anyString(), anyString(), any(), anyInt(), anyInt(), anyString()))
                    .thenReturn(RuleOptionsPage.empty());

            sut.getRuleOptions(ruleId, null, 2, 5, TENANT_ID);

            verify(ruleOptionsLookupPort).lookup("SEGMENT", endpoint, null, 2, 5, TENANT_ID);
        }

        @Test
        @DisplayName("Should default page=0 and size=20 when null passed")
        void shouldDefaultPageAndSize_whenNull() {
            String ruleId = "customer_segment";
            when(operatorConfigService.getAllCategoriesWithOptions(TENANT_ID))
                    .thenReturn(List.of(categoryWith(segmentOption(ruleId))));
            when(ruleOptionsLookupPort.lookup(anyString(), anyString(), any(), anyInt(), anyInt(), anyString()))
                    .thenReturn(RuleOptionsPage.empty());

            RuleOptionsResponse response = sut.getRuleOptions(ruleId, null, null, null, TENANT_ID);

            verify(ruleOptionsLookupPort).lookup(eq("SEGMENT"), anyString(), eq(null), eq(0), eq(20), eq(TENANT_ID));
            assertThat(response.page()).isZero();
            assertThat(response.size()).isEqualTo(20);
        }
    }

    // =========================================================================
    // Branch 3 — NOT_IMPLEMENTED (port returns empty)
    // =========================================================================

    @Nested
    @DisplayName("Branch NOT_IMPLEMENTED — port returns empty page")
    class NotImplementedBranch {

        @Test
        @DisplayName("Should return empty response when port returns empty page (LOYALTY_TIER)")
        void shouldReturnEmpty_whenPortReturnsEmptyPage() {
            String ruleId = "loyalty_tier";
            String endpoint = "/promotion/promotion-loyalty/api/tiers";

            when(operatorConfigService.getAllCategoriesWithOptions(TENANT_ID))
                    .thenReturn(List.of(categoryWith(loyaltyTierOption(ruleId))));
            when(ruleOptionsLookupPort.lookup("LOYALTY_TIER", endpoint, null, 0, 20, TENANT_ID))
                    .thenReturn(RuleOptionsPage.empty());

            RuleOptionsResponse response = sut.getRuleOptions(ruleId, null, null, null, TENANT_ID);

            assertThat(response.ruleId()).isEqualTo(ruleId);
            assertThat(response.options()).isEmpty();
            assertThat(response.totalElements()).isZero();
            verify(ruleOptionsLookupPort).lookup("LOYALTY_TIER", endpoint, null, 0, 20, TENANT_ID);
        }

        @Test
        @DisplayName("Should still delegate to port even for unknown dataSourceType")
        void shouldDelegateToPort_evenForUnknownDataSourceType() {
            String ruleId = "loyalty_tier";
            when(operatorConfigService.getAllCategoriesWithOptions(TENANT_ID))
                    .thenReturn(List.of(categoryWith(loyaltyTierOption(ruleId))));
            when(ruleOptionsLookupPort.lookup(anyString(), anyString(), any(), anyInt(), anyInt(), anyString()))
                    .thenReturn(RuleOptionsPage.empty());

            sut.getRuleOptions(ruleId, null, null, null, TENANT_ID);

            verify(ruleOptionsLookupPort, times(1)).lookup(
                    eq("LOYALTY_TIER"), anyString(), any(), anyInt(), anyInt(), eq(TENANT_ID));
        }
    }

    // =========================================================================
    // Edge case — ruleId not found
    // =========================================================================

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("Should return empty paginated response when ruleId does not match any option")
        void shouldReturnEmpty_whenRuleIdNotFound() {
            when(operatorConfigService.getAllCategoriesWithOptions(TENANT_ID))
                    .thenReturn(List.of(categoryWith(staticOption("other_rule"))));

            RuleOptionsResponse response = sut.getRuleOptions("nonexistent_rule", null, 0, 10, TENANT_ID);

            assertThat(response.ruleId()).isEqualTo("nonexistent_rule");
            assertThat(response.options()).isEmpty();
            assertThat(response.totalElements()).isZero();
            verifyNoInteractions(ruleOptionsLookupPort);
        }
    }
}
