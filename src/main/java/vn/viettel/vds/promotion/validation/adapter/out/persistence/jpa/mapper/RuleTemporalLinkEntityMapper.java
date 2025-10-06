package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.mapper;

import org.mapstruct.Mapper;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.RuleTemporalLinkEntity;
import vn.viettel.vds.promotion.validation.domain.entity.RuleTemporalLink;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RuleTemporalLinkEntityMapper {

    RuleTemporalLink toDomain(RuleTemporalLinkEntity entity);

    RuleTemporalLinkEntity toEntity(RuleTemporalLink domain);

    List<RuleTemporalLink> toDomainList(List<RuleTemporalLinkEntity> entities);

    List<RuleTemporalLinkEntity> toEntityList(List<RuleTemporalLink> domains);
}
