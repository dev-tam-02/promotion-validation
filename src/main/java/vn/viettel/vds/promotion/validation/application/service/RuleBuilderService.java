package vn.viettel.vds.promotion.validation.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import vn.viettel.vds.promotion.validation.adapter.in.web.dto.rulebuilder.*;
import vn.viettel.vds.promotion.validation.domain.model.MetadataSchema;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service for Rule Builder API.
 * Provides hardcoded rule categories and dynamic metadata integration.
 */
@Service
public class RuleBuilderService {

    private static final Logger logger = LoggerFactory.getLogger(RuleBuilderService.class);
    private static final String VERSION = "1.0.0";

    private final MetadataSchemaService metadataSchemaService;

    public RuleBuilderService(MetadataSchemaService metadataSchemaService) {
        this.metadataSchemaService = metadataSchemaService;
    }

    /**
     * Get all rule categories with their rules.
     * Includes static categories and dynamic metadata categories.
     */
    public RuleCategoriesResponse getAllCategories(String tenantId) {
        logger.debug("Getting all rule categories for tenant: {}", tenantId);

        List<RuleCategoryResponse> categories = new ArrayList<>();

        // Static categories
        categories.add(buildAudienceCategory());
        categories.add(buildProductsCategory());
        categories.add(buildPricesQuantitiesCategory());
        categories.add(buildBudgetConstraintsCategory());
        categories.add(buildRedemptionsCategory());

        // Dynamic metadata categories
        categories.add(buildCustomerMetadataCategory(tenantId));
        categories.add(buildOrderMetadataCategory(tenantId));

        return RuleCategoriesResponse.of(categories, VERSION);
    }

    /**
     * Get options for a specific rule.
     * This will call external services or return mock data.
     */
    public RuleOptionsResponse getRuleOptions(String ruleId, String search, Integer page, Integer size, String tenantId) {
        logger.debug("Getting options for rule: {} (search: {}, page: {}, size: {}, tenant: {})",
                ruleId, search, page, size, tenantId);

        // For now, return empty options - in production, this should call external services
        List<RuleOptionResponse> options = getMockOptionsForRule(ruleId, search);

        int totalElements = options.size();
        int pageNumber = page != null ? page : 0;
        int pageSize = size != null ? size : 20;

        // Simple pagination
        int fromIndex = Math.min(pageNumber * pageSize, totalElements);
        int toIndex = Math.min(fromIndex + pageSize, totalElements);
        List<RuleOptionResponse> pagedOptions = options.subList(fromIndex, toIndex);

        return RuleOptionsResponse.paginated(ruleId, pagedOptions, totalElements, pageNumber, pageSize);
    }

    // ========== CATEGORY BUILDERS ==========

    private RuleCategoryResponse buildAudienceCategory() {
        return RuleCategoryResponse.builder()
                .id("audience")
                .code("AUDIENCE")
                .name("Audience", "Đối tượng")
                .icon("PeopleOutline")
                .order(1)
                .rules(List.of(
                        buildCustomerSegmentRule(),
                        buildCustomerLoyaltyTierRule(),
                        buildRedemptionByCodeHolderRule()
                ))
                .build();
    }

    private RuleCategoryResponse buildProductsCategory() {
        return RuleCategoryResponse.builder()
                .id("products")
                .code("PRODUCTS")
                .name("Products", "Sản phẩm")
                .icon("ShoppingCartOutline")
                .order(2)
                .rules(List.of(
                        buildProductCategoryRule(),
                        buildProductBrandRule(),
                        buildProductSkuRule(),
                        buildProductCollectionRule(),
                        buildAnyOrderItemRule(),
                        buildEveryOrderItemRule(),
                        buildNoneOfOrderItemsRule()
                ))
                .build();
    }

    private RuleCategoryResponse buildPricesQuantitiesCategory() {
        return RuleCategoryResponse.builder()
                .id("prices_quantities")
                .code("PRICES_QUANTITIES")
                .name("Prices & Quantities", "Giá & Số lượng")
                .icon("CalculatorOutline")
                .order(3)
                .rules(List.of(
                        buildOrderTotalAmountRule(),
                        buildOrderInitialAmountRule(),
                        buildOrderItemQuantityRule(),
                        buildOrderItemPriceRule(),
                        buildOrderItemsCountRule()
                ))
                .build();
    }

    private RuleCategoryResponse buildBudgetConstraintsCategory() {
        return RuleCategoryResponse.builder()
                .id("budget_constraints")
                .code("BUDGET_CONSTRAINTS")
                .name("Budget & Constraints", "Ngân sách & Giới hạn")
                .icon("WalletOutline")
                .order(4)
                .rules(List.of(
                        buildTotalRedemptionsLimitRule(),
                        buildRedemptionsPerCustomerRule(),
                        buildDailyRedemptionsLimitRule(),
                        buildMonthlyRedemptionsLimitRule(),
                        buildTotalBudgetLimitRule(),
                        buildDailyBudgetLimitRule(),
                        buildMonthlyBudgetLimitRule(),
                        buildCustomerDailyLimitRule(),
                        buildCustomerMonthlyLimitRule(),
                        buildCustomerTotalLimitRule()
                ))
                .build();
    }

    private RuleCategoryResponse buildRedemptionsCategory() {
        return RuleCategoryResponse.builder()
                .id("redemptions")
                .code("REDEMPTIONS")
                .name("Redemptions", "Quy tắc sử dụng")
                .icon("TicketOutline")
                .order(5)
                .rules(List.of(
                        buildRedeemingUserTypeRule(),
                        buildRedemptionApiKeyRule(),
                        buildCodeHolderOnlyRule()
                ))
                .build();
    }

    private RuleCategoryResponse buildCustomerMetadataCategory(String tenantId) {
        List<MetadataSchema> customerSchemas = metadataSchemaService.getSchemaFields(tenantId, "customer");

        List<RuleItemResponse> rules = customerSchemas.stream()
                .map(this::buildMetadataRule)
                .toList();

        return RuleCategoryResponse.builder()
                .id("customer_metadata")
                .code("CUSTOMER_METADATA")
                .name("Customer Metadata", "Thuộc tính khách hàng")
                .icon("PersonCircleOutline")
                .order(6)
                .rules(rules)
                .build();
    }

    private RuleCategoryResponse buildOrderMetadataCategory(String tenantId) {
        List<MetadataSchema> orderSchemas = metadataSchemaService.getSchemaFields(tenantId, "order");

        List<RuleItemResponse> rules = orderSchemas.stream()
                .map(this::buildMetadataRule)
                .toList();

        return RuleCategoryResponse.builder()
                .id("order_metadata")
                .code("ORDER_METADATA")
                .name("Order Metadata", "Thuộc tính đơn hàng")
                .icon("ReceiptOutline")
                .order(7)
                .rules(rules)
                .build();
    }

    // ========== RULE BUILDERS - AUDIENCE ==========

    private RuleItemResponse buildCustomerSegmentRule() {
        return RuleItemResponse.builder()
                .id("customer_segment")
                .code("CUSTOMER_SEGMENT")
                .name("Customer segment", "Tập khách hàng")
                .description("Target specific customer segments", "Nhắm mục tiêu các tập khách hàng cụ thể")
                .type("SEGMENT")
                .inputConfig(RuleInputConfigResponse.builder()
                        .dataSourceType("SEGMENT")
                        .dataSourceEndpoint("/segment/api/v1/segments")
                        .label("Select segments", "Chọn tập khách hàng")
                        .placeholder("Choose customer segments...", "Chọn tập khách hàng...")
                        .multiple(true)
                        .searchable(true)
                        .build())
                .operators(buildSegmentOperators())
                .build();
    }

    private RuleItemResponse buildCustomerLoyaltyTierRule() {
        return RuleItemResponse.builder()
                .id("customer_loyalty_tier")
                .code("CUSTOMER_LOYALTY_TIER")
                .name("Customer loyalty tier", "Hạng thành viên")
                .description("Target customers by loyalty tier", "Nhắm mục tiêu khách hàng theo hạng thành viên")
                .type("LOYALTY_TIER")
                .inputConfig(RuleInputConfigResponse.builder()
                        .dataSourceType("LOYALTY_TIER")
                        .dataSourceEndpoint("/customer/api/v1/loyalty-tiers")
                        .label("Select loyalty tier", "Chọn hạng thành viên")
                        .placeholder("Choose loyalty tier...", "Chọn hạng thành viên...")
                        .multiple(true)
                        .searchable(true)
                        .build())
                .operators(buildSegmentOperators())
                .build();
    }

    private RuleItemResponse buildRedemptionByCodeHolderRule() {
        return RuleItemResponse.builder()
                .id("redemption_by_code_holder")
                .code("REDEMPTION_BY_CODE_HOLDER")
                .name("Redemption by code holder", "Chỉ người giữ mã")
                .description("Only allow redemption by code holder", "Chỉ cho phép người giữ mã sử dụng")
                .type("BOOLEAN")
                .inputConfig(null)
                .operators(buildBooleanOperators())
                .build();
    }

    // ========== RULE BUILDERS - PRODUCTS ==========

    private RuleItemResponse buildProductCategoryRule() {
        return RuleItemResponse.builder()
                .id("product_category")
                .code("PRODUCT_CATEGORY")
                .name("Product category", "Danh mục sản phẩm")
                .description("Filter by product categories", "Lọc theo danh mục sản phẩm")
                .type("PRODUCT")
                .inputConfig(RuleInputConfigResponse.builder()
                        .dataSourceType("PRODUCT_CATEGORY")
                        .dataSourceEndpoint("/product/api/v1/categories")
                        .label("Select categories", "Chọn danh mục")
                        .placeholder("Choose categories...", "Chọn danh mục...")
                        .multiple(true)
                        .searchable(true)
                        .build())
                .operators(buildProductOperators())
                .build();
    }

    private RuleItemResponse buildProductBrandRule() {
        return RuleItemResponse.builder()
                .id("product_brand")
                .code("PRODUCT_BRAND")
                .name("Product brand", "Thương hiệu")
                .description("Filter by product brands", "Lọc theo thương hiệu sản phẩm")
                .type("PRODUCT")
                .inputConfig(RuleInputConfigResponse.builder()
                        .dataSourceType("PRODUCT_BRAND")
                        .dataSourceEndpoint("/product/api/v1/brands")
                        .label("Select brands", "Chọn thương hiệu")
                        .placeholder("Choose brands...", "Chọn thương hiệu...")
                        .multiple(true)
                        .searchable(true)
                        .build())
                .operators(buildProductOperators())
                .build();
    }

    private RuleItemResponse buildProductSkuRule() {
        return RuleItemResponse.builder()
                .id("product_sku")
                .code("PRODUCT_SKU")
                .name("Product SKU", "Mã sản phẩm")
                .description("Filter by specific products", "Lọc theo sản phẩm cụ thể")
                .type("PRODUCT")
                .inputConfig(RuleInputConfigResponse.builder()
                        .dataSourceType("PRODUCT")
                        .dataSourceEndpoint("/product/api/v1/products")
                        .label("Select products", "Chọn sản phẩm")
                        .placeholder("Choose products...", "Chọn sản phẩm...")
                        .multiple(true)
                        .searchable(true)
                        .build())
                .operators(buildProductOperators())
                .build();
    }

    private RuleItemResponse buildProductCollectionRule() {
        return RuleItemResponse.builder()
                .id("product_collection")
                .code("PRODUCT_COLLECTION")
                .name("Product collection", "Bộ sưu tập")
                .description("Filter by product collections", "Lọc theo bộ sưu tập sản phẩm")
                .type("COLLECTION")
                .inputConfig(RuleInputConfigResponse.builder()
                        .dataSourceType("COLLECTION")
                        .dataSourceEndpoint("/product/api/v1/collections")
                        .label("Select collections", "Chọn bộ sưu tập")
                        .placeholder("Choose collections...", "Chọn bộ sưu tập...")
                        .multiple(true)
                        .searchable(true)
                        .build())
                .operators(buildProductOperators())
                .build();
    }

    private RuleItemResponse buildAnyOrderItemRule() {
        return RuleItemResponse.builder()
                .id("any_order_item")
                .code("ANY_ORDER_ITEM")
                .name("Any order item", "Bất kỳ sản phẩm nào")
                .description("At least one item matches criteria", "Ít nhất một sản phẩm thỏa mãn")
                .type("PRODUCT")
                .inputConfig(null)
                .operators(buildProductOperators())
                .build();
    }

    private RuleItemResponse buildEveryOrderItemRule() {
        return RuleItemResponse.builder()
                .id("every_order_item")
                .code("EVERY_ORDER_ITEM")
                .name("Every order item", "Tất cả sản phẩm")
                .description("All items must match criteria", "Tất cả sản phẩm phải thỏa mãn")
                .type("PRODUCT")
                .inputConfig(null)
                .operators(buildProductOperators())
                .build();
    }

    private RuleItemResponse buildNoneOfOrderItemsRule() {
        return RuleItemResponse.builder()
                .id("none_of_order_items")
                .code("NONE_OF_ORDER_ITEMS")
                .name("None of order items", "Không có sản phẩm nào")
                .description("No items match criteria", "Không có sản phẩm nào thỏa mãn")
                .type("PRODUCT")
                .inputConfig(null)
                .operators(buildProductOperators())
                .build();
    }

    // ========== RULE BUILDERS - PRICES & QUANTITIES ==========

    private RuleItemResponse buildOrderTotalAmountRule() {
        return RuleItemResponse.builder()
                .id("order_total_amount")
                .code("ORDER_TOTAL_AMOUNT")
                .name("Order total amount", "Tổng giá trị đơn hàng")
                .description("Total order value after discounts", "Tổng giá trị đơn hàng sau giảm giá")
                .type("NUMBER")
                .inputConfig(RuleInputConfigResponse.builder()
                        .inputType("number")
                        .label("Amount", "Số tiền")
                        .placeholder("Enter amount...", "Nhập số tiền...")
                        .minValue("0")
                        .step("1000")
                        .build())
                .operators(buildNumberOperators())
                .build();
    }

    private RuleItemResponse buildOrderInitialAmountRule() {
        return RuleItemResponse.builder()
                .id("order_initial_amount")
                .code("ORDER_INITIAL_AMOUNT")
                .name("Order initial amount", "Giá trị ban đầu")
                .description("Total order value before discounts", "Tổng giá trị đơn hàng trước giảm giá")
                .type("NUMBER")
                .inputConfig(RuleInputConfigResponse.builder()
                        .inputType("number")
                        .label("Amount", "Số tiền")
                        .placeholder("Enter amount...", "Nhập số tiền...")
                        .minValue("0")
                        .step("1000")
                        .build())
                .operators(buildNumberOperators())
                .build();
    }

    private RuleItemResponse buildOrderItemQuantityRule() {
        return RuleItemResponse.builder()
                .id("order_item_quantity")
                .code("ORDER_ITEM_QUANTITY")
                .name("Order item quantity", "Số lượng sản phẩm")
                .description("Quantity of specific items", "Số lượng của sản phẩm cụ thể")
                .type("NUMBER")
                .inputConfig(RuleInputConfigResponse.builder()
                        .inputType("number")
                        .label("Quantity", "Số lượng")
                        .placeholder("Enter quantity...", "Nhập số lượng...")
                        .minValue("0")
                        .step("1")
                        .build())
                .operators(buildNumberOperators())
                .build();
    }

    private RuleItemResponse buildOrderItemPriceRule() {
        return RuleItemResponse.builder()
                .id("order_item_price")
                .code("ORDER_ITEM_PRICE")
                .name("Order item price", "Đơn giá sản phẩm")
                .description("Price of individual items", "Giá của từng sản phẩm")
                .type("NUMBER")
                .inputConfig(RuleInputConfigResponse.builder()
                        .inputType("number")
                        .label("Price", "Giá")
                        .placeholder("Enter price...", "Nhập giá...")
                        .minValue("0")
                        .step("1000")
                        .build())
                .operators(buildNumberOperators())
                .build();
    }

    private RuleItemResponse buildOrderItemsCountRule() {
        return RuleItemResponse.builder()
                .id("order_items_count")
                .code("ORDER_ITEMS_COUNT")
                .name("Order items count", "Tổng số sản phẩm")
                .description("Total number of items in order", "Tổng số sản phẩm trong đơn hàng")
                .type("NUMBER")
                .inputConfig(RuleInputConfigResponse.builder()
                        .inputType("number")
                        .label("Count", "Số lượng")
                        .placeholder("Enter count...", "Nhập số lượng...")
                        .minValue("0")
                        .step("1")
                        .build())
                .operators(buildNumberOperators())
                .build();
    }

    // ========== RULE BUILDERS - BUDGET & CONSTRAINTS ==========

    private RuleItemResponse buildTotalRedemptionsLimitRule() {
        return RuleItemResponse.builder()
                .id("total_redemptions_limit")
                .code("TOTAL_REDEMPTIONS_LIMIT")
                .name("Total redemptions limit", "Giới hạn tổng số lần sử dụng")
                .description("Maximum total redemptions allowed", "Số lần sử dụng tối đa cho toàn bộ chương trình")
                .type("NUMBER")
                .inputConfig(RuleInputConfigResponse.builder()
                        .inputType("number")
                        .label("Limit", "Giới hạn")
                        .placeholder("Enter limit...", "Nhập giới hạn...")
                        .minValue("1")
                        .step("1")
                        .build())
                .operators(buildLimitOperators())
                .build();
    }

    private RuleItemResponse buildRedemptionsPerCustomerRule() {
        return RuleItemResponse.builder()
                .id("redemptions_per_customer")
                .code("REDEMPTIONS_PER_CUSTOMER")
                .name("Redemptions per customer", "Số lần sử dụng mỗi khách")
                .description("Maximum redemptions per customer", "Số lần sử dụng tối đa mỗi khách hàng")
                .type("NUMBER")
                .inputConfig(RuleInputConfigResponse.builder()
                        .inputType("number")
                        .label("Limit", "Giới hạn")
                        .placeholder("Enter limit...", "Nhập giới hạn...")
                        .minValue("1")
                        .step("1")
                        .build())
                .operators(buildLimitOperators())
                .build();
    }

    private RuleItemResponse buildDailyRedemptionsLimitRule() {
        return RuleItemResponse.builder()
                .id("daily_redemptions_limit")
                .code("DAILY_REDEMPTIONS_LIMIT")
                .name("Daily redemptions limit", "Giới hạn theo ngày")
                .description("Maximum redemptions per day", "Số lần sử dụng tối đa mỗi ngày")
                .type("NUMBER")
                .inputConfig(RuleInputConfigResponse.builder()
                        .inputType("number")
                        .label("Daily limit", "Giới hạn ngày")
                        .placeholder("Enter daily limit...", "Nhập giới hạn ngày...")
                        .minValue("1")
                        .step("1")
                        .build())
                .operators(buildLimitOperators())
                .build();
    }

    private RuleItemResponse buildMonthlyRedemptionsLimitRule() {
        return RuleItemResponse.builder()
                .id("monthly_redemptions_limit")
                .code("MONTHLY_REDEMPTIONS_LIMIT")
                .name("Monthly redemptions limit", "Giới hạn theo tháng")
                .description("Maximum redemptions per month", "Số lần sử dụng tối đa mỗi tháng")
                .type("NUMBER")
                .inputConfig(RuleInputConfigResponse.builder()
                        .inputType("number")
                        .label("Monthly limit", "Giới hạn tháng")
                        .placeholder("Enter monthly limit...", "Nhập giới hạn tháng...")
                        .minValue("1")
                        .step("1")
                        .build())
                .operators(buildLimitOperators())
                .build();
    }

    private RuleItemResponse buildTotalBudgetLimitRule() {
        return RuleItemResponse.builder()
                .id("total_budget_limit")
                .code("TOTAL_BUDGET_LIMIT")
                .name("Total budget limit", "Ngân sách tổng")
                .description("Maximum total budget", "Ngân sách tối đa cho toàn bộ chương trình")
                .type("NUMBER")
                .inputConfig(RuleInputConfigResponse.builder()
                        .inputType("number")
                        .label("Budget", "Ngân sách")
                        .placeholder("Enter budget...", "Nhập ngân sách...")
                        .minValue("0")
                        .step("1000000")
                        .build())
                .operators(buildLimitOperators())
                .build();
    }

    private RuleItemResponse buildDailyBudgetLimitRule() {
        return RuleItemResponse.builder()
                .id("daily_budget_limit")
                .code("DAILY_BUDGET_LIMIT")
                .name("Daily budget limit", "Ngân sách theo ngày")
                .description("Maximum budget per day", "Ngân sách tối đa mỗi ngày")
                .type("NUMBER")
                .inputConfig(RuleInputConfigResponse.builder()
                        .inputType("number")
                        .label("Daily budget", "Ngân sách ngày")
                        .placeholder("Enter daily budget...", "Nhập ngân sách ngày...")
                        .minValue("0")
                        .step("100000")
                        .build())
                .operators(buildLimitOperators())
                .build();
    }

    private RuleItemResponse buildMonthlyBudgetLimitRule() {
        return RuleItemResponse.builder()
                .id("monthly_budget_limit")
                .code("MONTHLY_BUDGET_LIMIT")
                .name("Monthly budget limit", "Ngân sách theo tháng")
                .description("Maximum budget per month", "Ngân sách tối đa mỗi tháng")
                .type("NUMBER")
                .inputConfig(RuleInputConfigResponse.builder()
                        .inputType("number")
                        .label("Monthly budget", "Ngân sách tháng")
                        .placeholder("Enter monthly budget...", "Nhập ngân sách tháng...")
                        .minValue("0")
                        .step("1000000")
                        .build())
                .operators(buildLimitOperators())
                .build();
    }

    private RuleItemResponse buildCustomerDailyLimitRule() {
        return RuleItemResponse.builder()
                .id("customer_daily_limit")
                .code("CUSTOMER_DAILY_LIMIT")
                .name("Customer daily limit", "Giới hạn ngày mỗi khách")
                .description("Maximum redemptions per customer per day", "Số lần sử dụng tối đa mỗi khách mỗi ngày")
                .type("NUMBER")
                .inputConfig(RuleInputConfigResponse.builder()
                        .inputType("number")
                        .label("Daily limit", "Giới hạn ngày")
                        .placeholder("Enter daily limit...", "Nhập giới hạn ngày...")
                        .minValue("1")
                        .step("1")
                        .build())
                .operators(buildLimitOperators())
                .build();
    }

    private RuleItemResponse buildCustomerMonthlyLimitRule() {
        return RuleItemResponse.builder()
                .id("customer_monthly_limit")
                .code("CUSTOMER_MONTHLY_LIMIT")
                .name("Customer monthly limit", "Giới hạn tháng mỗi khách")
                .description("Maximum redemptions per customer per month", "Số lần sử dụng tối đa mỗi khách mỗi tháng")
                .type("NUMBER")
                .inputConfig(RuleInputConfigResponse.builder()
                        .inputType("number")
                        .label("Monthly limit", "Giới hạn tháng")
                        .placeholder("Enter monthly limit...", "Nhập giới hạn tháng...")
                        .minValue("1")
                        .step("1")
                        .build())
                .operators(buildLimitOperators())
                .build();
    }

    private RuleItemResponse buildCustomerTotalLimitRule() {
        return RuleItemResponse.builder()
                .id("customer_total_limit")
                .code("CUSTOMER_TOTAL_LIMIT")
                .name("Customer total limit", "Giới hạn tổng mỗi khách")
                .description("Maximum total redemptions per customer", "Tổng số lần sử dụng tối đa mỗi khách hàng")
                .type("NUMBER")
                .inputConfig(RuleInputConfigResponse.builder()
                        .inputType("number")
                        .label("Total limit", "Giới hạn tổng")
                        .placeholder("Enter total limit...", "Nhập giới hạn tổng...")
                        .minValue("1")
                        .step("1")
                        .build())
                .operators(buildLimitOperators())
                .build();
    }

    // ========== RULE BUILDERS - REDEMPTIONS ==========

    private RuleItemResponse buildRedeemingUserTypeRule() {
        return RuleItemResponse.builder()
                .id("redeeming_user_type")
                .code("REDEEMING_USER_TYPE")
                .name("Redeeming user type", "Loại người dùng")
                .description("Type of user making the redemption", "Loại người dùng thực hiện sử dụng")
                .type("TEXT")
                .inputConfig(RuleInputConfigResponse.builder()
                        .inputType("text")
                        .label("User type", "Loại người dùng")
                        .placeholder("Enter user type...", "Nhập loại người dùng...")
                        .build())
                .operators(buildTextOperators())
                .build();
    }

    private RuleItemResponse buildRedemptionApiKeyRule() {
        return RuleItemResponse.builder()
                .id("redemption_api_key")
                .code("REDEMPTION_API_KEY")
                .name("Redemption API key", "API key sử dụng")
                .description("API key used for redemption", "API key được sử dụng để thực hiện")
                .type("TEXT")
                .inputConfig(RuleInputConfigResponse.builder()
                        .inputType("text")
                        .label("API key", "API key")
                        .placeholder("Enter API key...", "Nhập API key...")
                        .build())
                .operators(buildTextOperators())
                .build();
    }

    private RuleItemResponse buildCodeHolderOnlyRule() {
        return RuleItemResponse.builder()
                .id("code_holder_only")
                .code("CODE_HOLDER_ONLY")
                .name("Code holder only", "Chỉ người giữ mã")
                .description("Only code holder can redeem", "Chỉ người giữ mã mới được sử dụng")
                .type("BOOLEAN")
                .inputConfig(null)
                .operators(buildBooleanOperators())
                .build();
    }

    // ========== METADATA RULE BUILDER ==========

    private RuleItemResponse buildMetadataRule(MetadataSchema schema) {
        String ruleType = mapMetadataFieldTypeToRuleType(schema.getFieldType());

        RuleInputConfigResponse.Builder inputConfigBuilder = RuleInputConfigResponse.builder();

        if (schema.getAvailableValues() != null && !schema.getAvailableValues().isEmpty()) {
            // Has predefined values - use select
            inputConfigBuilder
                    .inputType("select")
                    .multiple(false)
                    .searchable(true);
        } else {
            // No predefined values - use input
            String inputType = switch (schema.getFieldType()) {
                case NUMBER -> "number";
                case BOOLEAN -> "checkbox";
                case DATE -> "date";
                default -> "text";
            };
            inputConfigBuilder.inputType(inputType);
        }

        inputConfigBuilder
                .label(schema.getFieldName(), schema.getFieldName())
                .placeholder("Enter " + schema.getFieldName() + "...", "Nhập " + schema.getFieldName() + "...");

        return RuleItemResponse.builder()
                .id("metadata_" + schema.getFieldKey())
                .code("METADATA_" + schema.getFieldKey().toUpperCase())
                .name(schema.getFieldName(), schema.getFieldName())
                .description("Metadata field: " + schema.getFieldName(), "Trường metadata: " + schema.getFieldName())
                .type(ruleType)
                .inputConfig(inputConfigBuilder.build())
                .operators(buildOperatorsForMetadataType(schema.getFieldType()))
                .build();
    }

    private String mapMetadataFieldTypeToRuleType(MetadataSchema.FieldType fieldType) {
        return switch (fieldType) {
            case NUMBER -> "NUMBER";
            case BOOLEAN -> "BOOLEAN";
            case DATE -> "DATE";
            default -> "TEXT";
        };
    }

    private List<OperatorResponse> buildOperatorsForMetadataType(MetadataSchema.FieldType fieldType) {
        return switch (fieldType) {
            case NUMBER -> buildNumberOperators();
            case BOOLEAN -> buildBooleanOperators();
            default -> buildTextOperators();
        };
    }

    // ========== OPERATOR BUILDERS ==========

    private List<OperatorResponse> buildSegmentOperators() {
        return List.of(
                OperatorResponse.of("is", "is", "Thuộc"),
                OperatorResponse.of("is_not", "is not", "Không thuộc"),
                OperatorResponse.of("is_any", "is any of", "Thuộc một trong"),
                OperatorResponse.of("is_none", "is none of", "Không thuộc bất kỳ")
        );
    }

    private List<OperatorResponse> buildProductOperators() {
        return List.of(
                OperatorResponse.of("is", "is", "Là"),
                OperatorResponse.of("is_not", "is not", "Không là"),
                OperatorResponse.of("is_any", "is any of", "Là một trong"),
                OperatorResponse.of("is_none", "is none of", "Không là bất kỳ")
        );
    }

    private List<OperatorResponse> buildNumberOperators() {
        return List.of(
                OperatorResponse.of("equals", "equals", "Bằng"),
                OperatorResponse.of("not_equals", "not equals", "Không bằng"),
                OperatorResponse.of("greater_than", "greater than", "Lớn hơn"),
                OperatorResponse.of("greater_than_or_equal", "greater than or equal", "Lớn hơn hoặc bằng"),
                OperatorResponse.of("less_than", "less than", "Nhỏ hơn"),
                OperatorResponse.of("less_than_or_equal", "less than or equal", "Nhỏ hơn hoặc bằng"),
                OperatorResponse.of("between", "between", "Trong khoảng"),
                OperatorResponse.of("not_between", "not between", "Ngoài khoảng")
        );
    }

    private List<OperatorResponse> buildTextOperators() {
        return List.of(
                OperatorResponse.of("equals", "equals", "Bằng"),
                OperatorResponse.of("not_equals", "not equals", "Không bằng"),
                OperatorResponse.of("contains", "contains", "Chứa"),
                OperatorResponse.of("not_contains", "does not contain", "Không chứa"),
                OperatorResponse.of("starts_with", "starts with", "Bắt đầu bằng"),
                OperatorResponse.of("ends_with", "ends with", "Kết thúc bằng")
        );
    }

    private List<OperatorResponse> buildBooleanOperators() {
        return List.of(
                OperatorResponse.of("is_true", "is true", "Đúng"),
                OperatorResponse.of("is_false", "is false", "Sai")
        );
    }

    private List<OperatorResponse> buildLimitOperators() {
        return List.of(
                OperatorResponse.of("less_than_or_equal", "less than or equal", "Nhỏ hơn hoặc bằng"),
                OperatorResponse.of("equals", "equals", "Bằng")
        );
    }

    // ========== MOCK OPTIONS (for testing) ==========

    private List<RuleOptionResponse> getMockOptionsForRule(String ruleId, String search) {
        // In production, this should call external services based on ruleId
        // For now, return mock data
        return switch (ruleId) {
            case "customer_segment" -> List.of(
                    RuleOptionResponse.of("seg-001", "VIP Customers", "Khách hàng VIP", Map.of("customerCount", 1250)),
                    RuleOptionResponse.of("seg-002", "New Customers", "Khách hàng mới", Map.of("customerCount", 3400)),
                    RuleOptionResponse.of("seg-003", "Loyal Customers", "Khách hàng trung thành", Map.of("customerCount", 890))
            );
            case "customer_loyalty_tier" -> List.of(
                    RuleOptionResponse.of("tier-gold", "Gold", "Vàng"),
                    RuleOptionResponse.of("tier-silver", "Silver", "Bạc"),
                    RuleOptionResponse.of("tier-bronze", "Bronze", "Đồng")
            );
            case "product_category" -> List.of(
                    RuleOptionResponse.of("cat-electronics", "Electronics", "Điện tử"),
                    RuleOptionResponse.of("cat-fashion", "Fashion", "Thời trang"),
                    RuleOptionResponse.of("cat-food", "Food & Beverage", "Thực phẩm & đồ uống")
            );
            default -> List.of();
        };
    }
}
