package vn.viettel.vds.promotion.validation.adapter.in.web.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import vn.viettel.vds.promotion.validation.adapter.in.web.dto.OperatorResponse;
import vn.viettel.vds.promotion.validation.domain.entity.Operator;

@Mapper(componentModel = "spring")
public interface OperatorMapper {

    @Mapping(source = "id", target = "operatorId")
    @Mapping(source = "status", target = "status", qualifiedByName = "operatorStatusToString")
    OperatorResponse toOperatorResponse(Operator operator);

    @Named("operatorStatusToString")
    default String operatorStatusToString(Operator.OperatorStatus status) {
        return status != null ? status.name().toLowerCase() : null;
    }
}