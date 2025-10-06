package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.TemporalPolicyEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.TemporalPolicyWindowEntity;
import vn.viettel.vds.promotion.validation.domain.entity.TemporalPolicy;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface TemporalPolicyEntityMapper {

    @Mapping(target = "timeOfDayWindows", expression = "java(mapTimeOfDayWindows(entity.getTimeOfDayWindows()))")
    TemporalPolicy toDomain(TemporalPolicyEntity entity);

    @Mapping(target = "timeOfDayWindows", expression = "java(mapTimeOfDayWindowEntities(domain, domain.getTimeOfDayWindows()))")
    @Mapping(target = "ruleLinks", ignore = true)
    @Mapping(target = "timeExceptions", ignore = true)
    TemporalPolicyEntity toEntity(TemporalPolicy domain);

    List<TemporalPolicy> toDomainList(List<TemporalPolicyEntity> entities);

    List<TemporalPolicyEntity> toEntityList(List<TemporalPolicy> domains);

    // Map List<TemporalPolicyWindowEntity> to List<TimeOfDayWindow>
    default List<TemporalPolicy.TimeOfDayWindow> mapTimeOfDayWindows(List<TemporalPolicyWindowEntity> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream()
                .map(this::mapTimeOfDayWindow)
                .collect(Collectors.toList());
    }

    // Map single TemporalPolicyWindowEntity to TimeOfDayWindow
    default TemporalPolicy.TimeOfDayWindow mapTimeOfDayWindow(TemporalPolicyWindowEntity entity) {
        if (entity == null) {
            return null;
        }
        TemporalPolicy.TimeOfDayWindow window = new TemporalPolicy.TimeOfDayWindow();
        window.setStart(entity.getStart());
        window.setEnd(entity.getEnd());
        return window;
    }

    // Map List<TimeOfDayWindow> to List<TemporalPolicyWindowEntity>
    default List<TemporalPolicyWindowEntity> mapTimeOfDayWindowEntities(
            TemporalPolicy policy,
            List<TemporalPolicy.TimeOfDayWindow> windows) {
        if (windows == null) {
            return null;
        }
        return windows.stream()
                .map(window -> mapTimeOfDayWindowEntity(policy, window))
                .collect(Collectors.toList());
    }

    // Map single TimeOfDayWindow to TemporalPolicyWindowEntity
    default TemporalPolicyWindowEntity mapTimeOfDayWindowEntity(
            TemporalPolicy policy,
            TemporalPolicy.TimeOfDayWindow window) {
        if (window == null) {
            return null;
        }
        TemporalPolicyWindowEntity entity = new TemporalPolicyWindowEntity();
        entity.setStart(window.getStart());
        entity.setEnd(window.getEnd());
        // Note: temporalPolicy relationship will be set by JPA adapter
        return entity;
    }
}
