package vn.viettel.vds.promotion.validation.adapter.out.persistence.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import vn.viettel.vds.promotion.validation.domain.entity.TemporalPolicy;

import java.util.List;
import java.util.Optional;

@Repository
public interface TemporalPolicyRepository extends MongoRepository<TemporalPolicy, String> {

    /**
     * Find temporal policy by tenant and name
     */
    Optional<TemporalPolicy> findByTenantIdAndName(String tenantId, String name);

    /**
     * Find temporal policies by tenant and timezone
     */
    List<TemporalPolicy> findByTenantIdAndTz(String tenantId, String tz);

    /**
     * Find temporal policies by tenant with name pattern
     */
    @Query("{ 'tenantId': ?0, 'name': { $regex: ?1, $options: 'i' } }")
    Page<TemporalPolicy> findByTenantIdAndNamePattern(String tenantId, String namePattern, Pageable pageable);

    /**
     * Find temporal policies by tenant with optional filters
     */
    @Query("{ 'tenantId': ?0, " +
           "$and: [ " +
           "  { $or: [ { 'tz': { $exists: false } }, { 'tz': ?1 } ] }, " +
           "  { $or: [ { 'name': { $exists: false } }, { 'name': { $regex: ?2, $options: 'i' } } ] } " +
           "] }")
    Page<TemporalPolicy> findWithFilters(String tenantId, String tz, String namePattern, Pageable pageable);

    /**
     * Find all temporal policies by tenant ordered by name
     */
    List<TemporalPolicy> findByTenantIdOrderByNameAsc(String tenantId);

    /**
     * Check if temporal policy exists by tenant and name
     */
    boolean existsByTenantIdAndName(String tenantId, String name);

    /**
     * Count temporal policies by tenant
     */
    long countByTenantId(String tenantId);

    /**
     * Find temporal policies by tenant and timezone with pagination
     */
    Page<TemporalPolicy> findByTenantIdAndTz(String tenantId, String tz, Pageable pageable);
}