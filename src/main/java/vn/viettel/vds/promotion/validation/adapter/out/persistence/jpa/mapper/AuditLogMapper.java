package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.mapper;

import org.mapstruct.*;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.AuditLogEntity;
import vn.viettel.vds.promotion.validation.domain.model.AuditLog;

/**
 * MapStruct mapper for converting between AuditLog domain model and AuditLogEntity.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AuditLogMapper {

    AuditLog toDomain(AuditLogEntity entity);

    AuditLogEntity toEntity(AuditLog domain);

    /**
     * Update entity from domain model.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    void updateEntityFromDomain(AuditLog domain, @MappingTarget AuditLogEntity entity);
}
