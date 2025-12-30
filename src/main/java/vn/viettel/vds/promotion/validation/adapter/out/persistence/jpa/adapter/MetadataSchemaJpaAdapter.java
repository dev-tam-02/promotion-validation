package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.adapter;

import com.promix.platform.data.jpa.autoconfigure.condition.ConditionalOnPromixJpa;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.MetadataSchemaEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.mapper.MetadataSchemaMapper;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.MetadataSchemaJpaRepository;
import vn.viettel.vds.promotion.validation.application.port.out.MetadataSchemaPersistencePort;
import vn.viettel.vds.promotion.validation.domain.model.MetadataSchema;

import java.util.List;
import java.util.Optional;

/**
 * JPA adapter implementation for MetadataSchema persistence.
 */
@Component
@ConditionalOnPromixJpa
public class MetadataSchemaJpaAdapter implements MetadataSchemaPersistencePort {

    private final MetadataSchemaJpaRepository repository;
    private final MetadataSchemaMapper mapper;

    public MetadataSchemaJpaAdapter(MetadataSchemaJpaRepository repository, MetadataSchemaMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<MetadataSchema> findByTenantIdAndSchemaType(String tenantId, String schemaType) {
        return repository.findByTenantIdAndSchemaTypeOrderByDisplayOrderAsc(tenantId, schemaType)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<MetadataSchema> findActiveByTenantIdAndSchemaType(String tenantId, String schemaType) {
        return repository.findByTenantIdAndSchemaTypeAndActiveTrueOrderByDisplayOrderAsc(tenantId, schemaType)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<MetadataSchema> findByTenantIdAndSchemaTypeAndFieldKey(String tenantId, String schemaType, String fieldKey) {
        return repository.findByTenantIdAndSchemaTypeAndFieldKey(tenantId, schemaType, fieldKey)
                .map(mapper::toDomain);
    }

    @Override
    public List<MetadataSchema> findByTenantId(String tenantId) {
        return repository.findByTenantIdOrderBySchemaTypeAscDisplayOrderAsc(tenantId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<MetadataSchema> findActiveByTenantId(String tenantId) {
        return repository.findByTenantIdAndActiveTrueOrderBySchemaTypeAscDisplayOrderAsc(tenantId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public MetadataSchema save(MetadataSchema schema) {
        MetadataSchemaEntity entity = mapper.toEntity(schema);
        MetadataSchemaEntity saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public boolean existsByTenantIdAndSchemaTypeAndFieldKey(String tenantId, String schemaType, String fieldKey) {
        return repository.existsByTenantIdAndSchemaTypeAndFieldKey(tenantId, schemaType, fieldKey);
    }

    @Override
    @Transactional
    public void deleteByTenantIdAndSchemaTypeAndFieldKey(String tenantId, String schemaType, String fieldKey) {
        repository.deleteByTenantIdAndSchemaTypeAndFieldKey(tenantId, schemaType, fieldKey);
    }

    @Override
    public void deleteById(String id) {
        repository.deleteById(id);
    }

    @Override
    public long countByTenantIdAndSchemaType(String tenantId, String schemaType) {
        return repository.countByTenantIdAndSchemaType(tenantId, schemaType);
    }
}
