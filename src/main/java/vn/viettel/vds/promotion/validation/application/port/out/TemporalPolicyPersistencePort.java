package vn.viettel.vds.promotion.validation.application.port.out;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.viettel.vds.promotion.validation.domain.entity.TemporalPolicy;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Outbound port for TemporalPolicy persistence operations.
 */
public interface TemporalPolicyPersistencePort {

    TemporalPolicy save(TemporalPolicy temporalPolicy);

    Optional<TemporalPolicy> findById(String id);

    Optional<TemporalPolicy> findByTenantIdAndName(String tenantId, String name);

    List<TemporalPolicy> findByTenantIdAndTz(String tenantId, String tz);

    Page<TemporalPolicy> findByTenantIdAndNamePattern(String tenantId, String namePattern, Pageable pageable);

    Page<TemporalPolicy> findWithFilters(String tenantId, String tz, String namePattern, Pageable pageable);

    List<TemporalPolicy> findByTenantIdOrderByNameAsc(String tenantId);

    boolean existsByTenantIdAndName(String tenantId, String name);

    long countByTenantId(String tenantId);

    Page<TemporalPolicy> findByTenantIdAndTz(String tenantId, String tz, Pageable pageable);

    List<TemporalPolicy> findActivePoliciesAt(Instant checkTime);

    List<TemporalPolicy> findByRruleIsNotNull();

    void delete(TemporalPolicy temporalPolicy);

    void deleteById(String id);
}
