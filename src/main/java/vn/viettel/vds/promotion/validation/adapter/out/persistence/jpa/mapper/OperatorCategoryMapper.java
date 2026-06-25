package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.OperatorCategoryEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.OperatorOptionEntity;
import vn.viettel.vds.promotion.validation.domain.model.OperatorCategory;
import vn.viettel.vds.promotion.validation.domain.model.OperatorOption;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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
        // The JOIN FETCH that loads this collection does not order it (findAllWithOptions
        // orders only the category root), so sort by displayOrder here to honour the
        // configured operator ordering per category. Null displayOrder sinks to the end.
        return entities.stream()
                .filter(OperatorOptionEntity::getActive)
                .sorted(Comparator.comparing(OperatorOptionEntity::getDisplayOrder,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(entity -> mapOption(entity, objectMapper))
                .toList();
    }

    default OperatorOption mapOption(OperatorOptionEntity entity, ObjectMapper objectMapper) {
        return OperatorOption.builder()
                .id(entity.getId())
                .categoryId(entity.getCategory() != null ? entity.getCategory().getId() : null)
                .code(entity.getCode())
                // name / description / label / placeholder (all locales) are overlaid from
                // the translations table in OperatorCategoryJpaAdapter (changelog 082) —
                // the inline operator_options i18n columns were dropped.
                .displayOrder(entity.getDisplayOrder())
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
                // Input configuration
                .dataSourceType(entity.getDataSourceType())
                .dataSourceEndpoint(entity.getDataSourceEndpoint())
                .dataLoaderType(entity.getDataLoaderType())
                .dataLoaderConfig(entity.getDataLoaderConfig())
                .appliesToMetadataSchema(entity.getAppliesToMetadataSchema())
                .inputType(entity.getInputType())
                .inputMultiple(entity.getInputMultiple())
                .inputSearchable(entity.getInputSearchable())
                .inputStep(entity.getInputStep())
                .valueParamKey(entity.getValueParamKey())
                // Auto-apply configuration
                .autoApply(entity.getAutoApply())
                .defaultOperator(entity.getDefaultOperator())
                .active(entity.getActive())
                .paramsSchema(entity.getParamsSchema())
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
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }

    /**
     * Parse value_options JSON column into domain ValueOption list.
     * Supports two label shapes stored in the DB:
     * <ul>
     *   <li>Object: {@code {"label": {"en": "Paid", "vi": "Trả phí"}}} — full i18n</li>
     *   <li>String (legacy): {@code {"label": "Paid"}} — same text for EN and VI</li>
     * </ul>
     * Defensive: if a language key is missing, falls back to the other language.
     */
    default List<OperatorOption.ValueOption> parseValueOptions(String json, ObjectMapper objectMapper) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            List<java.util.Map<String, Object>> options = objectMapper.readValue(json, new TypeReference<>() {
            });
            return options.stream()
                    .map(OperatorCategoryMapper::toValueOption)
                    .toList();
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }

    @SuppressWarnings("deprecation") // BC: legacy label setter intentionally populated with EN text
    private static OperatorOption.ValueOption toValueOption(java.util.Map<String, Object> opt) {
        String value = (String) opt.get("value");
        String[] labels = extractLabels(opt.get("label"));
        return OperatorOption.ValueOption.builder()
                .value(value)
                .label(labels[0])
                .labelEn(labels[0])
                .labelVi(labels[1])
                .build();
    }

    @SuppressWarnings("unchecked")
    private static String[] extractLabels(Object labelNode) {
        if (labelNode instanceof java.util.Map<?, ?> labelMap) {
            java.util.Map<String, Object> typed = (java.util.Map<String, Object>) labelMap;
            String en = typed.get("en") != null ? String.valueOf(typed.get("en")) : null;
            String vi = typed.get("vi") != null ? String.valueOf(typed.get("vi")) : null;
            if (en == null) en = vi;
            if (vi == null) vi = en;
            return new String[]{en, vi};
        }
        if (labelNode instanceof String s) {
            return new String[]{s, s};
        }
        return new String[]{null, null};
    }
}
