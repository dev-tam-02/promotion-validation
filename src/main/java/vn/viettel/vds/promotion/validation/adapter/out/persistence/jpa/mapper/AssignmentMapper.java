package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.AssignmentEntity;
import vn.viettel.vds.promotion.validation.domain.model.Assignment;

/**
 * MapStruct mapper for converting between Assignment domain model and AssignmentEntity.
 * <p>
 * NOTE: The domain model contains fields that don't exist in the database schema.
 * This mapper provides best-effort mapping between the two structures.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AssignmentMapper {

    @Mapping(source = "entityType", target = "subject.type")
    @Mapping(source = "entityId", target = "subject.key")
    @Mapping(target = "ruleVersionPinned", ignore = true)
    @Mapping(target = "assignmentVersion", ignore = true)
    @Mapping(target = "validFrom", ignore = true)
    @Mapping(target = "validTo", ignore = true)
    @Mapping(target = "trafficPercent", ignore = true)
    @Mapping(target = "stickyKeyStrategy", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    Assignment toDomain(AssignmentEntity entity);

    @Mapping(source = "subject.type", target = "entityType")
    @Mapping(source = "subject.key", target = "entityId")
    @Mapping(target = "priority", ignore = true)
    AssignmentEntity toEntity(Assignment domain);

    /**
     * Update entity from domain model.
     */
    @Mapping(source = "subject.type", target = "entityType")
    @Mapping(source = "subject.key", target = "entityId")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "priority", ignore = true)
    void updateEntityFromDomain(Assignment domain, @MappingTarget AssignmentEntity entity);
}
