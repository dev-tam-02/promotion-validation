package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.adapter;

import com.promix.platform.data.jpa.autoconfigure.condition.ConditionalOnPromixJpa;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.ReasonCodeEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.mapper.ReasonCodeEntityMapper;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.ReasonCodeJpaRepository;
import vn.viettel.vds.promotion.validation.application.port.out.ReasonCodePersistencePort;
import vn.viettel.vds.promotion.validation.domain.model.ReasonCode;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@ConditionalOnPromixJpa
public class ReasonCodeJpaAdapter implements ReasonCodePersistencePort {

    private final ReasonCodeJpaRepository repository;
    private final ReasonCodeEntityMapper mapper;

    public ReasonCodeJpaAdapter(ReasonCodeJpaRepository repository,
                                ReasonCodeEntityMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public ReasonCode save(ReasonCode reasonCode) {
        ReasonCodeEntity entity = mapper.toEntity(reasonCode);
        ReasonCodeEntity saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<ReasonCode> findByIdAndTenant(String tenantId, String id) {
        // JPA has findByTenantIdAndId, also need to support global codes (tenantId = null)
        Optional<ReasonCodeEntity> result = repository.findByTenantIdAndId(tenantId, id);
        if (result.isPresent()) {
            return result.map(mapper::toDomain);
        }

        // Check global codes (tenantId = null)
        return repository.findById(id)
                .filter(entity -> entity.getTenantId() == null)
                .map(mapper::toDomain);
    }

    @Override
    public List<ReasonCode> findByTenantAndCategory(String tenantId, String category) {
        // Include both tenant-specific and global codes
        List<ReasonCodeEntity> tenantCodes = repository.findByTenantIdAndCategory(tenantId, category);
        List<ReasonCodeEntity> globalCodes = repository.findAll().stream()
                .filter(e -> e.getTenantId() == null && category.equals(e.getCategory()))
                .collect(Collectors.toList());

        tenantCodes.addAll(globalCodes);
        return tenantCodes.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Page<ReasonCode> findByTenant(String tenantId, Pageable pageable) {
        // Include both tenant-specific and global codes
        List<ReasonCodeEntity> all = repository.findAll();
        List<ReasonCodeEntity> filtered = all.stream()
                .filter(e -> tenantId.equals(e.getTenantId()) || e.getTenantId() == null)
                .collect(Collectors.toList());
        return convertToPage(filtered, pageable);
    }

    @Override
    public Page<ReasonCode> findWithFilters(String tenantId, String categoryPattern,
                                           ReasonCode.Severity severity, Pageable pageable) {
        // Implement filtering manually
        List<ReasonCodeEntity> all = repository.findAll();
        List<ReasonCodeEntity> filtered = all.stream()
                .filter(e -> tenantId.equals(e.getTenantId()) || e.getTenantId() == null)
                .filter(e -> categoryPattern == null ||
                        (e.getCategory() != null && e.getCategory().toLowerCase().contains(categoryPattern.toLowerCase())))
                .filter(e -> severity == null ||
                        (e.getSeverity() != null && e.getSeverity().name().equals(severity.name())))
                .collect(Collectors.toList());
        return convertToPage(filtered, pageable);
    }

    @Override
    public List<ReasonCode> findByTenantIdIsNull() {
        // Find global codes
        return repository.findAll().stream()
                .filter(e -> e.getTenantId() == null)
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ReasonCode> findByTenantId(String tenantId) {
        return repository.findByTenantId(tenantId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ReasonCode> findByTenantAndSeverity(String tenantId, ReasonCode.Severity severity) {
        // Include both tenant-specific and global codes
        ReasonCodeEntity.Severity entitySeverity = mapper.mapSeverity(severity);

        List<ReasonCodeEntity> tenantCodes = repository.findByTenantIdAndSeverity(tenantId, entitySeverity);
        List<ReasonCodeEntity> globalCodes = repository.findAll().stream()
                .filter(e -> e.getTenantId() == null && entitySeverity.equals(e.getSeverity()))
                .collect(Collectors.toList());

        tenantCodes.addAll(globalCodes);
        return tenantCodes.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByIdAndTenant(String tenantId, String id) {
        // Check tenant-specific first
        if (repository.existsByTenantIdAndId(tenantId, id)) {
            return true;
        }

        // Check global codes
        return repository.findById(id)
                .map(entity -> entity.getTenantId() == null)
                .orElse(false);
    }

    @Override
    public long countByTenant(String tenantId) {
        return repository.findAll().stream()
                .filter(e -> tenantId.equals(e.getTenantId()) || e.getTenantId() == null)
                .count();
    }

    @Override
    public void delete(ReasonCode reasonCode) {
        repository.deleteById(reasonCode.getId());
    }

    @Override
    public void deleteById(String id) {
        repository.deleteById(id);
    }

    private Page<ReasonCode> convertToPage(List<ReasonCodeEntity> entities, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), entities.size());
        List<ReasonCode> pageContent = entities.subList(start, end)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
        return new PageImpl<>(pageContent, pageable, entities.size());
    }
}
