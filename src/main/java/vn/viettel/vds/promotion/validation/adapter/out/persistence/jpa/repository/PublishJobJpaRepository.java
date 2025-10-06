package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.PublishJobEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface PublishJobJpaRepository extends JpaRepository<PublishJobEntity, String> {

    Optional<PublishJobEntity> findByTenantIdAndRuleIdAndTargetVersion(
            String tenantId,
            String ruleId,
            Integer targetVersion
    );

    List<PublishJobEntity> findByTenantIdAndStatusOrderByRequestedAtDesc(
            String tenantId,
            PublishJobEntity.JobStatus status
    );

    List<PublishJobEntity> findByTenantIdAndRuleIdOrderByTargetVersionDesc(
            String tenantId,
            String ruleId
    );

    @Query("SELECT p FROM PublishJobEntity p WHERE p.tenantId = :tenantId " +
            "AND p.status = :status ORDER BY p.requestedAt ASC")
    List<PublishJobEntity> findPendingJobs(
            @Param("tenantId") String tenantId,
            @Param("status") PublishJobEntity.JobStatus status
    );

    long countByTenantIdAndRuleIdAndStatus(
            String tenantId,
            String ruleId,
            PublishJobEntity.JobStatus status
    );
}
