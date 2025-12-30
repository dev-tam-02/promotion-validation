package vn.viettel.vds.promotion.validation.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.viettel.vds.promotion.validation.application.port.out.MetadataSchemaPersistencePort;
import vn.viettel.vds.promotion.validation.domain.model.MetadataSchema;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing metadata schemas.
 * Metadata schemas define dynamic fields for customer, order, redemption, and custom event metadata.
 */
@Service
@Transactional
public class MetadataSchemaService {

    private static final Logger logger = LoggerFactory.getLogger(MetadataSchemaService.class);

    private final MetadataSchemaPersistencePort persistencePort;

    public MetadataSchemaService(MetadataSchemaPersistencePort persistencePort) {
        this.persistencePort = persistencePort;
    }

    /**
     * Get all metadata fields for a schema type.
     *
     * @param tenantId   the tenant identifier
     * @param schemaType the schema type (customer, order, redemption, custom_event)
     * @return list of metadata schema fields
     */
    @Transactional(readOnly = true)
    public List<MetadataSchema> getSchemaFields(String tenantId, String schemaType) {
        logger.debug("Getting schema fields for tenant: {}, type: {}", tenantId, schemaType);
        return persistencePort.findActiveByTenantIdAndSchemaType(tenantId, schemaType);
    }

    /**
     * Get a specific metadata field.
     *
     * @param tenantId   the tenant identifier
     * @param schemaType the schema type
     * @param fieldKey   the field key
     * @return optional metadata schema
     */
    @Transactional(readOnly = true)
    public Optional<MetadataSchema> getSchemaField(String tenantId, String schemaType, String fieldKey) {
        logger.debug("Getting schema field: {}.{} for tenant: {}", schemaType, fieldKey, tenantId);
        return persistencePort.findByTenantIdAndSchemaTypeAndFieldKey(tenantId, schemaType, fieldKey);
    }

    /**
     * Create a new metadata field.
     *
     * @param tenantId   the tenant identifier
     * @param schemaType the schema type
     * @param request    the create request
     * @return created metadata schema
     */
    public MetadataSchema createSchemaField(String tenantId, String schemaType, CreateSchemaFieldRequest request) {
        logger.info("Creating schema field: {}.{} for tenant: {}", schemaType, request.fieldKey(), tenantId);

        // Check if field already exists
        if (persistencePort.existsByTenantIdAndSchemaTypeAndFieldKey(tenantId, schemaType, request.fieldKey())) {
            throw new IllegalArgumentException("Schema field already exists: " + schemaType + "." + request.fieldKey());
        }

        // Get next display order
        long count = persistencePort.countByTenantIdAndSchemaType(tenantId, schemaType);

        MetadataSchema schema = MetadataSchema.builder()
                .id(UUID.randomUUID().toString())
                .tenantId(tenantId)
                .schemaType(parseSchemaType(schemaType))
                .fieldKey(request.fieldKey())
                .fieldName(request.fieldName())
                .fieldType(request.fieldType())
                .availableValues(request.availableValues())
                .required(request.required() != null ? request.required() : false)
                .displayOrder((int) count)
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        return persistencePort.save(schema);
    }

    /**
     * Update an existing metadata field.
     *
     * @param tenantId   the tenant identifier
     * @param schemaType the schema type
     * @param fieldKey   the field key
     * @param request    the update request
     * @return updated metadata schema
     */
    public MetadataSchema updateSchemaField(String tenantId, String schemaType, String fieldKey, UpdateSchemaFieldRequest request) {
        logger.info("Updating schema field: {}.{} for tenant: {}", schemaType, fieldKey, tenantId);

        MetadataSchema existing = persistencePort.findByTenantIdAndSchemaTypeAndFieldKey(tenantId, schemaType, fieldKey)
                .orElseThrow(() -> new IllegalArgumentException("Schema field not found: " + schemaType + "." + fieldKey));

        MetadataSchema updated = existing.toBuilder()
                .fieldName(request.fieldName() != null ? request.fieldName() : existing.getFieldName())
                .fieldType(request.fieldType() != null ? request.fieldType() : existing.getFieldType())
                .availableValues(request.availableValues() != null ? request.availableValues() : existing.getAvailableValues())
                .required(request.required() != null ? request.required() : existing.isRequired())
                .displayOrder(request.displayOrder() != null ? request.displayOrder() : existing.getDisplayOrder())
                .updatedAt(Instant.now())
                .build();

        return persistencePort.save(updated);
    }

    /**
     * Delete a metadata field.
     *
     * @param tenantId   the tenant identifier
     * @param schemaType the schema type
     * @param fieldKey   the field key
     */
    public void deleteSchemaField(String tenantId, String schemaType, String fieldKey) {
        logger.info("Deleting schema field: {}.{} for tenant: {}", schemaType, fieldKey, tenantId);

        if (!persistencePort.existsByTenantIdAndSchemaTypeAndFieldKey(tenantId, schemaType, fieldKey)) {
            throw new IllegalArgumentException("Schema field not found: " + schemaType + "." + fieldKey);
        }

        persistencePort.deleteByTenantIdAndSchemaTypeAndFieldKey(tenantId, schemaType, fieldKey);
    }

    /**
     * Toggle active status of a metadata field.
     *
     * @param tenantId   the tenant identifier
     * @param schemaType the schema type
     * @param fieldKey   the field key
     * @param active     new active status
     * @return updated metadata schema
     */
    public MetadataSchema toggleActive(String tenantId, String schemaType, String fieldKey, boolean active) {
        logger.info("Setting schema field {}.{} active={} for tenant: {}", schemaType, fieldKey, active, tenantId);

        MetadataSchema existing = persistencePort.findByTenantIdAndSchemaTypeAndFieldKey(tenantId, schemaType, fieldKey)
                .orElseThrow(() -> new IllegalArgumentException("Schema field not found: " + schemaType + "." + fieldKey));

        MetadataSchema updated = existing.toBuilder()
                .active(active)
                .updatedAt(Instant.now())
                .build();

        return persistencePort.save(updated);
    }

    private MetadataSchema.SchemaType parseSchemaType(String schemaType) {
        try {
            return MetadataSchema.SchemaType.valueOf(schemaType.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid schema type: " + schemaType);
        }
    }

    // Request records
    public record CreateSchemaFieldRequest(
            String fieldKey,
            String fieldName,
            MetadataSchema.FieldType fieldType,
            List<String> availableValues,
            Boolean required
    ) {}

    public record UpdateSchemaFieldRequest(
            String fieldName,
            MetadataSchema.FieldType fieldType,
            List<String> availableValues,
            Boolean required,
            Integer displayOrder
    ) {}
}
