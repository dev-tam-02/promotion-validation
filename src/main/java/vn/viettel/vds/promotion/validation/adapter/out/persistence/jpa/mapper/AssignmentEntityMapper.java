package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.mapper;

import org.mapstruct.Mapper;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.AssignmentEntity;
import vn.viettel.vds.promotion.validation.domain.entity.Assignment;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AssignmentEntityMapper {

    Assignment toDomain(AssignmentEntity entity);

    AssignmentEntity toEntity(Assignment domain);

    List<Assignment> toDomainList(List<AssignmentEntity> entities);

    List<AssignmentEntity> toEntityList(List<Assignment> domains);

    // Map SubjectEmbeddable <-> Subject
    default Assignment.Subject mapSubject(AssignmentEntity.SubjectEmbeddable embeddable) {
        if (embeddable == null) {
            return null;
        }
        Assignment.Subject subject = new Assignment.Subject();
        subject.setType(embeddable.getType());
        subject.setKey(embeddable.getKey());
        return subject;
    }

    default AssignmentEntity.SubjectEmbeddable mapSubject(Assignment.Subject subject) {
        if (subject == null) {
            return null;
        }
        AssignmentEntity.SubjectEmbeddable embeddable = new AssignmentEntity.SubjectEmbeddable();
        embeddable.setType(subject.getType());
        embeddable.setKey(subject.getKey());
        return embeddable;
    }
}
