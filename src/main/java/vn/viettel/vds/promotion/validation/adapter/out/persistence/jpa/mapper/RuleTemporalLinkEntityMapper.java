package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.RuleTemporalLinkEntity;
import vn.viettel.vds.promotion.validation.domain.model.RuleTemporalLink;

import java.util.List;

/**
 * MapStruct mapper for converting between RuleTemporalLinkEntity (persistence) and RuleTemporalLink (domain).
 *
 * <p>This mapper handles the JPA relationship pattern where:</p>
 * <ul>
 *   <li><strong>Entity layer</strong>: Uses ManyToOne relationships (validationRule, temporalPolicy)</li>
 *   <li><strong>Domain layer</strong>: Uses simple ID references (validationRuleId, temporalPolicyId)</li>
 * </ul>
 *
 * <p>The ID fields in the domain are populated separately by the repository layer
 * by extracting IDs from the entity relationships. Similarly, when converting domain to entity,
 * the repository layer is responsible for loading and setting the actual entity references.</p>
 */
@Mapper(componentModel = "spring")
public interface RuleTemporalLinkEntityMapper {

    /**
     * Maps entity to domain. The validationRuleId and temporalPolicyId fields are intentionally
     * ignored here and should be populated by the repository layer from the entity relationships.
     */
    @Mapping(target = "validationRuleId", ignore = true)  // Populated from entity.getValidationRule().getId()
    @Mapping(target = "temporalPolicyId", ignore = true)
    // Populated from entity.getTemporalPolicy().getId()
    RuleTemporalLink toDomain(RuleTemporalLinkEntity entity);

    /**
     * Maps domain to entity. The validationRule and temporalPolicy relationships are intentionally
     * ignored here and should be set by the repository layer by loading the referenced entities.
     */
    @Mapping(target = "validationRule", ignore = true)  // Set by repository after loading ValidationRuleEntity
    @Mapping(target = "temporalPolicy", ignore = true)
    // Set by repository after loading TemporalPolicyEntity
    RuleTemporalLinkEntity toEntity(RuleTemporalLink domain);

    List<RuleTemporalLink> toDomainList(List<RuleTemporalLinkEntity> entities);

    List<RuleTemporalLinkEntity> toEntityList(List<RuleTemporalLink> domains);
}
