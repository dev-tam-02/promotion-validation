package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.RuleBindingEntity;
import vn.viettel.vds.promotion.validation.domain.model.RuleBinding;

import java.util.Collections;
import java.util.List;

/**
 * MapStruct mapper for converting between RuleBindingEntity (persistence) and RuleBinding (domain).
 * <p>
 * Handles JSON serialization/deserialization for list fields:
 * - timeWindows: JSON array of TimeWindow objects
 * - excludedDates: JSON array of date strings
 * - includedProducts, excludedProducts, includedCategories, etc.: JSON arrays of strings
 */
@Mapper(componentModel = "spring")
public interface RuleBindingMapper {

    Logger LOGGER = LoggerFactory.getLogger(RuleBindingMapper.class);
    ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // ========== Entity to Domain ==========

    /**
     * Convert JSON string to List of TimeWindow
     */
    @Named("jsonToTimeWindows")
    static List<RuleBinding.TimeWindow> jsonToTimeWindows(String json) {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<List<RuleBinding.TimeWindow>>() {
            });
        } catch (JsonProcessingException e) {
            LOGGER.warn("Failed to parse timeWindows JSON: {}", json, e);
            return Collections.emptyList();
        }
    }

    /**
     * Convert List of TimeWindow to JSON string
     */
    @Named("timeWindowsToJson")
    static String timeWindowsToJson(List<RuleBinding.TimeWindow> timeWindows) {
        if (timeWindows == null || timeWindows.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(timeWindows);
        } catch (JsonProcessingException e) {
            LOGGER.warn("Failed to serialize timeWindows to JSON", e);
            return null;
        }
    }

    // ========== Domain to Entity ==========

    /**
     * Convert JSON string to List of String
     */
    @Named("jsonToStringList")
    static List<String> jsonToStringList(String json) {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (JsonProcessingException e) {
            LOGGER.warn("Failed to parse string list JSON: {}", json, e);
            return Collections.emptyList();
        }
    }

    /**
     * Convert List of String to JSON string
     */
    @Named("stringListToJson")
    static String stringListToJson(List<String> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            LOGGER.warn("Failed to serialize string list to JSON", e);
            return null;
        }
    }

    // ========== JSON Conversion Methods ==========

    /**
     * Convert String to StickyKeyStrategy enum
     */
    @Named("stringToStickyKeyStrategy")
    static RuleBinding.StickyKeyStrategy stringToStickyKeyStrategy(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return RuleBinding.StickyKeyStrategy.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Invalid StickyKeyStrategy value: {}", value);
            return null;
        }
    }

    /**
     * Convert StickyKeyStrategy enum to String
     */
    @Named("stickyKeyStrategyToString")
    static String stickyKeyStrategyToString(RuleBinding.StickyKeyStrategy strategy) {
        return strategy != null ? strategy.name() : null;
    }

    @Mapping(target = "timeWindows", source = "timeWindows", qualifiedByName = "jsonToTimeWindows")
    @Mapping(target = "excludedDates", source = "excludedDates", qualifiedByName = "jsonToStringList")
    @Mapping(target = "includedProducts", source = "includedProducts", qualifiedByName = "jsonToStringList")
    @Mapping(target = "excludedProducts", source = "excludedProducts", qualifiedByName = "jsonToStringList")
    @Mapping(target = "includedCategories", source = "includedCategories", qualifiedByName = "jsonToStringList")
    @Mapping(target = "excludedCategories", source = "excludedCategories", qualifiedByName = "jsonToStringList")
    @Mapping(target = "includedBrands", source = "includedBrands", qualifiedByName = "jsonToStringList")
    @Mapping(target = "excludedBrands", source = "excludedBrands", qualifiedByName = "jsonToStringList")
    @Mapping(target = "stickyKeyStrategy", source = "stickyKeyStrategy", qualifiedByName = "stringToStickyKeyStrategy")
    RuleBinding toDomain(RuleBindingEntity entity);

    List<RuleBinding> toDomainList(List<RuleBindingEntity> entities);

    @Mapping(target = "timeWindows", source = "timeWindows", qualifiedByName = "timeWindowsToJson")
    @Mapping(target = "excludedDates", source = "excludedDates", qualifiedByName = "stringListToJson")
    @Mapping(target = "includedProducts", source = "includedProducts", qualifiedByName = "stringListToJson")
    @Mapping(target = "excludedProducts", source = "excludedProducts", qualifiedByName = "stringListToJson")
    @Mapping(target = "includedCategories", source = "includedCategories", qualifiedByName = "stringListToJson")
    @Mapping(target = "excludedCategories", source = "excludedCategories", qualifiedByName = "stringListToJson")
    @Mapping(target = "includedBrands", source = "includedBrands", qualifiedByName = "stringListToJson")
    @Mapping(target = "excludedBrands", source = "excludedBrands", qualifiedByName = "stringListToJson")
    @Mapping(target = "stickyKeyStrategy", source = "stickyKeyStrategy", qualifiedByName = "stickyKeyStrategyToString")
    RuleBindingEntity toEntity(RuleBinding domain);

    List<RuleBindingEntity> toEntityList(List<RuleBinding> domains);
}
