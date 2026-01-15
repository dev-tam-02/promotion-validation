package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.mapper;

import org.mapstruct.*;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.AssignmentApplicabilityRuleEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.AssignmentEntity;
import vn.viettel.vds.promotion.validation.domain.model.ApplicabilityRule;

import java.util.List;

/**
 * MapStruct mapper for converting between ApplicabilityRule domain model and AssignmentApplicabilityRuleEntity.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ApplicabilityRuleMapper {

    @Mapping(source = "assignment.id", target = "assignmentId")
    @Mapping(source = "ruleType", target = "ruleType", qualifiedByName = "stringToRuleType")
    @Mapping(source = "objectType", target = "objectType", qualifiedByName = "stringToObjectType")
    @Mapping(source = "effect", target = "effect", qualifiedByName = "stringToEffectType")
    @Mapping(source = "target", target = "target", qualifiedByName = "stringToTargetType")
    ApplicabilityRule toDomain(AssignmentApplicabilityRuleEntity entity);

    List<ApplicabilityRule> toDomainList(List<AssignmentApplicabilityRuleEntity> entities);

    @Mapping(target = "assignment", ignore = true)
    @Mapping(source = "ruleType", target = "ruleType", qualifiedByName = "ruleTypeToString")
    @Mapping(source = "objectType", target = "objectType", qualifiedByName = "objectTypeToString")
    @Mapping(source = "effect", target = "effect", qualifiedByName = "effectTypeToString")
    @Mapping(source = "target", target = "target", qualifiedByName = "targetTypeToString")
    AssignmentApplicabilityRuleEntity toEntity(ApplicabilityRule domain);

    List<AssignmentApplicabilityRuleEntity> toEntityList(List<ApplicabilityRule> domains);

    @Named("stringToRuleType")
    default ApplicabilityRule.RuleType stringToRuleType(String value) {
        if (value == null) return null;
        try {
            return ApplicabilityRule.RuleType.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Named("ruleTypeToString")
    default String ruleTypeToString(ApplicabilityRule.RuleType value) {
        return value != null ? value.name() : null;
    }

    @Named("stringToObjectType")
    default ApplicabilityRule.ObjectType stringToObjectType(String value) {
        if (value == null) return null;
        try {
            return ApplicabilityRule.ObjectType.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Named("objectTypeToString")
    default String objectTypeToString(ApplicabilityRule.ObjectType value) {
        return value != null ? value.name() : null;
    }

    @Named("stringToEffectType")
    default ApplicabilityRule.EffectType stringToEffectType(String value) {
        if (value == null) return null;
        try {
            return ApplicabilityRule.EffectType.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Named("effectTypeToString")
    default String effectTypeToString(ApplicabilityRule.EffectType value) {
        return value != null ? value.name() : null;
    }

    @Named("stringToTargetType")
    default ApplicabilityRule.TargetType stringToTargetType(String value) {
        if (value == null) return null;
        try {
            return ApplicabilityRule.TargetType.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Named("targetTypeToString")
    default String targetTypeToString(ApplicabilityRule.TargetType value) {
        return value != null ? value.name() : null;
    }

    /**
     * Helper method to set assignment by ID.
     */
    @AfterMapping
    default void setAssignmentById(@MappingTarget AssignmentApplicabilityRuleEntity entity, ApplicabilityRule domain) {
        if (domain.getAssignmentId() != null) {
            AssignmentEntity assignment = new AssignmentEntity();
            assignment.setId(domain.getAssignmentId());
            entity.setAssignment(assignment);
        }
    }
}
