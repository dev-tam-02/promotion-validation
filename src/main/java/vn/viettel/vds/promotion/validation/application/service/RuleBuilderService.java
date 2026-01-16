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
        return RuleItemResponse.builder()
                .id(option.getCode())
                .code(option.getCode().toUpperCase())
                .name(option.getName(), option.getNameVi() != null ? option.getNameVi() : option.getName())
                .description(
                        option.getDescription() != null ? option.getDescription() : "",
                        option.getDescriptionVi() != null ? option.getDescriptionVi() : (option.getDescription() != null ? option.getDescription() : "")
                )
                .type(mapValueTypeToRuleType(option.getValueType(), option.getDataSourceType()))
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
        // Skip input config for boolean types
        if (option.getValueType() == OperatorOption.ValueType.BOOLEAN) {
            return null;
        }

        // Skip if no input configuration
        if (option.getInputType() == null && option.getDataSourceType() == null && option.getValueSource() == null) {
            return null;
        }

        RuleInputConfigResponse.Builder builder = RuleInputConfigResponse.builder();

        // Data source configuration
        if (option.getDataSourceType() != null) {
            builder.dataSourceType(option.getDataSourceType());
        }
        if (option.getDataSourceEndpoint() != null) {
            builder.dataSourceEndpoint(option.getDataSourceEndpoint());
        }

        // Input type
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

        // Multiple and searchable
        if (option.getInputMultiple() != null) {
            builder.multiple(option.getInputMultiple());
        }
        if (option.getInputSearchable() != null) {
            builder.searchable(option.getInputSearchable());
        }

        // Labels
        if (option.getLabelEn() != null || option.getLabelVi() != null) {
            builder.label(
                    option.getLabelEn() != null ? option.getLabelEn() : option.getName(),
                    option.getLabelVi() != null ? option.getLabelVi() : (option.getNameVi() != null ? option.getNameVi() : option.getName())
            );
        }

        // Placeholders
        if (option.getPlaceholderEn() != null || option.getPlaceholderVi() != null) {
            builder.placeholder(
                    option.getPlaceholderEn() != null ? option.getPlaceholderEn() : "",
                    option.getPlaceholderVi() != null ? option.getPlaceholderVi() : ""
            );
        }

        // Number validation
        if (option.getMinValue() != null) {
            builder.minValue(option.getMinValue().toString());
        }
        if (option.getMaxValue() != null) {
            builder.maxValue(option.getMaxValue().toString());
        }
        if (option.getInputStep() != null) {
            builder.step(option.getInputStep());
        }

        return builder.build();
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
                    OperatorResponse.of("equals", "equals", "Bằng"),
                    OperatorResponse.of("not_equals", "not equals", "Không bằng")
            );
        }

        return switch (comparisonType) {
            case RANGE -> List.of(
                    OperatorResponse.of("equals", "equals", "Bằng"),
                    OperatorResponse.of("not_equals", "not equals", "Không bằng"),
                    OperatorResponse.of("gte", "greater than or equal", "Lớn hơn hoặc bằng"),
                    OperatorResponse.of("lte", "less than or equal", "Nhỏ hơn hoặc bằng"),
                    OperatorResponse.of("between", "between", "Trong khoảng")
            );
            case LIST -> List.of(
                    OperatorResponse.of("in", "is any of", "Thuộc một trong"),
                    OperatorResponse.of("not_in", "is none of", "Không thuộc bất kỳ")
            );
            case BOOLEAN -> List.of(
                    OperatorResponse.of("is_true", "is true", "Đúng"),
                    OperatorResponse.of("is_false", "is false", "Sai")
            );
            default -> List.of(
                    OperatorResponse.of("equals", "equals", "Bằng"),
                    OperatorResponse.of("not_equals", "not equals", "Không bằng")
            );
        };
    }

    /**
     * Map comparator string to OperatorResponse.
     */
    private OperatorResponse mapComparatorToOperator(String comparator) {
        return switch (comparator.toLowerCase()) {
            case "equals" -> OperatorResponse.of("equals", "equals", "Bằng");
            case "not_equals" -> OperatorResponse.of("not_equals", "not equals", "Không bằng");
            case "in" -> OperatorResponse.of("in", "is any of", "Thuộc một trong");
            case "not_in" -> OperatorResponse.of("not_in", "is none of", "Không thuộc bất kỳ");
            case "gte", "greater_than_or_equal" -> OperatorResponse.of("gte", "greater than or equal", "Lớn hơn hoặc bằng");
            case "gt", "greater_than" -> OperatorResponse.of("gt", "greater than", "Lớn hơn");
            case "lte", "less_than_or_equal" -> OperatorResponse.of("lte", "less than or equal", "Nhỏ hơn hoặc bằng");
            case "lt", "less_than" -> OperatorResponse.of("lt", "less than", "Nhỏ hơn");
            case "between" -> OperatorResponse.of("between", "between", "Trong khoảng");
            case "not_between" -> OperatorResponse.of("not_between", "not between", "Ngoài khoảng");
            case "contains" -> OperatorResponse.of("contains", "contains", "Chứa");
            case "not_contains" -> OperatorResponse.of("not_contains", "does not contain", "Không chứa");
            case "starts_with" -> OperatorResponse.of("starts_with", "starts with", "Bắt đầu bằng");
            case "ends_with" -> OperatorResponse.of("ends_with", "ends with", "Kết thúc bằng");
            case "is_true" -> OperatorResponse.of("is_true", "is true", "Đúng");
            case "is_false" -> OperatorResponse.of("is_false", "is false", "Sai");
            case "is" -> OperatorResponse.of("is", "is", "Là");
            case "is_not" -> OperatorResponse.of("is_not", "is not", "Không là");
            case "is_any" -> OperatorResponse.of("is_any", "is any of", "Là một trong");
            case "is_none" -> OperatorResponse.of("is_none", "is none of", "Không là bất kỳ");
            default -> OperatorResponse.of(comparator, comparator, comparator);
        };
    }
}
