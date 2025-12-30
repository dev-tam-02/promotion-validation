package vn.viettel.vds.promotion.validation.application.port.out;

import vn.viettel.vds.promotion.validation.domain.model.MetadataSchema;

import java.util.List;
import java.util.Optional;

/**
 * Port for metadata schema persistence operations.
 */
public interface MetadataSchemaPersistencePort {

    /**
     * Find schemas by tenant ID and schema type.
     */
    List<MetadataSchema> findByTenantIdAndSchemaType(String tenantId, String schemaType);

    /**
     * Find active schemas by tenant ID and schema type.
     */
    List<MetadataSchema> findActiveByTenantIdAndSchemaType(String tenantId, String schemaType);

    /**
     * Find schema by tenant ID, schema type, and field key.
     */
    Optional<MetadataSchema> findByTenantIdAndSchemaTypeAndFieldKey(String tenantId, String schemaType, String fieldKey);

    /**
     * Find all schemas by tenant ID.
     */
    List<MetadataSchema> findByTenantId(String tenantId);

    /**
     * Find all active schemas by tenant ID.
     */
    List<MetadataSchema> findActiveByTenantId(String tenantId);

    /**
     * Save schema.
     */
    MetadataSchema save(MetadataSchema schema);

    /**
     * Check if schema exists.
     */
    boolean existsByTenantIdAndSchemaTypeAndFieldKey(String tenantId, String schemaType, String fieldKey);

    /**
     * Delete schema by tenant ID, schema type, and field key.
     */
    void deleteByTenantIdAndSchemaTypeAndFieldKey(String tenantId, String schemaType, String fieldKey);

    /**
     * Delete schema by ID.
     */
    void deleteById(String id);

    /**
     * Count schemas by tenant ID and schema type.
     */
    long countByTenantIdAndSchemaType(String tenantId, String schemaType);
}
