package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.TemporalPolicyEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.TemporalPolicyWindowEntity;
import vn.viettel.vds.promotion.validation.domain.model.TemporalPolicy;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface TemporalPolicyEntityMapper {

    @Mapping(target = "ruleLinks", ignore = true)
    @Mapping(target = "timeExceptions", ignore = true)
    TemporalPolicyEntity toEntity(TemporalPolicy domain);

    TemporalPolicy toDomain(TemporalPolicyEntity entity);

    List<TemporalPolicy> toDomainList(List<TemporalPolicyEntity> entities);

    List<TemporalPolicyEntity> toEntityList(List<TemporalPolicy> domains);
}
