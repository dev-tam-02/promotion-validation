package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.RuleJpaEntity;
import vn.viettel.vds.promotion.validation.domain.model.Rule;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * MapStruct mapper for converting between RuleJpaEntity (persistence) and Rule (domain).
 *
 * <p>Mapping Notes:</p>
 * <ul>
 *   <li><strong>ruleVersion, publishedAt, publishedBy</strong>: Now properly mapped - these fields
 *       exist in both the validation_rules table AND the domain model.</li>
 *   <li><strong>dsl</strong>: Maps to domain's 'dsl' field (configuration was removed from entity)</li>
 *   <li><strong>nodes</strong>: Rule node tree structure is stored in a separate rule_nodes table and
 *       loaded via dedicated services, not through this basic entity mapping.</li>
 *   <li><strong>limits</strong>: Dynamic limits are calculated from configuration or stored elsewhere,
 *       not in the main rules table.</li>
 * </ul>
 *
 * <p>Schema Alignment:</p>
 * The entity now correctly matches the validation_rules table schema:
 * - id, code, name, state, rule_version, logic, dsl, published_at, published_by
 * - created_at, updated_at, created_by, updated_by
 * - Plus ElementCollections: rule_configuration, rule_target_segments
 */
@Mapper(componentModel = "spring")
public interface RuleEntityMapper {

    @Mapping(target = "state", source = "state", qualifiedByName = "stringToRuleState")
    @Mapping(target = "logic", source = "logic", qualifiedByName = "stringToLogicType")
    @Mapping(target = "dsl", source = "dsl", qualifiedByName = "stringToObjectMap")
    @Mapping(target = "nodes", ignore = true)        // Loaded separately via rule_nodes relationship
    @Mapping(target = "limits", ignore = true)       // Calculated dynamically from configuration
    @Mapping(target = "tenantId", ignore = true)     // No longer exists in entity
    @Mapping(target = "ruleCode", ignore = true)     // No longer exists in entity
    @Mapping(target = "description", ignore = true)  // No longer exists in entity
    @Mapping(target = "notes", ignore = true)        // No longer exists in entity
    @Mapping(target = "active", ignore = true)       // No longer exists in entity
    @Mapping(target = "latestVersion", ignore = true) // No longer exists in entity
    @Mapping(target = "type", ignore = true)         // No longer exists in entity
    @Mapping(target = "expression", ignore = true)   // No longer exists in entity
    @Mapping(target = "priority", ignore = true)     // No longer exists in entity
    @Mapping(target = "effectiveFrom", ignore = true) // No longer exists in entity
    @Mapping(target = "effectiveTo", ignore = true)  // No longer exists in entity
    @Mapping(target = "targetSegmentsList", ignore = true) // Duplicate of targetSegments
    @Mapping(target = "campaignId", ignore = true)   // No longer exists in entity
    @Mapping(target = "ruleSetId", ignore = true)    // No longer exists in entity
    @Mapping(target = "version", ignore = true)      // No longer exists in entity
    Rule toDomain(RuleJpaEntity entity);

    @Mapping(target = "state", source = "state", qualifiedByName = "ruleStateToString")
    @Mapping(target = "logic", source = "logic", qualifiedByName = "logicTypeToString")
    @Mapping(target = "dsl", source = "dsl", qualifiedByName = "objectMapToString")
    RuleJpaEntity toEntity(Rule domain);

    List<Rule> toDomainList(List<RuleJpaEntity> entities);

    List<RuleJpaEntity> toEntityList(List<Rule> domains);

    /**
     * Convert JSON string (from database TEXT field) to Map<String, Object>
     * Used when loading DSL from database into domain model
     */
    @Named("stringToObjectMap")
    static Map<String, Object> stringToObjectMap(String dslJson) {
        if (dslJson == null || dslJson.trim().isEmpty()) {
            return new HashMap<>();
        }
        // Parse JSON string to Map
        try {
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return objectMapper.readValue(dslJson, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            // If parsing fails, return empty map
            return new HashMap<>();
        }
    }

    /**
     * Convert Map<String, Object> to JSON string (for database TEXT field)
     * Used when persisting DSL from domain model to database
     */
    @Named("objectMapToString")
    static String objectMapToString(Map<String, Object> dslMap) {
        if (dslMap == null || dslMap.isEmpty()) {
            return null;
        }
        // Convert Map to JSON string
        try {
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return objectMapper.writeValueAsString(dslMap);
        } catch (Exception e) {
            // If serialization fails, return null
            return null;
        }
    }

    @Named("stringMapToObjectMap")
    static Map<String, Object> stringMapToObjectMap(Map<String, String> source) {
        if (source == null) {
            return new HashMap<>();
        }
        return new HashMap<>(source);
    }

    @Named("objectMapToStringMap")
    static Map<String, String> objectMapToStringMap(Map<String, Object> source) {
        if (source == null) {
            return new HashMap<>();
        }
        return source.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue() != null ? e.getValue().toString() : ""
                ));
    }

    @Named("stringToRuleState")
    static Rule.RuleState stringToRuleState(String state) {
        if (state == null) {
            return null;
        }
        try {
            return Rule.RuleState.valueOf(state);
        } catch (IllegalArgumentException e) {
            return Rule.RuleState.DRAFT;
        }
    }

    @Named("ruleStateToString")
    static String ruleStateToString(Rule.RuleState state) {
        return state != null ? state.name() : null;
    }

    @Named("stringToLogicType")
    static Rule.LogicType stringToLogicType(String logic) {
        if (logic == null) {
            return null;
        }
        try {
            return Rule.LogicType.valueOf(logic);
        } catch (IllegalArgumentException e) {
            return Rule.LogicType.ALL;
        }
    }

    @Named("logicTypeToString")
    static String logicTypeToString(Rule.LogicType logic) {
        return logic != null ? logic.name() : null;
    }
}
