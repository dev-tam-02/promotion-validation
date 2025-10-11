package vn.viettel.vds.promotion.validation.application.port.out;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.viettel.vds.promotion.validation.domain.model.Operator;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port for Operator persistence operations.
 * Abstracts database technology (MongoDB or JPA) from application layer.
 */
public interface OperatorPersistencePort {

    /**
     * Save operator
     */
    Operator save(Operator operator);

    /**
     * Find operator by ID
     */
    Optional<Operator> findById(String id);

    /**
     * Find operator by tenant, name and version
     */
    Optional<Operator> findByTenantIdAndNameAndVersion(String tenantId, String name, Integer version);

    /**
     * Find latest version of operator by tenant and name
     */
    Optional<Operator> findLatestVersion(String tenantId, String name);

    /**
     * Find operators by tenant and context
     */
    List<Operator> findByTenantIdAndContext(String tenantId, String context);

    /**
     * Find operators by tenant, context and status with pagination
     */
    Page<Operator> findByTenantIdAndContextAndStatus(String tenantId, String context,
                                                     Operator.OperatorStatus status, Pageable pageable);

    /**
     * Find active operators by tenant
     */
    List<Operator> findByTenantIdAndStatus(String tenantId, Operator.OperatorStatus status);

    /**
     * Find all versions of an operator
     */
    List<Operator> findAllVersions(String tenantId, String name);

    /**
     * Find operators with filters
     */
    Page<Operator> findWithFilters(String tenantId, String contextPattern,
                                   Operator.OperatorStatus status, Pageable pageable);

    /**
     * Check if operator exists by tenant, name and version
     */
    boolean existsByTenantIdAndNameAndVersion(String tenantId, String name, Integer version);

    /**
     * Find global operators (tenantId is null)
     */
    List<Operator> findGlobalOperatorsByStatus(Operator.OperatorStatus status);

    /**
     * Count operators by tenant and status
     */
    long countByTenantIdAndStatus(String tenantId, Operator.OperatorStatus status);

    /**
     * Delete operator
     */
    void delete(Operator operator);

    /**
     * Delete by ID
     */
    void deleteById(String id);

    /**
     * Find all operators by tenant
     */
    List<Operator> findAllByTenantId(String tenantId);
}
