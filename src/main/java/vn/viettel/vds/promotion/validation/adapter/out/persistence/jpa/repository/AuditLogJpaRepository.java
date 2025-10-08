package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.AuditLogEntity;

import java.time.Instant;
import java.util.List;

/**
 * JPA Repository for AuditLogEntity.
 *
 * NOTE: The schema does NOT have tenant_id or target embeddable.
 * It has entity_type, entity_id, actor_id, action, timestamp instead.
 */
@Repository
public interface AuditLogJpaRepository extends JpaRepository<AuditLogEntity, String> {

    List<AuditLogEntity> findByActionOrderByTimestampDesc(String action);

    List<AuditLogEntity> findAllByOrderByTimestampDesc();

    @Query("SELECT a FROM AuditLogEntity a WHERE " +
            "a.timestamp BETWEEN :fromDate AND :toDate ORDER BY a.timestamp DESC")
    List<AuditLogEntity> findByDateRange(
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate
    );

    @Query("SELECT a FROM AuditLogEntity a WHERE " +
            "a.entityType = :entityType AND a.entityId = :entityId ORDER BY a.timestamp DESC")
    List<AuditLogEntity> findByEntity(
            @Param("entityType") String entityType,
            @Param("entityId") String entityId
    );

    List<AuditLogEntity> findByActorIdOrderByTimestampDesc(String actorId);
}
