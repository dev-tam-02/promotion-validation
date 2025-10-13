package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.OperatorEntity;
import vn.viettel.vds.promotion.validation.domain.model.Operator;

import java.util.List;

/**
 * Mapper between JPA OperatorEntity and domain Operator.
 * Used by JPA adapter to convert between persistence and domain layers.
 */
@Mapper(componentModel = "spring")
public interface OperatorEntityMapper {

    Operator toDomain(OperatorEntity entity);

    OperatorEntity toEntity(Operator domain);

    List<Operator> toDomainList(List<OperatorEntity> entities);

    List<OperatorEntity> toEntityList(List<Operator> domains);
}
