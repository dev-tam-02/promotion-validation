package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.AuditLogEntity;

import java.time.Instant;
import java.util.List;

@Repository
public interface AuditLogJpaRepository extends JpaRepository<AuditLogEntity, String> {

    List<AuditLogEntity> findByTenantIdAndActionOrderByAtDesc(
            String tenantId,
            AuditLogEntity.AuditAction action
    );

    List<AuditLogEntity> findByTenantIdOrderByAtDesc(String tenantId);

    @Query("SELECT a FROM AuditLogEntity a WHERE a.tenantId = :tenantId " +
            "AND a.at BETWEEN :fromDate AND :toDate ORDER BY a.at DESC")
    List<AuditLogEntity> findByTenantIdAndDateRange(
            @Param("tenantId") String tenantId,
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate
    );

    @Query("SELECT a FROM AuditLogEntity a WHERE a.tenantId = :tenantId " +
            "AND a.target.type = :targetType AND a.target.id = :targetId ORDER BY a.at DESC")
    List<AuditLogEntity> findByTenantIdAndTarget(
            @Param("tenantId") String tenantId,
            @Param("targetType") String targetType,
            @Param("targetId") String targetId
    );
}
