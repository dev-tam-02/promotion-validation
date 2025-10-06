package vn.viettel.vds.promotion.validation.adapter.out.persistence.mongo.adapter;

import com.promix.platform.autoconfigure.mongo.condition.ConditionalOnPromixMongo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.mongo.repository.PublishJobRepository;
import vn.viettel.vds.promotion.validation.application.port.out.PublishJobPersistencePort;
import vn.viettel.vds.promotion.validation.domain.entity.PublishJob;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
@ConditionalOnPromixMongo
public class PublishJobMongoAdapter implements PublishJobPersistencePort {

    private final PublishJobRepository repository;

    public PublishJobMongoAdapter(PublishJobRepository repository) {
        this.repository = repository;
    }

    @Override
    public PublishJob save(PublishJob publishJob) {
        return repository.save(publishJob);
    }

    @Override
    public Optional<PublishJob> findById(String id) {
        return repository.findById(id);
    }

    @Override
    public Optional<PublishJob> findByTenantIdAndRuleIdAndTargetVersion(String tenantId, String ruleId, Integer targetVersion) {
        return repository.findByTenantIdAndRuleIdAndTargetVersion(tenantId, ruleId, targetVersion);
    }

    @Override
    public List<PublishJob> findByTenantIdAndRuleIdOrderByTargetVersionDesc(String tenantId, String ruleId) {
        return repository.findByTenantIdAndRuleIdOrderByTargetVersionDesc(tenantId, ruleId);
    }

    @Override
    public Page<PublishJob> findByTenantIdAndStatusOrderByRequestedAtDesc(String tenantId, PublishJob.JobStatus status, Pageable pageable) {
        return repository.findByTenantIdAndStatusOrderByRequestedAtDesc(tenantId, status, pageable);
    }

    @Override
    public List<PublishJob> findRunningJobsOlderThan(Instant cutoffTime) {
        return repository.findRunningJobsOlderThan(cutoffTime);
    }

    @Override
    public Optional<PublishJob> findFirstByTenantIdAndRuleIdOrderByRequestedAtDesc(String tenantId, String ruleId) {
        return repository.findFirstByTenantIdAndRuleIdOrderByRequestedAtDesc(tenantId, ruleId);
    }

    @Override
    public Page<PublishJob> findWithFilters(String tenantId, PublishJob.JobStatus status, Instant from, Instant to, Pageable pageable) {
        return repository.findWithFilters(tenantId, status, from, to, pageable);
    }

    @Override
    public long countByTenantIdAndStatus(String tenantId, PublishJob.JobStatus status) {
        return repository.countByTenantIdAndStatus(tenantId, status);
    }

    @Override
    public List<PublishJob> findJobsForCleanup(Instant cutoffTime) {
        return repository.findJobsForCleanup(cutoffTime);
    }

    @Override
    public Page<PublishJob> findByTenantIdOrderByRequestedAtDesc(String tenantId, Pageable pageable) {
        return repository.findByTenantIdOrderByRequestedAtDesc(tenantId, pageable);
    }

    @Override
    public boolean existsByTenantIdAndRuleIdAndTargetVersion(String tenantId, String ruleId, Integer targetVersion) {
        return repository.existsByTenantIdAndRuleIdAndTargetVersion(tenantId, ruleId, targetVersion);
    }

    @Override
    public void delete(PublishJob publishJob) {
        repository.delete(publishJob);
    }

    @Override
    public void deleteById(String id) {
        repository.deleteById(id);
    }
}
