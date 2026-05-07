package vn.viettel.vds.promotion.validation.adapter.in.web.dto;

import java.util.List;

/**
 * Response DTO for operator category.
 */
public record OperatorCategoryResponse(
        String id,
        String code,
        String name,
        Integer displayOrder,
        String icon,
        String description,
        boolean isMetadataCategory,
        String metadataSchemaType,
        List<OperatorOptionResponse> options
) {
    /**
     * Create response from domain model.
     */
    public static OperatorCategoryResponse from(vn.viettel.vds.promotion.validation.domain.model.OperatorCategory category) {
        List<OperatorOptionResponse> optionResponses = category.getOptions() != null
                ? category.getOptions().stream()
                .map(OperatorOptionResponse::from)
                .toList()
                : List.of();

        return new OperatorCategoryResponse(
                category.getId(),
                category.getCode(),
                category.getName(),
                category.getDisplayOrder(),
                category.getIcon(),
                category.getDescription(),
                category.isMetadataCategory(),
                category.getMetadataSchemaType(),
                optionResponses
        );
    }
}
