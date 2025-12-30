package vn.viettel.vds.promotion.validation.adapter.in.web.dto;

import vn.viettel.vds.promotion.validation.domain.model.OperatorOption;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response DTO for operator option.
 */
public record OperatorOptionResponse(
        String id,
        String categoryId,
        String code,
        String name,
        Integer displayOrder,
        String description,
        String operatorName,
        Integer operatorVersion,
        String comparisonType,
        List<String> availableComparators,
        String defaultComparator,
        String valueType,
        String valueSource,
        List<ValueOptionResponse> valueOptions,
        BigDecimal minValue,
        BigDecimal maxValue,
        String pattern
) {
    /**
     * Create response from domain model.
     */
    public static OperatorOptionResponse from(OperatorOption option) {
        List<ValueOptionResponse> valueOptionResponses = option.getValueOptions() != null
                ? option.getValueOptions().stream()
                        .map(vo -> new ValueOptionResponse(vo.getValue(), vo.getLabel()))
                        .toList()
                : List.of();

        return new OperatorOptionResponse(
                option.getId(),
                option.getCategoryId(),
                option.getCode(),
                option.getName(),
                option.getDisplayOrder(),
                option.getDescription(),
                option.getOperatorName(),
                option.getOperatorVersion(),
                option.getComparisonType() != null ? option.getComparisonType().name().toLowerCase() : null,
                option.getAvailableComparators(),
                option.getDefaultComparator(),
                option.getValueType() != null ? option.getValueType().name().toLowerCase() : null,
                option.getValueSource() != null ? option.getValueSource().name().toLowerCase() : null,
                valueOptionResponses,
                option.getMinValue(),
                option.getMaxValue(),
                option.getPattern()
        );
    }

    /**
     * Nested value option response.
     */
    public record ValueOptionResponse(String value, String label) {}
}
