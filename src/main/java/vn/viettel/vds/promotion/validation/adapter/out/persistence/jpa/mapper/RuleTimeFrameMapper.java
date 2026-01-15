package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.RuleTimeFrameEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.ValidationRuleEntity;
import vn.viettel.vds.promotion.validation.domain.model.RuleTimeFrame;

import java.util.List;

/**
 * MapStruct mapper for converting between RuleTimeFrame domain model and RuleTimeFrameEntity.
 * <p>
 * NOTE: This is legacy code kept for backward compatibility.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RuleTimeFrameMapper {

    @Mapping(source = "validationRule.id", target = "validationRuleId")
    RuleTimeFrame toDomain(RuleTimeFrameEntity entity);

    List<RuleTimeFrame> toDomainList(List<RuleTimeFrameEntity> entities);

    @Mapping(target = "validationRule", source = "validationRuleId", qualifiedByName = "toValidationRuleEntity")
    RuleTimeFrameEntity toEntity(RuleTimeFrame domain);

    List<RuleTimeFrameEntity> toEntityList(List<RuleTimeFrame> domains);

    @Named("toValidationRuleEntity")
    default ValidationRuleEntity toValidationRuleEntity(String validationRuleId) {
        if (validationRuleId == null) {
            return null;
        }
        ValidationRuleEntity entity = new ValidationRuleEntity();
        entity.setId(validationRuleId);
        return entity;
    }
}
