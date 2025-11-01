package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.ValidationRuleAssignmentDeletedEntity;
import vn.viettel.vds.promotion.validation.domain.model.ValidationRuleAssignmentDeleted;

/**
 * MapStruct mapper để convert giữa ValidationRuleAssignmentDeletedEntity (JPA) và
 * ValidationRuleAssignmentDeleted (Domain model).
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ValidationRuleAssignmentDeletedEntityMapper {

    /**
     * Convert từ domain model sang JPA entity.
     *
     * @param domain Domain model
     * @return JPA entity
     */
    ValidationRuleAssignmentDeletedEntity toEntity(ValidationRuleAssignmentDeleted domain);

    /**
     * Convert từ JPA entity sang domain model.
     *
     * @param entity JPA entity
     * @return Domain model
     */
    ValidationRuleAssignmentDeleted toDomain(ValidationRuleAssignmentDeletedEntity entity);
}
