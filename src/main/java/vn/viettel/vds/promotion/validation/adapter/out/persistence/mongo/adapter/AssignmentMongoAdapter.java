package vn.viettel.vds.promotion.validation.adapter.out.persistence.mongo.adapter;

import com.promix.platform.autoconfigure.mongo.condition.ConditionalOnPromixMongo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.mongo.repository.AssignmentRepository;
import vn.viettel.vds.promotion.validation.application.port.out.AssignmentPersistencePort;
import vn.viettel.vds.promotion.validation.domain.entity.Assignment;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
@ConditionalOnPromixMongo
public class AssignmentMongoAdapter implements AssignmentPersistencePort {

    private final AssignmentRepository repository;

    public AssignmentMongoAdapter(AssignmentRepository repository) {
        this.repository = repository;
    }

    @Override
    public Assignment save(Assignment assignment) {
        return repository.save(assignment);
    }

    @Override
    public Optional<Assignment> findById(String id) {
        return repository.findById(id);
    }

    @Override
    public Optional<Assignment> findByTenantIdAndSubjectTypeAndSubjectKey(String tenantId, String subjectType, String subjectKey) {
        return repository.findByTenantIdAndSubjectTypeAndSubjectKey(tenantId, subjectType, subjectKey);
    }

    @Override
    public List<Assignment> findActiveByTenantIdAndSubject(String tenantId, String subjectType, String subjectKey) {
        return repository.findActiveByTenantIdAndSubject(tenantId, subjectType, subjectKey);
    }

    @Override
    public List<Assignment> findByTenantIdAndRuleId(String tenantId, String ruleId) {
        return repository.findByTenantIdAndRuleIdOrderByAssignmentVersionDesc(tenantId, ruleId);
    }

    @Override
    public Page<Assignment> findWithFilters(String tenantId, String subjectType, String subjectKeyPattern,
                                           Boolean active, String ruleId, Pageable pageable) {
        return repository.findWithFilters(tenantId, subjectType, subjectKeyPattern, active, ruleId, pageable);
    }

    @Override
    public List<Assignment> findActiveAtTime(String tenantId, Instant time) {
        return repository.findActiveAtTime(tenantId, time);
    }

    @Override
    public List<Assignment> findOverlappingAssignments(String tenantId, String subjectType, String subjectKey,
                                                       Instant validFrom, Instant validTo) {
        return repository.findOverlappingAssignments(tenantId, subjectType, subjectKey, validFrom, validTo);
    }

    @Override
    public Page<Assignment> findByTenantIdAndRuleIdPaged(String tenantId, String ruleId, Pageable pageable) {
        return repository.findByTenantIdAndRuleId(tenantId, ruleId, pageable);
    }

    @Override
    public boolean existsActiveAssignmentForSubject(String tenantId, String subjectType, String subjectKey) {
        return repository.existsActiveAssignmentForSubject(tenantId, subjectType, subjectKey);
    }

    @Override
    public long countByTenantIdAndActive(String tenantId, Boolean active) {
        return repository.countByTenantIdAndActive(tenantId, active);
    }

    @Override
    public Optional<Assignment> findLatestVersionByRuleId(String tenantId, String ruleId) {
        return repository.findFirstByTenantIdAndRuleIdOrderByAssignmentVersionDesc(tenantId, ruleId);
    }

    @Override
    public boolean existsBySubjectTypeAndSubjectKey(String subjectType, String subjectKey) {
        return repository.existsBySubjectTypeAndSubjectKey(subjectType, subjectKey);
    }

    @Override
    public Optional<Assignment> findBySubjectTypeAndSubjectKey(String subjectType, String subjectKey) {
        return repository.findBySubjectTypeAndSubjectKey(subjectType, subjectKey);
    }

    @Override
    public List<Assignment> findAllBySubjectTypeAndSubjectKey(String subjectType, String subjectKey) {
        return repository.findAllBySubjectTypeAndSubjectKey(subjectType, subjectKey);
    }

    @Override
    public void delete(Assignment assignment) {
        repository.delete(assignment);
    }

    @Override
    public void deleteById(String id) {
        repository.deleteById(id);
    }
}
