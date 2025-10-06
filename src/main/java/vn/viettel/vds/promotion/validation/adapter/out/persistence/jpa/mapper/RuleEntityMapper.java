package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.RuleJpaEntity;
import vn.viettel.vds.promotion.validation.domain.entity.Rule;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface RuleEntityMapper {

    @Mapping(target = "nodes", ignore = true) // Complex nested structure - not stored in JPA
    @Mapping(target = "limits", source = "configuration", qualifiedByName = "stringMapToObjectMap")
    @Mapping(target = "state", source = "state", qualifiedByName = "stringToRuleState")
    @Mapping(target = "logic", source = "logic", qualifiedByName = "stringToLogicType")
    Rule toDomain(RuleJpaEntity entity);

    @Mapping(target = "configuration", source = "limits", qualifiedByName = "objectMapToStringMap")
    @Mapping(target = "state", source = "state", qualifiedByName = "ruleStateToString")
    @Mapping(target = "logic", source = "logic", qualifiedByName = "logicTypeToString")
    @Mapping(target = "version", ignore = true) // JPA version is different from domain latestVersion
    @Mapping(target = "ruleCode", source = "code") // Map code to ruleCode
    @Mapping(target = "description", source = "notes") // Map notes to description
    @Mapping(target = "expression", constant = "") // Default to empty string
    @Mapping(target = "active", expression = "java(domain.getState() == vn.viettel.vds.promotion.validation.domain.entity.Rule.RuleState.PUBLISHED)")
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
