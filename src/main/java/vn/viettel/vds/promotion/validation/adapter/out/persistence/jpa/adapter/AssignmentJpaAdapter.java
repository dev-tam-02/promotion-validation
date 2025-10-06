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
import vn.viettel.vds.promotion.validation.domain.entity.Assignment;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
        List<AssignmentEntity> entities = repository.findByTenantIdAndSubject(tenantId, subjectType, subjectKey);
        return entities.stream().findFirst().map(mapper::toDomain);
    }

    @Override
    public List<Assignment> findActiveByTenantIdAndSubject(String tenantId, String subjectType, String subjectKey) {
        List<AssignmentEntity> entities = repository.findActiveAssignmentsBySubject(tenantId, subjectType, subjectKey);
        return entities.stream().map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Assignment> findByTenantIdAndRuleId(String tenantId, String ruleId) {
        List<AssignmentEntity> entities = repository.findByTenantIdAndRuleIdOrderByVersionDesc(tenantId, ruleId);
        return entities.stream().map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public Page<Assignment> findWithFilters(String tenantId, String subjectType, String subjectKeyPattern,
                                           Boolean active, String ruleId, Pageable pageable) {
        // Simplified filtering - can be enhanced with Specifications
        List<AssignmentEntity> all = repository.findAll();
        List<AssignmentEntity> filtered = all.stream()
                .filter(e -> e.getTenantId().equals(tenantId))
                .filter(e -> subjectType == null ||
                        (e.getSubject() != null && e.getSubject().getType().equals(subjectType)))
                .filter(e -> subjectKeyPattern == null ||
                        (e.getSubject() != null && e.getSubject().getKey().contains(subjectKeyPattern)))
                .filter(e -> active == null || e.getActive().equals(active))
                .filter(e -> ruleId == null || e.getRuleId().equals(ruleId))
                .collect(Collectors.toList());
        return convertToPage(filtered, pageable);
    }

    @Override
    public List<Assignment> findActiveAtTime(String tenantId, Instant time) {
        List<AssignmentEntity> entities = repository.findCurrentActiveAssignments(tenantId, time);
        return entities.stream().map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Assignment> findOverlappingAssignments(String tenantId, String subjectType, String subjectKey,
                                                       Instant validFrom, Instant validTo) {
        // JPA repo doesn't have this method - implement with filters
        List<AssignmentEntity> active = repository.findActiveAssignmentsBySubject(tenantId, subjectType, subjectKey);
        return active.stream()
                .filter(e -> e.getValidFrom() == null || e.getValidFrom().isBefore(validTo))
                .filter(e -> e.getValidTo() == null || e.getValidTo().isAfter(validFrom))
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Page<Assignment> findByTenantIdAndRuleIdPaged(String tenantId, String ruleId, Pageable pageable) {
        List<AssignmentEntity> entities = repository.findByTenantIdAndRuleIdOrderByVersionDesc(tenantId, ruleId);
        return convertToPage(entities, pageable);
    }

    @Override
    public boolean existsActiveAssignmentForSubject(String tenantId, String subjectType, String subjectKey) {
        return !repository.findActiveAssignmentsBySubject(tenantId, subjectType, subjectKey).isEmpty();
    }

    @Override
    public long countByTenantIdAndActive(String tenantId, Boolean active) {
        return repository.countByTenantIdAndActive(tenantId, active);
    }

    @Override
    public Optional<Assignment> findLatestVersionByRuleId(String tenantId, String ruleId) {
        List<AssignmentEntity> entities = repository.findByTenantIdAndRuleIdOrderByVersionDesc(tenantId, ruleId);
        return entities.stream().findFirst().map(mapper::toDomain);
    }

    @Override
    public boolean existsBySubjectTypeAndSubjectKey(String subjectType, String subjectKey) {
        // Need to implement custom query or search all
        return repository.findAll().stream()
                .anyMatch(e -> e.getSubject() != null &&
                        e.getSubject().getType().equals(subjectType) &&
                        e.getSubject().getKey().equals(subjectKey));
    }

    @Override
    public Optional<Assignment> findBySubjectTypeAndSubjectKey(String subjectType, String subjectKey) {
        return repository.findAll().stream()
                .filter(e -> e.getSubject() != null &&
                        e.getSubject().getType().equals(subjectType) &&
                        e.getSubject().getKey().equals(subjectKey))
                .findFirst()
                .map(mapper::toDomain);
    }

    @Override
    public List<Assignment> findAllBySubjectTypeAndSubjectKey(String subjectType, String subjectKey) {
        return repository.findAll().stream()
                .filter(e -> e.getSubject() != null &&
                        e.getSubject().getType().equals(subjectType) &&
                        e.getSubject().getKey().equals(subjectKey))
                .map(mapper::toDomain)
                .collect(Collectors.toList());
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
        int end = Math.min((start + pageable.getPageSize()), entities.size());
        List<Assignment> pageContent = entities.subList(start, end)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
        return new PageImpl<>(pageContent, pageable, entities.size());
    }
}
