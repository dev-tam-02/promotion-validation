package vn.viettel.vds.promotion.validation.adapter.out.persistence.mapper;

import org.mapstruct.*;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.RuleJpaEntity;
import vn.viettel.vds.promotion.validation.domain.model.Rule;
import vn.viettel.vds.promotion.validation.domain.model.ValidationRule;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Mapper between Rule entities and ValidationRule domain model
 * Converts MongoDB Rule and JPA RuleJpaEntity to ValidationRule
 */
@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ValidationRuleMapper {

    /**
     * Convert Rule domain model to ValidationRule domain model
     */
    @Mapping(source = "id", target = "ruleId")
    @Mapping(source = "ruleCode", target = "ruleCode")
    @Mapping(source = "name", target = "name")
    @Mapping(source = "description", target = "description")
    @Mapping(source = "type", target = "type")
    @Mapping(source = "priority", target = "priority")
    @Mapping(source = "createdAt", target = "createdAt")
    @Mapping(source = "updatedAt", target = "updatedAt")
    @Mapping(source = "targetSegments", target = "targetSegments", qualifiedByName = "listToSet")
    @Mapping(source = "expression", target = "expression")
    @Mapping(target = "configuration", ignore = true)
    @Mapping(target = "effectiveFrom", ignore = true)
    @Mapping(target = "effectiveTo", ignore = true)
    @Mapping(source = "active", target = "active")
    ValidationRule toDomain(Rule rule);

    /**
     * Convert JPA RuleJpaEntity to ValidationRule domain model
     */
    @Mapping(source = "id", target = "ruleId")
    @Mapping(source = "code", target = "ruleCode")
    @Mapping(source = "name", target = "name")
    @Mapping(source = "notes", target = "description")
    @Mapping(source = "type", target = "type")
    @Mapping(source = "priority", target = "priority")
    @Mapping(source = "createdAt", target = "createdAt")
    @Mapping(source = "updatedAt", target = "updatedAt")
    @Mapping(source = "targetSegments", target = "targetSegments", qualifiedByName = "listToSet")
    @Mapping(target = "expression", ignore = true)
    @Mapping(target = "configuration", ignore = true)
    @Mapping(target = "effectiveFrom", ignore = true)
    @Mapping(target = "effectiveTo", ignore = true)
    @Mapping(target = "active", expression = "java(\"PUBLISHED\".equals(entity.getState()))")
    ValidationRule jpaEntityToDomain(RuleJpaEntity entity);

    /**
     * Convert ValidationRule domain model to Rule domain model
     */
    @Mapping(source = "ruleId", target = "id")
    @Mapping(source = "ruleCode", target = "ruleCode")
    @Mapping(source = "name", target = "name")
    @Mapping(source = "description", target = "description")
    @Mapping(source = "type", target = "type")
    @Mapping(source = "priority", target = "priority")
    @Mapping(source = "createdAt", target = "createdAt")
    @Mapping(source = "updatedAt", target = "updatedAt")
    @Mapping(source = "targetSegments", target = "targetSegments", qualifiedByName = "setToList")
    @Mapping(source = "expression", target = "expression")
    @Mapping(source = "active", target = "active")
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "state", expression = "java(validationRule.isActive() ? \"PUBLISHED\" : \"DRAFT\")")
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "notes", ignore = true)
    @Mapping(target = "latestVersion", ignore = true)
    @Mapping(target = "logic", ignore = true)
    @Mapping(target = "dsl", ignore = true)
    @Mapping(target = "ruleVersion", ignore = true)
    @Mapping(target = "publishedAt", ignore = true)
    @Mapping(target = "publishedBy", ignore = true)
    @Mapping(target = "configuration", ignore = true)
    @Mapping(target = "ruleSetId", ignore = true)
    @Mapping(target = "campaignId", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    Rule toEntity(ValidationRule validationRule);

    /**
     * Convert List to Set for target segments
     */
    @Named("listToSet")
    default Set<String> listToSet(List<String> list) {
        return list != null ? new HashSet<>(list) : new HashSet<>();
    }

    /**
     * Convert Set to List for target segments
     */
    @Named("setToList")
    default List<String> setToList(Set<String> set) {
        return set != null ? List.copyOf(set) : List.of();
    }
}
