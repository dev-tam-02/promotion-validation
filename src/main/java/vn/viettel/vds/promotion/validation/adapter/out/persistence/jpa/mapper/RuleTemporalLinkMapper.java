package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.mapper;

import org.mapstruct.*;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.RuleTemporalLinkEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.TemporalPolicyEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.ValidationRuleEntity;
import vn.viettel.vds.promotion.validation.domain.model.RuleTemporalLink;

/**
 * MapStruct mapper for converting between RuleTemporalLink domain model and RuleTemporalLinkEntity.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RuleTemporalLinkMapper {

    @Mapping(source = "validationRule.id", target = "validationRuleId")
    @Mapping(source = "temporalPolicy.id", target = "temporalPolicyId")
    RuleTemporalLink toDomain(RuleTemporalLinkEntity entity);

    @Mapping(target = "validationRule", ignore = true)
    @Mapping(target = "temporalPolicy", ignore = true)
    RuleTemporalLinkEntity toEntity(RuleTemporalLink domain);

    /**
     * Update entity from domain model, ignoring relationships.
     */
    @Mapping(target = "validationRule", ignore = true)
    @Mapping(target = "temporalPolicy", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    void updateEntityFromDomain(RuleTemporalLink domain, @MappingTarget RuleTemporalLinkEntity entity);

    /**
     * Helper method to set validation rule by ID.
     */
    @AfterMapping
    default void setValidationRuleById(@MappingTarget RuleTemporalLinkEntity entity, RuleTemporalLink domain) {
        if (domain.getValidationRuleId() != null) {
            ValidationRuleEntity rule = new ValidationRuleEntity();
            rule.setId(domain.getValidationRuleId());
            entity.setValidationRule(rule);
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
