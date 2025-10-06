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
 *   <li><strong>ruleVersion, publishedAt, publishedBy</strong>: These fields exist in the domain model
 *       but are not persisted in the RuleJpaEntity table. They are managed separately in a rule_versions
 *       table or versioning system.</li>
 *   <li><strong>nodes</strong>: Rule node tree structure is stored in a separate rule_nodes table and
 *       loaded via dedicated services, not through this basic entity mapping.</li>
 *   <li><strong>limits</strong>: Dynamic limits are calculated from configuration or stored elsewhere,
 *       not in the main rules table.</li>
 *   <li><strong>version (JPA)</strong>: The JPA @Version field for optimistic locking is different from
 *       the domain's latestVersion field which tracks business version numbers.</li>
 * </ul>
 */
@Mapper(componentModel = "spring")
public interface RuleEntityMapper {

    @Mapping(target = "dsl", source = "configuration", qualifiedByName = "stringMapToObjectMap")
    @Mapping(target = "state", source = "state", qualifiedByName = "stringToRuleState")
    @Mapping(target = "logic", source = "logic", qualifiedByName = "stringToLogicType")
    @Mapping(target = "ruleVersion", ignore = true)  // Stored in separate rule_versions table
    @Mapping(target = "publishedAt", ignore = true)  // Stored in separate rule_versions table
    @Mapping(target = "publishedBy", ignore = true)  // Stored in separate rule_versions table
    @Mapping(target = "nodes", ignore = true)        // Loaded separately via rule_nodes relationship
    @Mapping(target = "limits", ignore = true)       // Calculated dynamically from configuration
    Rule toDomain(RuleJpaEntity entity);

    @Mapping(target = "configuration", source = "dsl", qualifiedByName = "objectMapToStringMap")
    @Mapping(target = "state", source = "state", qualifiedByName = "ruleStateToString")
    @Mapping(target = "logic", source = "logic", qualifiedByName = "logicTypeToString")
    @Mapping(target = "version", ignore = true) // JPA version is different from domain latestVersion
    @Mapping(target = "active", expression = "java(domain.getState() == vn.viettel.vds.promotion.validation.domain.model.Rule.RuleState.PUBLISHED)")
    RuleJpaEntity toEntity(Rule domain);

    List<Rule> toDomainList(List<RuleJpaEntity> entities);

    List<RuleJpaEntity> toEntityList(List<Rule> domains);

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
