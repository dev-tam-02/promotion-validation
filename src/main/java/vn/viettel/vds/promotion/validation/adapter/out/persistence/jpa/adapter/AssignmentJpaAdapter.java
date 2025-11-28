package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.adapter;

import com.promix.platform.data.jpa.autoconfigure.condition.ConditionalOnPromixJpa;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.AssignmentEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.mapper.AssignmentEntityMapper;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.AssignmentJpaRepository;
import vn.viettel.vds.promotion.validation.application.port.out.AssignmentPersistencePort;
import vn.viettel.vds.promotion.validation.domain.model.Assignment;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
@ConditionalOnPromixJpa
public class AssignmentJpaAdapter implements AssignmentPersistencePort {

    private final AssignmentJpaRepository repository;
    private final AssignmentEntityMapper mapper;

    public AssignmentJpaAdapter(AssignmentJpaRepository repository, AssignmentEntityMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Assignment save(Assignment assignment) {
        AssignmentEntity entity = mapper.toEntity(assignment);
        AssignmentEntity saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Assignment> findById(String id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Assignment> findByTenantIdAndSubjectTypeAndSubjectKey(String tenantId, String subjectType, String subjectKey) {
        // Note: tenantId removed from schema, using entityType/entityId for subject
        List<AssignmentEntity> entities = repository.findByEntityTypeAndEntityId(subjectType, subjectKey);
        return entities.stream().findFirst().map(mapper::toDomain);
    }

    @Override
    public List<Assignment> findActiveByTenantIdAndSubject(String tenantId, String subjectType, String subjectKey) {
        // Note: tenantId removed from schema, using entityType/entityId and active flag
        List<AssignmentEntity> entities = repository.findByEntityTypeAndEntityIdAndActive(subjectType, subjectKey, true);
        return entities.stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Assignment> findByTenantIdAndRuleId(String tenantId, String ruleId) {
        // Note: tenantId removed, only filtering by ruleId
        List<AssignmentEntity> entities = repository.findByRuleIdOrderByCreatedAtDesc(ruleId);
        return entities.stream().map(mapper::toDomain).toList();
    }

    @Override
    public Page<Assignment> findWithFilters(String tenantId, String subjectType, String subjectKeyPattern,
                                            Boolean active, String ruleId, Pageable pageable) {
        // Simplified filtering - tenantId removed from schema
        List<AssignmentEntity> all = repository.findAll();
        List<AssignmentEntity> filtered = all.stream()
                .filter(e -> subjectType == null || subjectType.equals(e.getEntityType()))
                .filter(e -> subjectKeyPattern == null ||
                        (e.getEntityId() != null && e.getEntityId().contains(subjectKeyPattern)))
                .filter(e -> active == null || e.getActive().equals(active))
                .filter(e -> ruleId == null || e.getId().equals(ruleId))
                .toList();
        return convertToPage(filtered, pageable);
    }

    @Override
    public List<Assignment> findActiveAtTime(String tenantId, Instant time) {
        // Note: validFrom/validTo removed from schema, only checking active flag
        List<AssignmentEntity> entities = repository.findByActive(true);
        return entities.stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Assignment> findOverlappingAssignments(String tenantId, String subjectType, String subjectKey,
                                                       Instant validFrom, Instant validTo) {
        // Note: validFrom/validTo removed from schema, returning active assignments by subject
        List<AssignmentEntity> active = repository.findByEntityTypeAndEntityIdAndActive(subjectType, subjectKey, true);
        return active.stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Page<Assignment> findByTenantIdAndRuleIdPaged(String tenantId, String ruleId, Pageable pageable) {
        // Note: tenantId removed from schema
        List<AssignmentEntity> entities = repository.findByRuleIdOrderByCreatedAtDesc(ruleId);
        return convertToPage(entities, pageable);
    }

    @Override
    public boolean existsActiveAssignmentForSubject(String tenantId, String subjectType, String subjectKey) {
        // Note: tenantId removed from schema
        return !repository.findByEntityTypeAndEntityIdAndActive(subjectType, subjectKey, true).isEmpty();
    }

    @Override
    public long countByTenantIdAndActive(String tenantId, Boolean active) {
        // Note: tenantId removed from schema
        return repository.countByActive(active);
    }

    @Override
    public Optional<Assignment> findLatestVersionByRuleId(String tenantId, String ruleId) {
        // Note: tenantId and version removed from schema, using createdAt for ordering
        List<AssignmentEntity> entities = repository.findByRuleIdOrderByCreatedAtDesc(ruleId);
        return entities.stream().findFirst().map(mapper::toDomain);
    }

    @Override
    public boolean existsBySubjectTypeAndSubjectKey(String subjectType, String subjectKey) {
        // Using new entityType/entityId fields
        return repository.findAll().stream()
                .anyMatch(e -> subjectType.equals(e.getEntityType()) &&
                        subjectKey.equals(e.getEntityId()));
    }

    @Override
    public Optional<Assignment> findBySubjectTypeAndSubjectKey(String subjectType, String subjectKey) {
        // Using new entityType/entityId fields
        return repository.findAll().stream()
                .filter(e -> subjectType.equals(e.getEntityType()) &&
                        subjectKey.equals(e.getEntityId()))
                .findFirst()
                .map(mapper::toDomain);
    }

    @Override
    public List<Assignment> findAllBySubjectTypeAndSubjectKey(String subjectType, String subjectKey) {
        // Using new entityType/entityId fields
        return repository.findAll().stream()
                .filter(e -> subjectType.equals(e.getEntityType()) &&
                        subjectKey.equals(e.getEntityId()))
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public void delete(Assignment assignment) {
        repository.deleteById(assignment.getId());
    }

    @Override
    public void deleteById(String id) {
        repository.deleteById(id);
    }

    private Page<Assignment> convertToPage(List<AssignmentEntity> entities, Pageable pageable) {
        int start = (int) pageable.getOffset();
        // Handle case when start is beyond the list size (return empty page)
        if (start >= entities.size()) {
            return new PageImpl<>(List.of(), pageable, entities.size());
        }
        int end = Math.min((start + pageable.getPageSize()), entities.size());
        List<Assignment> pageContent = entities.subList(start, end)
                .stream()
                .map(mapper::toDomain)
                .toList();
        return new PageImpl<>(pageContent, pageable, entities.size());
    }
}
