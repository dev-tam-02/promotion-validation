package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.*;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.OperatorCategoryEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.OperatorOptionEntity;
import vn.viettel.vds.promotion.validation.domain.model.OperatorCategory;
import vn.viettel.vds.promotion.validation.domain.model.OperatorOption;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * MapStruct mapper for converting between OperatorCategory domain model and OperatorCategoryEntity.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OperatorCategoryMapper {

    @Mapping(target = "options", source = "options", qualifiedByName = "mapOptions")
    OperatorCategory toDomain(OperatorCategoryEntity entity);

    @Mapping(target = "options", ignore = true)
    OperatorCategoryEntity toEntity(OperatorCategory domain);

    @Named("mapOptions")
    default List<OperatorOption> mapOptions(List<OperatorOptionEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        ObjectMapper objectMapper = new ObjectMapper();
        return entities.stream()
                .filter(OperatorOptionEntity::getActive)
                .map(entity -> mapOption(entity, objectMapper))
                .collect(Collectors.toList());
    }

    default OperatorOption mapOption(OperatorOptionEntity entity, ObjectMapper objectMapper) {
        return OperatorOption.builder()
                .id(entity.getId())
                .categoryId(entity.getCategory() != null ? entity.getCategory().getId() : null)
                .code(entity.getCode())
                .name(entity.getName())
                .displayOrder(entity.getDisplayOrder())
                .description(entity.getDescription())
                .operatorName(entity.getOperatorName())
                .operatorVersion(entity.getOperatorVersion())
                .comparisonType(parseComparisonType(entity.getComparisonType()))
                .availableComparators(parseJsonList(entity.getAvailableComparators(), objectMapper))
                .defaultComparator(entity.getDefaultComparator())
                .valueType(parseValueType(entity.getValueType()))
                .valueSource(parseValueSource(entity.getValueSource()))
                .valueOptions(parseValueOptions(entity.getValueOptions(), objectMapper))
                .minValue(entity.getMinValue())
                .maxValue(entity.getMaxValue())
                .pattern(entity.getPattern())
                // I18n fields
                .nameVi(entity.getNameVi())
                .descriptionVi(entity.getDescriptionVi())
                // Input configuration
                .dataSourceType(entity.getDataSourceType())
                .dataSourceEndpoint(entity.getDataSourceEndpoint())
                .inputType(entity.getInputType())
                .inputMultiple(entity.getInputMultiple())
                .inputSearchable(entity.getInputSearchable())
                .inputStep(entity.getInputStep())
                .labelEn(entity.getLabelEn())
                .labelVi(entity.getLabelVi())
                .placeholderEn(entity.getPlaceholderEn())
                .placeholderVi(entity.getPlaceholderVi())
                // Auto-apply configuration
                .autoApply(entity.getAutoApply())
                .defaultOperator(entity.getDefaultOperator())
                .active(entity.getActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    default OperatorOption.ComparisonType parseComparisonType(String value) {
        if (value == null) return null;
        try {
            return OperatorOption.ComparisonType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    default OperatorOption.ValueType parseValueType(String value) {
        if (value == null) return null;
        try {
            return OperatorOption.ValueType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    default OperatorOption.ValueSource parseValueSource(String value) {
        if (value == null) return null;
        try {
            return OperatorOption.ValueSource.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    default List<String> parseJsonList(String json, ObjectMapper objectMapper) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }

    default List<OperatorOption.ValueOption> parseValueOptions(String json, ObjectMapper objectMapper) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            List<java.util.Map<String, String>> options = objectMapper.readValue(json, new TypeReference<>() {});
            return options.stream()
                    .map(opt -> OperatorOption.ValueOption.builder()
                            .value(opt.get("value"))
                            .label(opt.get("label"))
                            .build())
                    .collect(Collectors.toList());
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }
}
