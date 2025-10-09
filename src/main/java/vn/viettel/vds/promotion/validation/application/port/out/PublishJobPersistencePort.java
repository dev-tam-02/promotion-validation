package vn.viettel.vds.promotion.validation.application.port.out;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.viettel.vds.promotion.validation.domain.model.PublishJob;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PublishJobPersistencePort {
    PublishJob save(PublishJob job);
    Optional<PublishJob> findById(String id);
    Optional<PublishJob> findByRuleIdAndTargetVersion(String ruleId, Integer targetVersion);
    List<PublishJob> findByRuleIdOrderByTargetVersionDesc(String ruleId);
    Page<PublishJob> findByStatusOrderByRequestedAtDesc(PublishJob.JobStatus status, Pageable pageable);
    List<PublishJob> findRunningJobsOlderThan(Instant cutoffTime);
    Optional<PublishJob> findFirstByRuleIdOrderByRequestedAtDesc(String ruleId);
    Page<PublishJob> findWithFilters(PublishJob.JobStatus status, Instant from, Instant to, Pageable pageable);
    long countByStatus(PublishJob.JobStatus status);
    List<PublishJob> findJobsForCleanup(Instant cutoffTime);
    Page<PublishJob> findAllOrderByRequestedAtDesc(Pageable pageable);
    boolean existsByRuleIdAndTargetVersion(String ruleId, Integer targetVersion);
    void delete(PublishJob job);
    void deleteById(String id);
}
