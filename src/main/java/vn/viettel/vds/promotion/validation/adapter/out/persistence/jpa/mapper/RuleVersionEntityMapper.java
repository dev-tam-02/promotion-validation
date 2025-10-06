package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.RuleVersionEntity;
import vn.viettel.vds.promotion.validation.domain.entity.RuleVersion;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RuleVersionEntityMapper {

    @Mapping(source = "ruleVersion", target = "version")
    @Mapping(target = "logic", ignore = true) // Handle manually due to different enum types
    @Mapping(target = "nodes", ignore = true) // Handle manually - different types
    @Mapping(target = "timeLinks", ignore = true) // Handle manually - OneToMany relationship
    @Mapping(target = "compile", ignore = true) // Handle manually - different types
    RuleVersion toDomain(RuleVersionEntity entity);

    @Mapping(source = "version", target = "ruleVersion")
    @Mapping(target = "logic", ignore = true) // Handle manually
    @Mapping(target = "nodes", ignore = true) // Handle manually
    @Mapping(target = "timeLinks", ignore = true) // Handle manually
    @Mapping(target = "compile", ignore = true) // Handle manually
    RuleVersionEntity toEntity(RuleVersion domain);

    List<RuleVersion> toDomainList(List<RuleVersionEntity> entities);

    List<RuleVersionEntity> toEntityList(List<RuleVersion> domains);
}
