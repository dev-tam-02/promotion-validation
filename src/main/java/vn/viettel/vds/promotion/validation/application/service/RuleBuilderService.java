package vn.viettel.vds.promotion.validation.application.service;

import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import vn.viettel.vds.promotion.validation.adapter.in.web.dto.rulebuilder.*;
import vn.viettel.vds.promotion.validation.adapter.out.external.MetadataServiceFeignClient;
import vn.viettel.vds.promotion.validation.application.port.out.RuleOptionsLookupPort;
import vn.viettel.vds.promotion.validation.application.port.out.RuleOptionsPage;
import vn.viettel.vds.promotion.validation.domain.model.OperatorCategory;
import vn.viettel.vds.promotion.validation.domain.model.OperatorOption;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Service for Rule Builder API.
 * Provides rule categories and options from database.
 */
@Service
public class RuleBuilderService {

    private static final Logger logger = LoggerFactory.getLogger(RuleBuilderService.class);
    private static final String VERSION = "1.0.0";
    private static final String OP_EQUALS = "equals";
    private static final String OP_NOT_EQUALS = "not_equals";
    private static final String OP_NOT_IN = "not_in";
    private static final String OP_BETWEEN = "between";
    private static final String OP_CONTAINS = "contains";
    private static final String OP_NOT_CONTAINS = "not_contains";
    private static final String OP_IS = "is";
    private static final String OP_IS_NOT = "is_not";
    private static final String OP_EXISTS = "exists";
    private static final String OP_NOT_EXISTS = "not_exists";
    private static final String OP_IS_EXACTLY = "is_exactly";
    private static final String OP_IS_MORE_THAN = "is_more_than";
    private static final String OP_IS_LESS_THAN = "is_less_than";
    private static final String OP_IS_MORE_THAN_OR_EQUAL_TO = "is_more_than_or_equal_to";
    private static final String OP_IS_LESS_THAN_OR_EQUAL_TO = "is_less_than_or_equal_to";
    private static final String OP_IS_BEFORE = "is_before";
    private static final String OP_IS_AFTER = "is_after";
    private static final String OP_IS_TRUE = "is_true";
    private static final String OP_IS_FALSE = "is_false";
    private static final String OP_LABEL_NOT_EQUALS = "not equals";
    private static final String OP_LABEL_VI_EQUALS = "Bằng";
    private static final String OP_LABEL_VI_NOT_EQUALS = "Không bằng";
    private static final String OP_LABEL_VI_GTE = "Lớn hơn hoặc bằng";
    private static final String OP_LABEL_VI_LTE = "Nhỏ hơn hoặc bằng";
    private static final String OP_LABEL_IS_ANY_OF = "is any of";
    private static final String OP_LABEL_IS_NONE_OF = "is none of";

    private static final String DATA_SOURCE_STATIC = "STATIC";

    private static final Map<String, OperatorResponse> OPERATOR_MAP = buildOperatorMap();

    private static Map<String, OperatorResponse> buildOperatorMap() {
        Map<String, OperatorResponse> m = new LinkedHashMap<>();
        m.put(OP_EQUALS, OperatorResponse.of(OP_EQUALS, OP_EQUALS, OP_LABEL_VI_EQUALS));
        m.put(OP_NOT_EQUALS, OperatorResponse.of(OP_NOT_EQUALS, OP_LABEL_NOT_EQUALS, OP_LABEL_VI_NOT_EQUALS));
        m.put("in", OperatorResponse.of("in", OP_LABEL_IS_ANY_OF, "Thuộc một trong"));
        m.put(OP_NOT_IN, OperatorResponse.of(OP_NOT_IN, OP_LABEL_IS_NONE_OF, "Không thuộc bất kỳ"));
        OperatorResponse gte = OperatorResponse.of("gte", "greater than or equal", OP_LABEL_VI_GTE);
        m.put("gte", gte);
        m.put("greater_than_or_equal", gte);
        OperatorResponse gt = OperatorResponse.of("gt", "greater than", "Lớn hơn");
        m.put("gt", gt);
        m.put("greater_than", gt);
        OperatorResponse lte = OperatorResponse.of("lte", "less than or equal", OP_LABEL_VI_LTE);
        m.put("lte", lte);
        m.put("less_than_or_equal", lte);
        OperatorResponse lt = OperatorResponse.of("lt", "less than", "Nhỏ hơn");
        m.put("lt", lt);
        m.put("less_than", lt);
        m.put(OP_BETWEEN, OperatorResponse.of(OP_BETWEEN, OP_BETWEEN, "Trong khoảng"));
        m.put("not_between", OperatorResponse.of("not_between", "not between", "Ngoài khoảng"));
        m.put(OP_CONTAINS, OperatorResponse.of(OP_CONTAINS, OP_CONTAINS, "Chứa"));
        m.put(OP_NOT_CONTAINS, OperatorResponse.of(OP_NOT_CONTAINS, "does not contain", "Không chứa"));
        m.put("starts_with", OperatorResponse.of("starts_with", "starts with", "Bắt đầu bằng"));
        m.put("ends_with", OperatorResponse.of("ends_with", "ends with", "Kết thúc bằng"));
        m.put(OP_IS_TRUE, OperatorResponse.of(OP_IS_TRUE, "is true", "Đúng"));
        m.put(OP_IS_FALSE, OperatorResponse.of(OP_IS_FALSE, "is false", "Sai"));
        m.put(OP_IS, OperatorResponse.of(OP_IS, OP_IS, "Là"));
        m.put(OP_IS_NOT, OperatorResponse.of(OP_IS_NOT, "is not", "Không là"));
        m.put("is_any", OperatorResponse.of("is_any", OP_LABEL_IS_ANY_OF, "Là một trong"));
        m.put("is_none", OperatorResponse.of("is_none", OP_LABEL_IS_NONE_OF, "Không là bất kỳ"));
        m.put(OP_IS_MORE_THAN, OperatorResponse.of(OP_IS_MORE_THAN, "is more than", "Lớn hơn"));
        m.put(OP_IS_LESS_THAN, OperatorResponse.of(OP_IS_LESS_THAN, "is less than", "Nhỏ hơn"));
        m.put(OP_IS_EXACTLY, OperatorResponse.of(OP_IS_EXACTLY, "is exactly", "Đúng bằng"));
        m.put(OP_IS_MORE_THAN_OR_EQUAL_TO,
                OperatorResponse.of(OP_IS_MORE_THAN_OR_EQUAL_TO, "is more than or equal to", OP_LABEL_VI_GTE));
        m.put(OP_IS_LESS_THAN_OR_EQUAL_TO,
                OperatorResponse.of(OP_IS_LESS_THAN_OR_EQUAL_TO, "is less than or equal to", OP_LABEL_VI_LTE));
        m.put("from", OperatorResponse.of("from", "from", "Từ"));
        m.put(OP_EXISTS, OperatorResponse.of(OP_EXISTS, OP_EXISTS, "Tồn tại"));
        m.put(OP_NOT_EXISTS, OperatorResponse.of(OP_NOT_EXISTS, "does not exist", "Không tồn tại"));
        m.put(OP_IS_BEFORE, OperatorResponse.of(OP_IS_BEFORE, "is before", "Trước"));
        m.put(OP_IS_AFTER, OperatorResponse.of(OP_IS_AFTER, "is after", "Sau"));
        return Collections.unmodifiableMap(m);
    }

    private final OperatorConfigService operatorConfigService;
    private final RuleOptionsLookupPort ruleOptionsLookupPort;
    private final MetadataServiceFeignClient metadataServiceFeignClient;

    public RuleBuilderService(OperatorConfigService operatorConfigService,
                               RuleOptionsLookupPort ruleOptionsLookupPort,
                               MetadataServiceFeignClient metadataServiceFeignClient) {
        this.operatorConfigService = operatorConfigService;
        this.ruleOptionsLookupPort = ruleOptionsLookupPort;
        this.metadataServiceFeignClient = metadataServiceFeignClient;
    }

    /**
     * Get all rule categories with their rules from database.
     */
    public RuleCategoriesResponse getAllCategories(String tenantId) {
        logger.debug("Getting all rule categories for tenant: {}", tenantId);

        List<OperatorCategory> categories = operatorConfigService.getAllCategoriesWithOptions(tenantId);

        List<RuleCategoryResponse> categoryResponses = categories.stream()
                .map(this::mapCategoryToResponse)
                .toList();

        return RuleCategoriesResponse.of(categoryResponses, VERSION);
    }

    /**
     * Get options for a specific rule.
     * Routes by dataSourceType: null/"STATIC" uses in-memory predefined options;
     * any other value (e.g. "SEGMENT") delegates to the external lookup port.
     */
    public RuleOptionsResponse getRuleOptions(String ruleId, String search, Integer page, Integer size, String tenantId) {
        logger.debug("Getting options for rule: {} (search: {}, page: {}, size: {}, tenant: {})",
                ruleId, search, page, size, tenantId);

        List<OperatorOption> allOptions = operatorConfigService.getAllCategoriesWithOptions(tenantId)
                .stream()
                .flatMap(cat -> cat.getOptions().stream())
                .filter(opt -> opt.getCode().equals(ruleId) || opt.getId().equals(ruleId))
                .toList();

        if (allOptions.isEmpty()) {
            return RuleOptionsResponse.paginated(ruleId, Collections.emptyList(), 0, 0, 20);
        }

        OperatorOption option = allOptions.get(0);
        String dataSourceType = option.getDataSourceType();
        int pageNumber = page != null ? page : 0;
        int pageSize = size != null ? size : 20;

        if (dataSourceType == null || DATA_SOURCE_STATIC.equalsIgnoreCase(dataSourceType)) {
            return getStaticOptions(ruleId, option, search, pageNumber, pageSize);
        }

        return getExternalOptions(ruleId, option, dataSourceType, search, pageNumber, pageSize, tenantId);
    }

    /**
     * Return in-memory predefined value options with client-side filtering and pagination.
     * Uses {@code labelEn}/{@code labelVi} from the domain model for proper i18n;
     * falls back to the legacy {@code label} field if not set (backward compat).
     */
    private RuleOptionsResponse getStaticOptions(String ruleId, OperatorOption option,
                                                  String search, int pageNumber, int pageSize) {
        List<RuleOptionResponse> options = Collections.emptyList();
        if (option.getValueOptions() != null && !option.getValueOptions().isEmpty()) {
            Predicate<OperatorOption.ValueOption> searchFilter = matchSearch(search);
            options = option.getValueOptions().stream()
                    .filter(searchFilter)
                    .map(RuleBuilderService::toRuleOptionResponse)
                    .toList();
        }

        int totalElements = options.size();
        int fromIndex = Math.min(pageNumber * pageSize, totalElements);
        int toIndex = Math.min(fromIndex + pageSize, totalElements);
        List<RuleOptionResponse> pagedOptions = options.subList(fromIndex, toIndex);

        return RuleOptionsResponse.paginated(ruleId, pagedOptions, totalElements, pageNumber, pageSize);
    }

    private static Predicate<OperatorOption.ValueOption> matchSearch(String search) {
        if (search == null || search.isBlank()) {
            return vo -> true;
        }
        String lower = search.toLowerCase();
        return vo -> {
            String searchTarget = preferredLabel(vo);
            return (searchTarget != null && searchTarget.toLowerCase().contains(lower))
                    || (vo.getValue() != null && vo.getValue().toLowerCase().contains(lower));
        };
    }

    private static RuleOptionResponse toRuleOptionResponse(OperatorOption.ValueOption vo) {
        return RuleOptionResponse.of(vo.getValue(), preferredLabel(vo), preferredLabelVi(vo));
    }

    @SuppressWarnings("deprecation") // Legacy label kept as fallback when labelEn is null
    private static String preferredLabel(OperatorOption.ValueOption vo) {
        return vo.getLabelEn() != null ? vo.getLabelEn() : vo.getLabel();
    }

    @SuppressWarnings("deprecation") // Legacy label kept as fallback when labelVi is null
    private static String preferredLabelVi(OperatorOption.ValueOption vo) {
        return vo.getLabelVi() != null ? vo.getLabelVi() : vo.getLabel();
    }

    /**
     * Delegate to the external lookup port and map the result to the web DTO.
     */
    private RuleOptionsResponse getExternalOptions(String ruleId, OperatorOption option,
                                                    String dataSourceType, String search,
                                                    int pageNumber, int pageSize, String tenantId) {
        RuleOptionsPage resultPage = ruleOptionsLookupPort.lookup(
                dataSourceType, option.getDataSourceEndpoint(), search, pageNumber, pageSize, tenantId);

        List<RuleOptionResponse> mapped = resultPage.items().stream()
                .map(item -> {
                    Map<String, Object> meta = item.metadata();
                    return (meta == null || meta.isEmpty())
                            ? RuleOptionResponse.of(item.value(), item.labelEn(), item.labelVi())
                            : RuleOptionResponse.of(item.value(), item.labelEn(), item.labelVi(), meta);
                })
                .toList();

        return RuleOptionsResponse.paginated(ruleId, mapped, resultPage.totalElements(), pageNumber, pageSize);
    }

    /**
     * Map OperatorCategory domain model to RuleCategoryResponse DTO.
     *
     * <p>For metadata categories (customer / order / redemption), rules are
     * not seeded in operator_options. Instead they are resolved at request
     * time by calling pp-metadata for the schema's field definitions, and
     * one rule is synthesized per field. The data type drives the available
     * comparator list per spec.
     */
    private RuleCategoryResponse mapCategoryToResponse(OperatorCategory category) {
        List<RuleItemResponse> rules = new ArrayList<>();

        if (category.getOptions() != null) {
            category.getOptions().stream()
                    .map(this::mapOptionToRuleItem)
                    .forEach(rules::add);
        }

        if (category.isMetadataCategory() && category.getMetadataSchemaType() != null) {
            rules.addAll(resolveMetadataRules(category));
        }

        return RuleCategoryResponse.builder()
                .id(category.getCode())
                .code(category.getCode().toUpperCase())
                .name(category.getName(), category.getNameVi() != null ? category.getNameVi() : category.getName())
                .icon(category.getIcon())
                .order(category.getDisplayOrder())
                .rules(rules)
                .build();
    }

    /**
     * Resolve metadata category rules at request time by calling pp-metadata.
     * Each field in the schema becomes one synthesized rule; available
     * comparators are derived from the field's data type.
     *
     * <p>Failure modes (pp-metadata down / no schema / no fields) all degrade
     * to an empty rule list — the category still renders, just empty.
     */
    @SuppressWarnings("unchecked")
    private List<RuleItemResponse> resolveMetadataRules(OperatorCategory category) {
        String schemaType = "STANDARD";
        String schemaName = category.getMetadataSchemaType();
        try {
            Map<String, Object> listResp = metadataServiceFeignClient.listSchemas(
                    schemaType, schemaName, 0, 20);
            Map<String, Object> listData = (Map<String, Object>) listResp.get("data");
            if (listData == null) return Collections.emptyList();
            List<Map<String, Object>> schemas = (List<Map<String, Object>>) listData.get("content");
            if (schemas == null || schemas.isEmpty()) {
                logger.debug("No metadata schema found for type={}, name={}", schemaType, schemaName);
                return Collections.emptyList();
            }
            String schemaId = (String) schemas.get(0).get("id");
            if (schemaId == null) return Collections.emptyList();

            Map<String, Object> schemaResp = metadataServiceFeignClient.getSchemaById(schemaId, 0, 100);
            Map<String, Object> schemaData = (Map<String, Object>) schemaResp.get("data");
            if (schemaData == null) return Collections.emptyList();
            Map<String, Object> definitions = (Map<String, Object>) schemaData.get("definitions");
            if (definitions == null) return Collections.emptyList();
            List<Map<String, Object>> fields = (List<Map<String, Object>>) definitions.get("content");
            if (fields == null || fields.isEmpty()) return Collections.emptyList();

            return fields.stream()
                    .map(f -> synthesizeMetadataRule(category, f))
                    .toList();
        } catch (FeignException e) {
            logger.error("Failed to resolve metadata rules for category {}: {}",
                    category.getCode(), e.getMessage());
            return Collections.emptyList();
        } catch (RuntimeException e) {
            logger.error("Unexpected error resolving metadata rules for category {}",
                    category.getCode(), e);
            return Collections.emptyList();
        }
    }

    private RuleItemResponse synthesizeMetadataRule(OperatorCategory category,
                                                     Map<String, Object> field) {
        String fieldName = stringOrEmpty(field.get("name"));
        String displayName = stringOrFallback(field.get("displayName"), fieldName);
        String dataType = stringOrFallback(field.get("type"), "STRING").toUpperCase();
        String description = stringOrEmpty(field.get("description"));

        List<String> comparators = comparatorsForMetadataType(dataType);

        return RuleItemResponse.builder()
                .id(category.getCode().toLowerCase() + "." + fieldName)
                .code((category.getCode() + "_" + fieldName).toUpperCase())
                .name(displayName, displayName)
                .description(description, description)
                .type(dataType)
                .operators(comparators.stream().map(this::mapComparatorToOperator).toList())
                .build();
    }

    /**
     * Default comparator set per metadata field data type — per spec.
     */
    private List<String> comparatorsForMetadataType(String dataType) {
        return switch (dataType) {
            case "STRING", "TEXT" ->
                    List.of(OP_IS, OP_IS_NOT, OP_CONTAINS, OP_NOT_CONTAINS, OP_EXISTS, OP_NOT_EXISTS);
            case "NUMBER", "INTEGER", "DECIMAL" -> List.of(
                    OP_IS_MORE_THAN, OP_IS_EXACTLY, OP_IS_LESS_THAN,
                    OP_IS_MORE_THAN_OR_EQUAL_TO, OP_IS_LESS_THAN_OR_EQUAL_TO);
            case "BOOLEAN" -> List.of(OP_IS);
            case "DATE", "DATETIME", "TIMESTAMP" ->
                    List.of(OP_IS_BEFORE, OP_IS_AFTER, OP_IS_EXACTLY, OP_BETWEEN);
            case "ENUM" -> List.of(OP_IS, OP_IS_NOT, "in", OP_NOT_IN);
            default -> List.of(OP_IS, OP_IS_NOT);
        };
    }

    private String stringOrEmpty(Object o) {
        return o == null ? "" : o.toString();
    }

    private String stringOrFallback(Object o, String fallback) {
        return o == null ? fallback : o.toString();
    }

    /**
     * Map OperatorOption domain model to RuleItemResponse DTO.
     */
    private RuleItemResponse mapOptionToRuleItem(OperatorOption option) {
        String descEn = option.getDescription() != null ? option.getDescription() : "";
        String descVi = option.getDescriptionVi() != null ? option.getDescriptionVi() : descEn;
        return RuleItemResponse.builder()
                .id(option.getCode())
                .code(option.getCode().toUpperCase())
                .name(option.getName(), option.getNameVi() != null ? option.getNameVi() : option.getName())
                .description(descEn, descVi)
                .type(mapValueTypeToRuleType(option.getValueType(), option.getDataSourceType()))
                .autoApply(option.getAutoApply())
                .defaultOperator(option.getDefaultOperator())
                .inputConfig(buildInputConfig(option))
                .operators(buildOperators(option))
                .build();
    }

    /**
     * Map ValueType to rule type string.
     */
    private String mapValueTypeToRuleType(OperatorOption.ValueType valueType, String dataSourceType) {
        // If dataSourceType is specified, use it as type
        if (dataSourceType != null && !dataSourceType.isBlank()) {
            return dataSourceType;
        }

        if (valueType == null) {
            return "TEXT";
        }

        return switch (valueType) {
            case NUMBER -> "NUMBER";
            case BOOLEAN -> "BOOLEAN";
            case DATE -> "DATE";
            case LIST -> "LIST";
            default -> "TEXT";
        };
    }

    /**
     * Build input configuration from option.
     */
    private RuleInputConfigResponse buildInputConfig(OperatorOption option) {
        if (option.getValueType() == OperatorOption.ValueType.BOOLEAN) {
            return null;
        }
        if (option.getInputType() == null && option.getDataSourceType() == null
                && option.getValueSource() == null && option.getDataLoaderType() == null) {
            return null;
        }

        RuleInputConfigResponse.Builder builder = RuleInputConfigResponse.builder();
        configureDataSource(builder, option);
        configureInputType(builder, option);
        configureLabels(builder, option);
        configureNumberConstraints(builder, option);
        return builder.build();
    }

    private void configureDataSource(RuleInputConfigResponse.Builder builder, OperatorOption option) {
        if (option.getDataSourceType() != null) {
            builder.dataSourceType(option.getDataSourceType());
        }
        if (option.getDataSourceEndpoint() != null) {
            builder.dataSourceEndpoint(option.getDataSourceEndpoint());
        }
        if (option.getDataLoaderType() != null) {
            builder.dataLoaderType(option.getDataLoaderType());
        }
        if (option.getDataLoaderConfig() != null) {
            builder.dataLoaderConfig(option.getDataLoaderConfig());
        }
    }

    private void configureInputType(RuleInputConfigResponse.Builder builder, OperatorOption option) {
        String inputType = option.getInputType();
        if (inputType == null && option.getValueType() != null) {
            inputType = switch (option.getValueType()) {
                case NUMBER -> "number";
                case BOOLEAN -> "checkbox";
                case DATE -> "date";
                default -> "text";
            };
        }
        if (inputType != null) {
            builder.inputType(inputType);
        }
        if (option.getInputMultiple() != null) {
            builder.multiple(option.getInputMultiple());
        }
        if (option.getInputSearchable() != null) {
            builder.searchable(option.getInputSearchable());
        }
    }

    private void configureLabels(RuleInputConfigResponse.Builder builder, OperatorOption option) {
        if (option.getLabelEn() != null || option.getLabelVi() != null) {
            String labelEn = option.getLabelEn() != null ? option.getLabelEn() : option.getName();
            String nameViFallback = option.getNameVi() != null ? option.getNameVi() : option.getName();
            String labelVi = option.getLabelVi() != null ? option.getLabelVi() : nameViFallback;
            builder.label(labelEn, labelVi);
        }
        if (option.getPlaceholderEn() != null || option.getPlaceholderVi() != null) {
            builder.placeholder(
                    option.getPlaceholderEn() != null ? option.getPlaceholderEn() : "",
                    option.getPlaceholderVi() != null ? option.getPlaceholderVi() : ""
            );
        }
    }

    private void configureNumberConstraints(RuleInputConfigResponse.Builder builder, OperatorOption option) {
        if (option.getMinValue() != null) {
            builder.minValue(option.getMinValue().toString());
        }
        if (option.getMaxValue() != null) {
            builder.maxValue(option.getMaxValue().toString());
        }
        if (option.getInputStep() != null) {
            builder.step(option.getInputStep());
        }
    }

    /**
     * Build operators list from option's available comparators.
     */
    private List<OperatorResponse> buildOperators(OperatorOption option) {
        if (option.getAvailableComparators() == null || option.getAvailableComparators().isEmpty()) {
            // Return default operators based on comparison type
            return getDefaultOperators(option.getComparisonType());
        }

        return option.getAvailableComparators().stream()
                .map(this::mapComparatorToOperator)
                .toList();
    }

    /**
     * Get default operators based on comparison type.
     */
    private List<OperatorResponse> getDefaultOperators(OperatorOption.ComparisonType comparisonType) {
        if (comparisonType == null) {
            return defaultSingleOperators();
        }
        return switch (comparisonType) {
            case RANGE -> List.of(
                    OPERATOR_MAP.get(OP_EQUALS),
                    OPERATOR_MAP.get(OP_NOT_EQUALS),
                    OPERATOR_MAP.get("gte"),
                    OPERATOR_MAP.get("lte"),
                    OPERATOR_MAP.get(OP_BETWEEN)
            );
            case LIST -> List.of(OPERATOR_MAP.get("in"), OPERATOR_MAP.get(OP_NOT_IN));
            case BOOLEAN -> List.of(OPERATOR_MAP.get(OP_IS_TRUE), OPERATOR_MAP.get(OP_IS_FALSE));
            default -> defaultSingleOperators();
        };
    }

    private List<OperatorResponse> defaultSingleOperators() {
        return List.of(OPERATOR_MAP.get(OP_EQUALS), OPERATOR_MAP.get(OP_NOT_EQUALS));
    }

    /**
     * Map comparator string to OperatorResponse.
     * Falls back to passing the raw comparator through when it is not known.
     */
    private OperatorResponse mapComparatorToOperator(String comparator) {
        OperatorResponse mapped = OPERATOR_MAP.get(comparator.toLowerCase());
        return mapped != null ? mapped : OperatorResponse.of(comparator, comparator, comparator);
    }
}
