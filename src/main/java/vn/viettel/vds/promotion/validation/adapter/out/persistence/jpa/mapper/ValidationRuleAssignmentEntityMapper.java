package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.ValidationRuleAssignmentEntity;
import vn.viettel.vds.promotion.validation.domain.model.ValidationRuleAssignment;

/**
 * MapStruct mapper để convert giữa ValidationRuleAssignmentEntity (JPA) và
 * ValidationRuleAssignment (Domain model).
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ValidationRuleAssignmentEntityMapper {

    /**
     * Convert từ domain model sang JPA entity.
     *
     * @param domain Domain model
     * @return JPA entity
     */
    ValidationRuleAssignmentEntity toEntity(ValidationRuleAssignment domain);

    /**
     * Convert từ JPA entity sang domain model.
     *
     * @param entity JPA entity
     * @return Domain model
     */
    ValidationRuleAssignment toDomain(ValidationRuleAssignmentEntity entity);
}
