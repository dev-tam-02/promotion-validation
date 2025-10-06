package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.mapper;

import org.mapstruct.*;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.RuleVersionEntity;
import vn.viettel.vds.promotion.validation.domain.model.RuleVersion;

/**
 * MapStruct mapper for converting between RuleVersion domain model and RuleVersionEntity.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RuleVersionMapper {

    RuleVersion toDomain(RuleVersionEntity entity);

    RuleVersionEntity toEntity(RuleVersion domain);

    /**
     * Update entity from domain model.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    void updateEntityFromDomain(RuleVersion domain, @MappingTarget RuleVersionEntity entity);
}
