package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.mapper;

import org.mapstruct.*;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.OperatorEntity;
import vn.viettel.vds.promotion.validation.domain.model.Operator;

/**
 * MapStruct mapper for converting between Operator domain model and OperatorEntity.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OperatorMapper {

    Operator toDomain(OperatorEntity entity);

    OperatorEntity toEntity(Operator domain);

    /**
     * Update entity from domain model.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    void updateEntityFromDomain(Operator domain, @MappingTarget OperatorEntity entity);
}
