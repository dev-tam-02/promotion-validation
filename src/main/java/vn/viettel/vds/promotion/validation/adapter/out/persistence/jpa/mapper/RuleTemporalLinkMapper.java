package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.mapper;

import org.mapstruct.*;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.AssignmentEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.RuleTemporalLinkEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.TemporalPolicyEntity;
import vn.viettel.vds.promotion.validation.domain.model.RuleTemporalLink;

/**
 * MapStruct mapper for converting between RuleTemporalLink domain model and RuleTemporalLinkEntity.
 * FIXED: Changed to map assignment instead of validation rule
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RuleTemporalLinkMapper {

    @Mapping(source = "assignment.id", target = "assignmentId")  // FIXED: was validationRule.id
    @Mapping(source = "temporalPolicy.id", target = "temporalPolicyId")
    RuleTemporalLink toDomain(RuleTemporalLinkEntity entity);

    @Mapping(target = "assignment", ignore = true)  // FIXED: was validationRule
    @Mapping(target = "temporalPolicy", ignore = true)
    RuleTemporalLinkEntity toEntity(RuleTemporalLink domain);

    /**
     * Update entity from domain model, ignoring relationships.
     */
    @Mapping(target = "assignment", ignore = true)  // FIXED: was validationRule
    @Mapping(target = "temporalPolicy", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    void updateEntityFromDomain(RuleTemporalLink domain, @MappingTarget RuleTemporalLinkEntity entity);

    /**
     * Helper method to set assignment by ID.
     * FIXED: Changed from setValidationRuleById to setAssignmentById
     */
    @AfterMapping
    default void setAssignmentById(@MappingTarget RuleTemporalLinkEntity entity, RuleTemporalLink domain) {
        if (domain.getAssignmentId() != null) {
            AssignmentEntity assignment = new AssignmentEntity();
            assignment.setId(domain.getAssignmentId());
            entity.setAssignment(assignment);
        }
    }

    /**
     * Helper method to set temporal policy by ID.
     */
    @AfterMapping
    default void setTemporalPolicyById(@MappingTarget RuleTemporalLinkEntity entity, RuleTemporalLink domain) {
        if (domain.getTemporalPolicyId() != null) {
            TemporalPolicyEntity policy = new TemporalPolicyEntity();
            policy.setId(domain.getTemporalPolicyId());
            entity.setTemporalPolicy(policy);
        }
    }
}
