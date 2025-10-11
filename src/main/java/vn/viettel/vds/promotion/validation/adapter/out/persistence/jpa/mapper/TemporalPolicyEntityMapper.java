package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.TemporalPolicyEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.TemporalPolicyWindowEntity;
import vn.viettel.vds.promotion.validation.domain.model.TemporalPolicy;
import vn.viettel.vds.promotion.validation.domain.model.TimeOfDayWindow;

import java.util.List;

/**
 * MapStruct mapper for converting between TemporalPolicyEntity (persistence) and TemporalPolicy (domain).
 *
 * <p>Mapping Notes:</p>
 * <ul>
 *   <li><strong>ruleLinks</strong>: OneToMany relationship managed separately by RuleTemporalLinkEntity,
 *       not populated during basic temporal policy mapping.</li>
 *   <li><strong>timeExceptions</strong>: OneToMany relationship managed separately by TimeExceptionEntity,
 *       loaded on-demand when needed for time validation.</li>
 *   <li><strong>timeOfDayWindows</strong>: Mapped using custom mapping methods to convert between
 *       entity and domain window representations.</li>
 * </ul>
 */
@Mapper(componentModel = "spring")
public interface TemporalPolicyEntityMapper {

    /**
     * Maps domain to entity. Related collections (ruleLinks, timeExceptions) are ignored
     * as they are managed separately.
     */
    @Mapping(target = "ruleLinks", ignore = true)       // Managed by RuleTemporalLinkEntity
    @Mapping(target = "timeExceptions", ignore = true)
    // Managed by TimeExceptionEntity
    TemporalPolicyEntity toEntity(TemporalPolicy domain);

    TemporalPolicy toDomain(TemporalPolicyEntity entity);

    List<TemporalPolicy> toDomainList(List<TemporalPolicyEntity> entities);

    List<TemporalPolicyEntity> toEntityList(List<TemporalPolicy> domains);

    /**
     * Maps a time window entity to domain model.
     */
    TimeOfDayWindow windowToDomain(TemporalPolicyWindowEntity entity);

    /**
     * Maps a time window domain to entity.
     * Audit fields and temporal policy relationship are ignored as they're set by the repository.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "temporalPolicy", ignore = true)
    // Set by repository when persisting
    TemporalPolicyWindowEntity windowToEntity(TimeOfDayWindow domain);

    List<TimeOfDayWindow> windowsToDomain(List<TemporalPolicyWindowEntity> entities);

    List<TemporalPolicyWindowEntity> windowsToEntity(List<TimeOfDayWindow> domains);
}
