package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.ReasonCodeEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReasonCodeJpaRepository extends JpaRepository<ReasonCodeEntity, String> {

    List<ReasonCodeEntity> findByTenantId(String tenantId);

    List<ReasonCodeEntity> findByTenantIdAndCategory(String tenantId, String category);

    List<ReasonCodeEntity> findByTenantIdAndSeverity(
            String tenantId,
            ReasonCodeEntity.Severity severity
    );

    Optional<ReasonCodeEntity> findByTenantIdAndId(String tenantId, String id);

    boolean existsByTenantIdAndId(String tenantId, String id);
}
