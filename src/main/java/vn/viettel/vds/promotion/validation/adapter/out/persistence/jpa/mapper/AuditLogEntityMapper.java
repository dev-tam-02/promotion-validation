package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.AuditLogEntity;
import vn.viettel.vds.promotion.validation.domain.entity.AuditLog;

import java.util.List;

/**
 * Mapper between JPA AuditLogEntity and domain AuditLog.
 */
@Mapper(componentModel = "spring")
public interface AuditLogEntityMapper {

    AuditLog toDomain(AuditLogEntity entity);

    AuditLogEntity toEntity(AuditLog domain);

    List<AuditLog> toDomainList(List<AuditLogEntity> entities);

    List<AuditLogEntity> toEntityList(List<AuditLog> domains);

    // MapStruct will automatically map between AuditTargetEmbeddable <-> AuditTarget
    // and AuditAction enum (same enum values)
    default AuditLog.AuditTarget mapTarget(AuditLogEntity.AuditTargetEmbeddable embeddable) {
        if (embeddable == null) {
            return null;
        }
        AuditLog.AuditTarget target = new AuditLog.AuditTarget();
        target.setType(embeddable.getType());
        target.setId(embeddable.getId());
        return target;
    }

    default AuditLogEntity.AuditTargetEmbeddable mapTarget(AuditLog.AuditTarget target) {
        if (target == null) {
            return null;
        }
        AuditLogEntity.AuditTargetEmbeddable embeddable = new AuditLogEntity.AuditTargetEmbeddable();
        embeddable.setType(target.getType());
        embeddable.setId(target.getId());
        return embeddable;
    }

    default AuditLog.AuditAction mapAction(AuditLogEntity.AuditAction action) {
        if (action == null) {
            return null;
        }
        return AuditLog.AuditAction.valueOf(action.name());
    }

    default AuditLogEntity.AuditAction mapAction(AuditLog.AuditAction action) {
        if (action == null) {
            return null;
        }
        return AuditLogEntity.AuditAction.valueOf(action.name());
    }
}
