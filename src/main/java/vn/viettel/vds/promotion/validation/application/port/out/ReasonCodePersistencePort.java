package vn.viettel.vds.promotion.validation.application.port.out;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.viettel.vds.promotion.validation.domain.model.ReasonCode;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port for ReasonCode persistence operations.
 */
public interface ReasonCodePersistencePort {

    ReasonCode save(ReasonCode reasonCode);

    Optional<ReasonCode> findByIdAndTenant(String tenantId, String id);

    List<ReasonCode> findByTenantAndCategory(String tenantId, String category);

    Page<ReasonCode> findByTenant(String tenantId, Pageable pageable);

    Page<ReasonCode> findWithFilters(String tenantId, String categoryPattern,
                                     ReasonCode.Severity severity, Pageable pageable);

    List<ReasonCode> findByTenantIdIsNull();

    List<ReasonCode> findByTenantId(String tenantId);

    List<ReasonCode> findByTenantAndSeverity(String tenantId, ReasonCode.Severity severity);

    boolean existsByIdAndTenant(String tenantId, String id);

    long countByTenant(String tenantId);

    void delete(ReasonCode reasonCode);

    void deleteById(String id);
}
