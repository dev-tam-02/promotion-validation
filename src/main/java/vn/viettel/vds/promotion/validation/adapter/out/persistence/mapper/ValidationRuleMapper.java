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
public abstract class ValidationRuleMapper {

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
    @Mapping(source = "targetSegments", target = "targetSegments")
    @Mapping(source = "expression", target = "expression")
    @Mapping(source = "effectiveFrom", target = "effectiveFrom")
    @Mapping(source = "effectiveTo", target = "effectiveTo")
    @Mapping(target = "configuration", ignore = true)
    @Mapping(source = "active", target = "active")
    public abstract ValidationRule toDomain(Rule rule);

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
    @Mapping(source = "targetSegments", target = "targetSegments")
    @Mapping(target = "expression", ignore = true)
    @Mapping(target = "configuration", ignore = true)
    @Mapping(target = "effectiveFrom", ignore = true)
    @Mapping(target = "effectiveTo", ignore = true)
    @Mapping(target = "active", expression = "java(\"PUBLISHED\".equals(entity.getState()))")
    public abstract ValidationRule jpaEntityToDomain(RuleJpaEntity entity);

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
    @Mapping(source = "targetSegments", target = "targetSegments")
    @Mapping(source = "effectiveFrom", target = "effectiveFrom")
    @Mapping(source = "effectiveTo", target = "effectiveTo")
    @Mapping(source = "expression", target = "expression")
    @Mapping(source = "active", target = "active")
    @Mapping(source = "limits", target = "limits", qualifiedByName = "validationUsageLimitsToRuleLimits")
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "state", expression = "java(validationRule.isActive() ? vn.viettel.vds.promotion.validation.domain.model.Rule.RuleState.PUBLISHED : vn.viettel.vds.promotion.validation.domain.model.Rule.RuleState.DRAFT)")
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
    @Mapping(target = "targetSegmentsList", ignore = true)
    @Mapping(target = "nodes", ignore = true)
    public abstract Rule toEntity(ValidationRule validationRule);

    /**
     * Convert ValidationRule.UsageLimits to Rule.UsageLimits
     */
    @Named("validationUsageLimitsToRuleLimits")
    protected Rule.UsageLimits validationUsageLimitsToRuleLimits(ValidationRule.UsageLimits limits) {
        if (limits == null) {
            return null;
        }
        return Rule.UsageLimits.builder()
            .perCodeTotal(limits.getPerCodeTotal())
            .perCustomer(limits.getPerCustomer())
            .perDay(limits.getPerDay())
            .build();
    }

    /**
     * Convert Rule.UsageLimits to ValidationRule.UsageLimits
     */
    @Named("ruleUsageLimitsToValidationLimits")
    protected ValidationRule.UsageLimits ruleUsageLimitsToValidationLimits(Rule.UsageLimits limits) {
        if (limits == null) {
            return null;
        }
        return new ValidationRule.UsageLimits(
            limits.getPerCodeTotal(),
            limits.getPerCustomer(),
            limits.getPerDay()
        );
    }
}
