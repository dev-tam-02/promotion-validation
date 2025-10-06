package vn.viettel.vds.promotion.validation.application.port.out;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.viettel.vds.promotion.validation.domain.model.PublishJob;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Outbound port for PublishJob persistence operations.
 */
public interface PublishJobPersistencePort {

    PublishJob save(PublishJob publishJob);

    Optional<PublishJob> findById(String id);

    Optional<PublishJob> findByTenantIdAndRuleIdAndTargetVersion(String tenantId, String ruleId, Integer targetVersion);

    List<PublishJob> findByTenantIdAndRuleIdOrderByTargetVersionDesc(String tenantId, String ruleId);

    Page<PublishJob> findByTenantIdAndStatusOrderByRequestedAtDesc(String tenantId, PublishJob.JobStatus status, Pageable pageable);

    List<PublishJob> findRunningJobsOlderThan(Instant cutoffTime);

    Optional<PublishJob> findFirstByTenantIdAndRuleIdOrderByRequestedAtDesc(String tenantId, String ruleId);

    Page<PublishJob> findWithFilters(String tenantId, PublishJob.JobStatus status, Instant from, Instant to, Pageable pageable);

    long countByTenantIdAndStatus(String tenantId, PublishJob.JobStatus status);

    List<PublishJob> findJobsForCleanup(Instant cutoffTime);

    Page<PublishJob> findByTenantIdOrderByRequestedAtDesc(String tenantId, Pageable pageable);

    boolean existsByTenantIdAndRuleIdAndTargetVersion(String tenantId, String ruleId, Integer targetVersion);

    void delete(PublishJob publishJob);

    void deleteById(String id);
}
