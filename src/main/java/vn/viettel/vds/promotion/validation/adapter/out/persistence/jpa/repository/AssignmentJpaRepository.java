package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.AssignmentEntity;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface AssignmentJpaRepository extends JpaRepository<AssignmentEntity, String> {

    @Query("SELECT a FROM AssignmentEntity a WHERE a.tenantId = :tenantId " +
            "AND a.subject.type = :subjectType AND a.subject.key = :subjectKey")
    List<AssignmentEntity> findByTenantIdAndSubject(
            @Param("tenantId") String tenantId,
            @Param("subjectType") String subjectType,
            @Param("subjectKey") String subjectKey
    );

    @Query("SELECT a FROM AssignmentEntity a WHERE a.tenantId = :tenantId " +
            "AND a.ruleId = :ruleId ORDER BY a.assignmentVersion DESC")
    List<AssignmentEntity> findByTenantIdAndRuleIdOrderByVersionDesc(
            @Param("tenantId") String tenantId,
            @Param("ruleId") String ruleId
    );

    @Query("SELECT a FROM AssignmentEntity a WHERE a.tenantId = :tenantId " +
            "AND a.active = true AND a.subject.type = :subjectType AND a.subject.key = :subjectKey")
    List<AssignmentEntity> findActiveAssignmentsBySubject(
            @Param("tenantId") String tenantId,
            @Param("subjectType") String subjectType,
            @Param("subjectKey") String subjectKey
    );

    @Query("SELECT a FROM AssignmentEntity a WHERE a.tenantId = :tenantId " +
            "AND a.active = true AND (a.validFrom IS NULL OR a.validFrom <= :now) " +
            "AND (a.validTo IS NULL OR a.validTo >= :now)")
    List<AssignmentEntity> findCurrentActiveAssignments(
            @Param("tenantId") String tenantId,
            @Param("now") Instant now
    );

    @Query("SELECT a FROM AssignmentEntity a WHERE a.tenantId = :tenantId " +
            "AND a.ruleId = :ruleId AND a.assignmentVersion = :version")
    Optional<AssignmentEntity> findByTenantIdAndRuleIdAndVersion(
            @Param("tenantId") String tenantId,
            @Param("ruleId") String ruleId,
            @Param("version") Integer version
    );

    @Query("SELECT COALESCE(MAX(a.assignmentVersion), 0) FROM AssignmentEntity a " +
            "WHERE a.tenantId = :tenantId AND a.ruleId = :ruleId")
    Integer findMaxVersionByRuleId(
            @Param("tenantId") String tenantId,
            @Param("ruleId") String ruleId
    );

    boolean existsByTenantIdAndRuleId(String tenantId, String ruleId);

    @Query("SELECT COUNT(a) FROM AssignmentEntity a WHERE a.tenantId = :tenantId AND a.active = :active")
    long countByTenantIdAndActive(@Param("tenantId") String tenantId, @Param("active") Boolean active);
}
