package vn.viettel.vds.promotion.validation.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import vn.viettel.vds.promotion.validation.adapter.in.web.dto.rulebuilder.*;
import vn.viettel.vds.promotion.validation.domain.model.OperatorCategory;
import vn.viettel.vds.promotion.validation.domain.model.OperatorOption;

import java.util.Collections;
import java.util.List;

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
    private static final String OP_IS_TRUE = "is_true";
    private static final String OP_IS_FALSE = "is_false";
    private static final String OP_LABEL_NOT_EQUALS = "not equals";
    private static final String OP_LABEL_VI_EQUALS = "Bằng";
    private static final String OP_LABEL_VI_NOT_EQUALS = "Không bằng";
    private static final String OP_LABEL_IS_ANY_OF = "is any of";
    private static final String OP_LABEL_IS_NONE_OF = "is none of";

    private final OperatorConfigService operatorConfigService;

    public RuleBuilderService(OperatorConfigService operatorConfigService) {
        this.operatorConfigService = operatorConfigService;
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
     * This delegates to external services based on dataSourceEndpoint configuration.
     */
    public RuleOptionsResponse getRuleOptions(String ruleId, String search, Integer page, Integer size, String tenantId) {
        logger.debug("Getting options for rule: {} (search: {}, page: {}, size: {}, tenant: {})",
                ruleId, search, page, size, tenantId);

        // Get options from the operator configuration
        // In production, this should call external services based on dataSourceEndpoint
        List<OperatorOption> allOptions = operatorConfigService.getAllCategoriesWithOptions(tenantId)
                .stream()
                .flatMap(cat -> cat.getOptions().stream())
                .filter(opt -> opt.getCode().equals(ruleId) || opt.getId().equals(ruleId))
                .toList();

        if (allOptions.isEmpty()) {
            return RuleOptionsResponse.paginated(ruleId, Collections.emptyList(), 0, 0, 20);
        }

        OperatorOption option = allOptions.get(0);

        // If option has predefined value options, return them
        List<RuleOptionResponse> options = Collections.emptyList();
        if (option.getValueOptions() != null && !option.getValueOptions().isEmpty()) {
            options = option.getValueOptions().stream()
                    .filter(vo -> search == null || search.isBlank() ||
                            vo.getLabel().toLowerCase().contains(search.toLowerCase()) ||
                            vo.getValue().toLowerCase().contains(search.toLowerCase()))
                    .map(vo -> RuleOptionResponse.of(vo.getValue(), vo.getLabel(), vo.getLabel()))
                    .toList();
        }

        int totalElements = options.size();
        int pageNumber = page != null ? page : 0;
        int pageSize = size != null ? size : 20;

        // Simple pagination
        int fromIndex = Math.min(pageNumber * pageSize, totalElements);
        int toIndex = Math.min(fromIndex + pageSize, totalElements);
        List<RuleOptionResponse> pagedOptions = options.subList(fromIndex, toIndex);

        return RuleOptionsResponse.paginated(ruleId, pagedOptions, totalElements, pageNumber, pageSize);
    }

    /**
     * Map OperatorCategory domain model to RuleCategoryResponse DTO.
     */
    private RuleCategoryResponse mapCategoryToResponse(OperatorCategory category) {
        List<RuleItemResponse> rules = category.getOptions() != null
                ? category.getOptions().stream()
                        .map(this::mapOptionToRuleItem)
                        .toList()
                : Collections.emptyList();

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
        if (option.getInputType() == null && option.getDataSourceType() == null && option.getValueSource() == null) {
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
            return List.of(
                    OperatorResponse.of(OP_EQUALS, OP_EQUALS, OP_LABEL_VI_EQUALS),
                    OperatorResponse.of(OP_NOT_EQUALS, OP_LABEL_NOT_EQUALS, OP_LABEL_VI_NOT_EQUALS)
            );
        }

        return switch (comparisonType) {
            case RANGE -> List.of(
                    OperatorResponse.of(OP_EQUALS, OP_EQUALS, OP_LABEL_VI_EQUALS),
                    OperatorResponse.of(OP_NOT_EQUALS, OP_LABEL_NOT_EQUALS, OP_LABEL_VI_NOT_EQUALS),
                    OperatorResponse.of("gte", "greater than or equal", "Lớn hơn hoặc bằng"),
                    OperatorResponse.of("lte", "less than or equal", "Nhỏ hơn hoặc bằng"),
                    OperatorResponse.of(OP_BETWEEN, OP_BETWEEN, "Trong khoảng")
            );
            case LIST -> List.of(
                    OperatorResponse.of("in", OP_LABEL_IS_ANY_OF, "Thuộc một trong"),
                    OperatorResponse.of(OP_NOT_IN, OP_LABEL_IS_NONE_OF, "Không thuộc bất kỳ")
            );
            case BOOLEAN -> List.of(
                    OperatorResponse.of(OP_IS_TRUE, "is true", "Đúng"),
                    OperatorResponse.of(OP_IS_FALSE, "is false", "Sai")
            );
            default -> List.of(
                    OperatorResponse.of(OP_EQUALS, OP_EQUALS, OP_LABEL_VI_EQUALS),
                    OperatorResponse.of(OP_NOT_EQUALS, OP_LABEL_NOT_EQUALS, OP_LABEL_VI_NOT_EQUALS)
            );
        };
    }

    /**
     * Map comparator string to OperatorResponse.
     */
    private OperatorResponse mapComparatorToOperator(String comparator) {
        return switch (comparator.toLowerCase()) {
            case OP_EQUALS -> OperatorResponse.of(OP_EQUALS, OP_EQUALS, OP_LABEL_VI_EQUALS);
            case OP_NOT_EQUALS -> OperatorResponse.of(OP_NOT_EQUALS, OP_LABEL_NOT_EQUALS, OP_LABEL_VI_NOT_EQUALS);
            case "in" -> OperatorResponse.of("in", OP_LABEL_IS_ANY_OF, "Thuộc một trong");
            case OP_NOT_IN -> OperatorResponse.of(OP_NOT_IN, OP_LABEL_IS_NONE_OF, "Không thuộc bất kỳ");
            case "gte", "greater_than_or_equal" -> OperatorResponse.of("gte", "greater than or equal", "Lớn hơn hoặc bằng");
            case "gt", "greater_than" -> OperatorResponse.of("gt", "greater than", "Lớn hơn");
            case "lte", "less_than_or_equal" -> OperatorResponse.of("lte", "less than or equal", "Nhỏ hơn hoặc bằng");
            case "lt", "less_than" -> OperatorResponse.of("lt", "less than", "Nhỏ hơn");
            case OP_BETWEEN -> OperatorResponse.of(OP_BETWEEN, OP_BETWEEN, "Trong khoảng");
            case "not_between" -> OperatorResponse.of("not_between", "not between", "Ngoài khoảng");
            case OP_CONTAINS -> OperatorResponse.of(OP_CONTAINS, OP_CONTAINS, "Chứa");
            case "not_contains" -> OperatorResponse.of("not_contains", "does not contain", "Không chứa");
            case "starts_with" -> OperatorResponse.of("starts_with", "starts with", "Bắt đầu bằng");
            case "ends_with" -> OperatorResponse.of("ends_with", "ends with", "Kết thúc bằng");
            case OP_IS_TRUE -> OperatorResponse.of(OP_IS_TRUE, "is true", "Đúng");
            case OP_IS_FALSE -> OperatorResponse.of(OP_IS_FALSE, "is false", "Sai");
            case "is" -> OperatorResponse.of("is", "is", "Là");
            case "is_not" -> OperatorResponse.of("is_not", "is not", "Không là");
            case "is_any" -> OperatorResponse.of("is_any", OP_LABEL_IS_ANY_OF, "Là một trong");
            case "is_none" -> OperatorResponse.of("is_none", OP_LABEL_IS_NONE_OF, "Không là bất kỳ");
            default -> OperatorResponse.of(comparator, comparator, comparator);
        };
    }
}
