package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.RuleVersionEntity;
import vn.viettel.vds.promotion.validation.domain.model.RuleVersion;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE, unmappedSourcePolicy = org.mapstruct.ReportingPolicy.IGNORE)
public interface RuleVersionEntityMapper {

    @Mapping(source = "version", target = "entityVersion")
    RuleVersion toDomain(RuleVersionEntity entity);

    @Mapping(source = "entityVersion", target = "version")
    RuleVersionEntity toEntity(RuleVersion domain);

    List<RuleVersion> toDomainList(List<RuleVersionEntity> entities);

    List<RuleVersionEntity> toEntityList(List<RuleVersion> domains);
}
