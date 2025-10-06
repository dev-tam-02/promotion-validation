package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.OperatorEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface OperatorJpaRepository extends JpaRepository<OperatorEntity, String> {

    Optional<OperatorEntity> findByTenantIdAndNameAndOperatorVersion(
            String tenantId,
            String name,
            Integer operatorVersion
    );

    List<OperatorEntity> findByTenantIdAndContextAndStatus(
            String tenantId,
            String context,
            OperatorEntity.OperatorStatus status
    );

    List<OperatorEntity> findByTenantIdAndNameOrderByOperatorVersionDesc(
            String tenantId,
            String name
    );

    @Query("SELECT o FROM OperatorEntity o WHERE o.tenantId = :tenantId " +
            "AND o.status = :status ORDER BY o.name ASC, o.operatorVersion DESC")
    List<OperatorEntity> findByTenantIdAndStatus(
            @Param("tenantId") String tenantId,
            @Param("status") OperatorEntity.OperatorStatus status
    );

    @Query("SELECT o FROM OperatorEntity o WHERE o.tenantId = :tenantId " +
            "AND o.name = :name ORDER BY o.operatorVersion DESC")
    Optional<OperatorEntity> findLatestVersionByName(
            @Param("tenantId") String tenantId,
            @Param("name") String name
    );

    @Query("SELECT COALESCE(MAX(o.operatorVersion), 0) FROM OperatorEntity o " +
            "WHERE o.tenantId = :tenantId AND o.name = :name")
    Integer findMaxVersionByName(
            @Param("tenantId") String tenantId,
            @Param("name") String name
    );

    boolean existsByTenantIdAndNameAndOperatorVersion(String tenantId, String name, Integer operatorVersion);

    List<OperatorEntity> findByTenantIdOrderByNameAscOperatorVersionDesc(String tenantId);
}
