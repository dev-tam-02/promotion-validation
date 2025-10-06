package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.PublishJobEntity;
import vn.viettel.vds.promotion.validation.domain.entity.PublishJob;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PublishJobEntityMapper {

    @Mapping(target = "compile", ignore = true) // Handle manually due to different types
    PublishJob toDomain(PublishJobEntity entity);

    @Mapping(target = "compile", ignore = true) // Handle manually
    PublishJobEntity toEntity(PublishJob domain);

    List<PublishJob> toDomainList(List<PublishJobEntity> entities);

    List<PublishJobEntity> toEntityList(List<PublishJob> domains);
}
