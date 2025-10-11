package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.ReasonCodeEntity;
import vn.viettel.vds.promotion.validation.domain.model.ReasonCode;

/**
 * MapStruct mapper for converting between ReasonCode domain model and ReasonCodeEntity.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ReasonCodeMapper {

    ReasonCode toDomain(ReasonCodeEntity entity);

    ReasonCodeEntity toEntity(ReasonCode domain);

    /**
     * Update entity from domain model.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    void updateEntityFromDomain(ReasonCode domain, @MappingTarget ReasonCodeEntity entity);
}
