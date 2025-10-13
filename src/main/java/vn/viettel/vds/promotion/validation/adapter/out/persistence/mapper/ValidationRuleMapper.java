package vn.viettel.vds.promotion.validation.adapter.out.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.RuleJpaEntity;
import vn.viettel.vds.promotion.validation.domain.model.Rule;

/**
 * Mapper between JPA entities and Rule domain model
 */
@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ValidationRuleMapper {

    /**
     * Convert JPA RuleJpaEntity to Rule domain model
     * <p>
     * Note: RuleJpaEntity no longer has notes, type, or priority fields.
     * Only mapping fields that actually exist in the validation_rules table.
     */
    @Mapping(source = "id", target = "id")
    @Mapping(source = "code", target = "code")
    @Mapping(source = "name", target = "name")
    @Mapping(source = "createdAt", target = "createdAt")
    @Mapping(source = "updatedAt", target = "updatedAt")
    @Mapping(source = "targetSegments", target = "targetSegments")
    @Mapping(source = "state", target = "state")
    @Mapping(target = "description", ignore = true)
    @Mapping(target = "type", ignore = true)
    @Mapping(target = "priority", ignore = true)
    @Mapping(target = "expression", ignore = true)
    @Mapping(target = "effectiveFrom", ignore = true)
    @Mapping(target = "effectiveTo", ignore = true)
    @Mapping(target = "dsl", ignore = true)
    Rule jpaEntityToDomain(RuleJpaEntity entity);
}
