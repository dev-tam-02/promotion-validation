package vn.viettel.vds.promotion.validation.adapter.out.persistence.mongo.repository;

import com.promix.platform.autoconfigure.mongo.condition.ConditionalOnPromixMongo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import vn.viettel.vds.promotion.validation.domain.entity.PublishJob;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@ConditionalOnPromixMongo
@Repository
public interface PublishJobRepository extends MongoRepository<PublishJob, String> {

    /**
     * Find publish job by tenant, rule ID and target version
     */
    Optional<PublishJob> findByTenantIdAndRuleIdAndTargetVersion(String tenantId, String ruleId, Integer targetVersion);

    /**
     * Find publish jobs by tenant and rule ID ordered by target version
     */
    List<PublishJob> findByTenantIdAndRuleIdOrderByTargetVersionDesc(String tenantId, String ruleId);

    /**
     * Find publish jobs by tenant and status
     */
    Page<PublishJob> findByTenantIdAndStatusOrderByRequestedAtDesc(String tenantId, PublishJob.JobStatus status, Pageable pageable);

    /**
     * Find running jobs older than specified time (for timeout detection)
     */
    @Query("{ 'status': 'RUNNING', 'requestedAt': { $lt: ?0 } }")
    List<PublishJob> findRunningJobsOlderThan(Instant cutoffTime);

    /**
     * Find latest job for rule
     */
    Optional<PublishJob> findFirstByTenantIdAndRuleIdOrderByRequestedAtDesc(String tenantId, String ruleId);

    /**
     * Find jobs by tenant with status and time filters
     */
    @Query("{ 'tenantId': ?0, " +
            "$and: [ " +
            "  { $or: [ { 'status': { $exists: false } }, { 'status': ?1 } ] }, " +
            "  { $or: [ { 'requestedAt': { $exists: false } }, { 'requestedAt': { $gte: ?2, $lte: ?3 } } ] } " +
            "] }")
    Page<PublishJob> findWithFilters(String tenantId, PublishJob.JobStatus status, Instant from, Instant to, Pageable pageable);

    /**
     * Count jobs by tenant and status
     */
    long countByTenantIdAndStatus(String tenantId, PublishJob.JobStatus status);

    /**
     * Find jobs for cleanup (completed jobs older than retention period)
     */
    @Query("{ 'status': { $in: ['SUCCESS', 'FAILED'] }, 'completedAt': { $lt: ?0 } }")
    List<PublishJob> findJobsForCleanup(Instant cutoffTime);

    /**
     * Find all jobs by tenant ordered by requested time
     */
    Page<PublishJob> findByTenantIdOrderByRequestedAtDesc(String tenantId, Pageable pageable);

    /**
     * Check if job exists for rule and version
     */
    boolean existsByTenantIdAndRuleIdAndTargetVersion(String tenantId, String ruleId, Integer targetVersion);
}