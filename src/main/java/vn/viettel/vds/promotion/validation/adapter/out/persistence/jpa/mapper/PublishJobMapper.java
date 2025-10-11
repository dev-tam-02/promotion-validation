package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.PublishJobEntity;
import vn.viettel.vds.promotion.validation.domain.model.PublishJob;

/**
 * MapStruct mapper for converting between PublishJob domain model and PublishJobEntity.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PublishJobMapper {

    PublishJob toDomain(PublishJobEntity entity);

    PublishJobEntity toEntity(PublishJob domain);

    /**
     * Update entity from domain model.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    void updateEntityFromDomain(PublishJob domain, @MappingTarget PublishJobEntity entity);
}
