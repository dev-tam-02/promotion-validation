package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.AssignmentSnapshotEntity;
import vn.viettel.vds.promotion.validation.domain.model.AssignmentSnapshot;

import java.util.List;

/**
 * MapStruct mapper for converting between AssignmentSnapshot domain model and entity.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AssignmentSnapshotMapper {

    @Mapping(source = "snapshotReason", target = "snapshotReason", qualifiedByName = "toDomainSnapshotReason")
    AssignmentSnapshot toDomain(AssignmentSnapshotEntity entity);

    List<AssignmentSnapshot> toDomainList(List<AssignmentSnapshotEntity> entities);

    @Mapping(source = "snapshotReason", target = "snapshotReason", qualifiedByName = "toEntitySnapshotReason")
    AssignmentSnapshotEntity toEntity(AssignmentSnapshot domain);

    List<AssignmentSnapshotEntity> toEntityList(List<AssignmentSnapshot> domains);

    @Named("toDomainSnapshotReason")
    default AssignmentSnapshot.SnapshotReason toDomainSnapshotReason(AssignmentSnapshotEntity.SnapshotReason reason) {
        if (reason == null) {
            return null;
        }
        return AssignmentSnapshot.SnapshotReason.valueOf(reason.name());
    }

    @Named("toEntitySnapshotReason")
    default AssignmentSnapshotEntity.SnapshotReason toEntitySnapshotReason(AssignmentSnapshot.SnapshotReason reason) {
        if (reason == null) {
            return null;
        }
        return AssignmentSnapshotEntity.SnapshotReason.valueOf(reason.name());
    }
}
