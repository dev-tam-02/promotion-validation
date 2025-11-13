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
 *   <li><strong>Entity layer</strong>: Uses ManyToOne relationships (assignment, temporalPolicy)</li>
 *   <li><strong>Domain layer</strong>: Uses simple ID references (assignmentId, temporalPolicyId)</li>
 * </ul>
 *
 * <p>FIXED: Changed from validationRule to assignment relationship</p>
 * <p>Rationale: Temporal constraints are assignment-specific, not rule-specific</p>
 *
 * <p>The ID fields in the domain are populated separately by the repository layer
 * by extracting IDs from the entity relationships. Similarly, when converting domain to entity,
 * the repository layer is responsible for loading and setting the actual entity references.</p>
 */
@Mapper(componentModel = "spring")
public interface RuleTemporalLinkEntityMapper {

    /**
     * Maps entity to domain. The assignmentId and temporalPolicyId fields are intentionally
     * ignored here and should be populated by the repository layer from the entity relationships.
     * FIXED: Changed validationRuleId to assignmentId
     */
    @Mapping(target = "assignmentId", ignore = true)  // FIXED: was validationRuleId
    @Mapping(target = "temporalPolicyId", ignore = true)
    RuleTemporalLink toDomain(RuleTemporalLinkEntity entity);

    /**
     * Maps domain to entity. The assignment and temporalPolicy relationships are intentionally
     * ignored here and should be set by the repository layer by loading the referenced entities.
     * FIXED: Changed validationRule to assignment
     */
    @Mapping(target = "assignment", ignore = true)  // FIXED: was validationRule
    @Mapping(target = "temporalPolicy", ignore = true)
    RuleTemporalLinkEntity toEntity(RuleTemporalLink domain);

    List<RuleTemporalLink> toDomainList(List<RuleTemporalLinkEntity> entities);

    List<RuleTemporalLinkEntity> toEntityList(List<RuleTemporalLink> domains);
}
