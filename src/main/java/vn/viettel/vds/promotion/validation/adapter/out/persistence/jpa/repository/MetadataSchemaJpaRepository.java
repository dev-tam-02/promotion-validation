package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository;

import com.promix.platform.data.jpa.autoconfigure.condition.ConditionalOnPromixJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.MetadataSchemaEntity;

import java.util.List;
import java.util.Optional;

/**
 * JPA repository for metadata schemas.
 */
@ConditionalOnPromixJpa
@Repository
public interface MetadataSchemaJpaRepository extends JpaRepository<MetadataSchemaEntity, String> {

    /**
     * Find schemas by tenant ID and schema type.
     */
    List<MetadataSchemaEntity> findByTenantIdAndSchemaTypeOrderByDisplayOrderAsc(String tenantId, String schemaType);

    /**
     * Find active schemas by tenant ID and schema type.
     */
    List<MetadataSchemaEntity> findByTenantIdAndSchemaTypeAndActiveTrueOrderByDisplayOrderAsc(String tenantId, String schemaType);

    /**
     * Find schema by tenant ID, schema type, and field key.
     */
    Optional<MetadataSchemaEntity> findByTenantIdAndSchemaTypeAndFieldKey(String tenantId, String schemaType, String fieldKey);

    /**
     * Find all schemas by tenant ID.
     */
    List<MetadataSchemaEntity> findByTenantIdOrderBySchemaTypeAscDisplayOrderAsc(String tenantId);

    /**
     * Find all active schemas by tenant ID.
     */
    List<MetadataSchemaEntity> findByTenantIdAndActiveTrueOrderBySchemaTypeAscDisplayOrderAsc(String tenantId);

    /**
     * Check if schema exists by tenant ID, schema type, and field key.
     */
    boolean existsByTenantIdAndSchemaTypeAndFieldKey(String tenantId, String schemaType, String fieldKey);

    /**
     * Delete schema by tenant ID, schema type, and field key.
     */
    void deleteByTenantIdAndSchemaTypeAndFieldKey(String tenantId, String schemaType, String fieldKey);

    /**
     * Count schemas by tenant ID and schema type.
     */
    long countByTenantIdAndSchemaType(String tenantId, String schemaType);
}
