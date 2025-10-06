package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.mapper;

import org.mapstruct.*;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.TemporalPolicyEntity;
import vn.viettel.vds.promotion.validation.domain.model.TemporalPolicy;

/**
 * MapStruct mapper for converting between TemporalPolicy domain model and TemporalPolicyEntity.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TemporalPolicyMapper {

    TemporalPolicy toDomain(TemporalPolicyEntity entity);

    TemporalPolicyEntity toEntity(TemporalPolicy domain);

    /**
     * Update entity from domain model.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    void updateEntityFromDomain(TemporalPolicy domain, @MappingTarget TemporalPolicyEntity entity);
}
