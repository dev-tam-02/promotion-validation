package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.mapper;

import org.mapstruct.*;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.RuleJpaEntity;
import vn.viettel.vds.promotion.validation.domain.model.Rule;

/**
 * MapStruct mapper for converting between Rule domain model and RuleJpaEntity.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RuleMapper {

    Rule toDomain(RuleJpaEntity entity);

    RuleJpaEntity toEntity(Rule domain);

    /**
     * Update entity from domain model.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    void updateEntityFromDomain(Rule domain, @MappingTarget RuleJpaEntity entity);
}
