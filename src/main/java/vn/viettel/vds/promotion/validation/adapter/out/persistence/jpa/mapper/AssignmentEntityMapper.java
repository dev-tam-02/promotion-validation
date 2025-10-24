package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.AssignmentEntity;
import vn.viettel.vds.promotion.validation.domain.model.Assignment;

import java.util.List;

/**
 * Mapper between Assignment domain model and AssignmentEntity.
 * <p>
 * NOTE: The domain model contains fields that don't exist in the database schema:
 * - tenantId, ruleVersionPinned, subject, assignmentVersion, validFrom, validTo,
 * trafficPercent, stickyKeyStrategy, createdBy, updatedBy, version
 * <p>
 * The database schema only has: id, entity_type, entity_id, rule_id, priority, active,
 * created_at, updated_at
 * <p>
 */
@Mapper(componentModel = "spring")
public interface AssignmentEntityMapper {

    /**
     * Maps entity to domain with limited fields.
     * Many domain fields will be null due to schema mismatch.
     */
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

    /**
     * Maps domain to entity with limited fields.
     * Many entity fields will be null due to schema mismatch.
     */
    @Mapping(source = "subject.type", target = "entityType")
    @Mapping(source = "subject.key", target = "entityId")
    @Mapping(target = "priority", ignore = true)
    AssignmentEntity toEntity(Assignment domain);

    List<Assignment> toDomainList(List<AssignmentEntity> entities);

    List<AssignmentEntity> toEntityList(List<Assignment> domains);
}
