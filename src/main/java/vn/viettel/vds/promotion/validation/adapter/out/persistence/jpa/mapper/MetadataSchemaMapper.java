package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.MetadataSchemaEntity;
import vn.viettel.vds.promotion.validation.domain.model.MetadataSchema;

import java.util.Collections;
import java.util.List;

/**
 * MapStruct mapper for converting between MetadataSchema domain model and MetadataSchemaEntity.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MetadataSchemaMapper {

    @Mapping(target = "schemaType", source = "schemaType", qualifiedByName = "stringToSchemaType")
    @Mapping(target = "fieldType", source = "fieldType", qualifiedByName = "stringToFieldType")
    @Mapping(target = "availableValues", source = "availableValues", qualifiedByName = "jsonToList")
    MetadataSchema toDomain(MetadataSchemaEntity entity);

    @Mapping(target = "schemaType", source = "schemaType", qualifiedByName = "schemaTypeToString")
    @Mapping(target = "fieldType", source = "fieldType", qualifiedByName = "fieldTypeToString")
    @Mapping(target = "availableValues", source = "availableValues", qualifiedByName = "listToJson")
    MetadataSchemaEntity toEntity(MetadataSchema domain);

    @Named("stringToSchemaType")
    default MetadataSchema.SchemaType stringToSchemaType(String value) {
        if (value == null) return null;
        try {
            return MetadataSchema.SchemaType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Named("schemaTypeToString")
    default String schemaTypeToString(MetadataSchema.SchemaType type) {
        return type != null ? type.name().toLowerCase() : null;
    }

    @Named("stringToFieldType")
    default MetadataSchema.FieldType stringToFieldType(String value) {
        if (value == null) return null;
        try {
            return MetadataSchema.FieldType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Named("fieldTypeToString")
    default String fieldTypeToString(MetadataSchema.FieldType type) {
        return type != null ? type.name().toLowerCase() : null;
    }

    @Named("jsonToList")
    default List<String> jsonToList(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }

    @Named("listToJson")
    default String listToJson(List<String> list) {
        if (list == null || list.isEmpty()) return null;
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
