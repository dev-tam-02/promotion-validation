package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.mapper;

import org.mapstruct.Mapper;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.ReasonCodeEntity;
import vn.viettel.vds.promotion.validation.domain.entity.ReasonCode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface ReasonCodeEntityMapper {

    default ReasonCode toDomain(ReasonCodeEntity entity) {
        if (entity == null) {
            return null;
        }

        ReasonCode domain = new ReasonCode();
        domain.setId(entity.getId());
        domain.setTenantId(entity.getTenantId());
        domain.setCategory(entity.getCategory());
        domain.setSeverity(mapSeverity(entity.getSeverity()));
        domain.setLabels(mapLabelsToString(entity.getLabels()));
        domain.setCreatedAt(entity.getCreatedAt());

        return domain;
    }

    default ReasonCodeEntity toEntity(ReasonCode domain) {
        if (domain == null) {
            return null;
        }

        ReasonCodeEntity entity = new ReasonCodeEntity();
        entity.setId(domain.getId());
        entity.setTenantId(domain.getTenantId());
        entity.setCategory(domain.getCategory());
        entity.setSeverity(mapSeverity(domain.getSeverity()));
        entity.setLabels(mapLabelsToObject(domain.getLabels()));
        entity.setCreatedAt(domain.getCreatedAt());

        return entity;
    }

    List<ReasonCode> toDomainList(List<ReasonCodeEntity> entities);

    List<ReasonCodeEntity> toEntityList(List<ReasonCode> domains);

    // Map Severity enum
    default ReasonCode.Severity mapSeverity(ReasonCodeEntity.Severity severity) {
        if (severity == null) {
            return null;
        }
        return ReasonCode.Severity.valueOf(severity.name());
    }

    default ReasonCodeEntity.Severity mapSeverity(ReasonCode.Severity severity) {
        if (severity == null) {
            return null;
        }
        return ReasonCodeEntity.Severity.valueOf(severity.name());
    }

    // Convert Map<String, Object> to Map<String, String>
    default Map<String, String> mapLabelsToString(Map<String, Object> labels) {
        if (labels == null) {
            return null;
        }
        return labels.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue() != null ? e.getValue().toString() : null
                ));
    }

    // Convert Map<String, String> to Map<String, Object>
    default Map<String, Object> mapLabelsToObject(Map<String, String> labels) {
        if (labels == null) {
            return null;
        }
        return new HashMap<>(labels);
    }
}
