package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.promix.platform.core.util.IdGenerator;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.AuditLogEntity;
import vn.viettel.vds.promotion.validation.domain.model.AuditLog;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Mapper between JPA AuditLogEntity and domain AuditLog.
 * <p>
 * NOTE: The domain model contains fields that don't exist in the database schema:
 * - tenantId, diff (replaced by details, snapshotBefore, snapshotAfter)
 * <p>
 * The database schema has: entity_type, entity_id, actor_id, action, details,
 * snapshot_before, snapshot_after, timestamp
 */
@Mapper(componentModel = "spring")
public interface AuditLogEntityMapper {

    ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Mapping(source = "entityType", target = "target.type")
    @Mapping(source = "entityId", target = "target.id")
    @Mapping(source = "actorId", target = "actor")
    @Mapping(source = "timestamp", target = "at")
    @Mapping(source = "details", target = "diff", qualifiedByName = "jsonStringToMap")
    AuditLog toDomain(AuditLogEntity entity);

    @Mapping(source = "target.type", target = "entityType")
    @Mapping(source = "target.id", target = "entityId")
    @Mapping(source = "actor", target = "actorId")
    @Mapping(source = "at", target = "timestamp")
    @Mapping(source = "diff", target = "details", qualifiedByName = "mapToJsonString")
    @Mapping(target = "snapshotBefore", ignore = true)
    @Mapping(target = "snapshotAfter", ignore = true)
    AuditLogEntity toEntity(AuditLog domain);

    List<AuditLog> toDomainList(List<AuditLogEntity> entities);

    List<AuditLogEntity> toEntityList(List<AuditLog> domains);

    @org.mapstruct.Named("jsonStringToMap")
    default Map<String, Object> jsonStringToMap(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    @org.mapstruct.Named("mapToJsonString")
    default String mapToJsonString(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    /**
     * After mapping from domain to entity, ensure required fields are set.
     * Generates ID and sets timestamp if not already present.
     */
    @AfterMapping
    default void setEntityDefaults(@MappingTarget AuditLogEntity entity) {
        if (entity.getId() == null) {
            entity.setId(IdGenerator.generateId());
        }
        if (entity.getTimestamp() == null) {
            entity.setTimestamp(Instant.now());
        }
    }
}
