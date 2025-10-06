package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.mapper;

import org.mapstruct.*;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.AssignmentEntity;
import vn.viettel.vds.promotion.validation.domain.model.Assignment;

/**
 * MapStruct mapper for converting between Assignment domain model and AssignmentEntity.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AssignmentMapper {

    @Mapping(source = "subject", target = "subject")
    @Mapping(source = "stickyKeyStrategy", target = "stickyKeyStrategy")
    Assignment toDomain(AssignmentEntity entity);

    @Mapping(source = "subject", target = "subject")
    @Mapping(source = "stickyKeyStrategy", target = "stickyKeyStrategy")
    AssignmentEntity toEntity(Assignment domain);

    /**
     * Update entity from domain model.
     */
    @Mapping(source = "subject", target = "subject")
    @Mapping(source = "stickyKeyStrategy", target = "stickyKeyStrategy")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    void updateEntityFromDomain(Assignment domain, @MappingTarget AssignmentEntity entity);

    /**
     * Map Subject to SubjectEmbeddable
     */
    default AssignmentEntity.SubjectEmbeddable mapSubject(Assignment.Subject subject) {
        if (subject == null) {
            return null;
        }
        AssignmentEntity.SubjectEmbeddable embeddable = new AssignmentEntity.SubjectEmbeddable();
        embeddable.setType(subject.getType());
        embeddable.setKey(subject.getKey());
        return embeddable;
    }

    /**
     * Map SubjectEmbeddable to Subject
     */
    default Assignment.Subject mapSubjectEmbeddable(AssignmentEntity.SubjectEmbeddable embeddable) {
        if (embeddable == null) {
            return null;
        }
        Assignment.Subject subject = new Assignment.Subject();
        subject.setType(embeddable.getType());
        subject.setKey(embeddable.getKey());
        return subject;
    }

    /**
     * Map StickyKeyStrategy enum
     */
    default Assignment.StickyKeyStrategy mapStickyKeyStrategy(AssignmentEntity.StickyKeyStrategy strategy) {
        if (strategy == null) {
            return null;
        }
        return Assignment.StickyKeyStrategy.valueOf(strategy.name());
    }

    /**
     * Map StickyKeyStrategy enum (reverse)
     */
    default AssignmentEntity.StickyKeyStrategy mapStickyKeyStrategyToEntity(Assignment.StickyKeyStrategy strategy) {
        if (strategy == null) {
            return null;
        }
        return AssignmentEntity.StickyKeyStrategy.valueOf(strategy.name());
    }
}
