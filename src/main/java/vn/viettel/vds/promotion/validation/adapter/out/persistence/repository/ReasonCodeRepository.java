package vn.viettel.vds.promotion.validation.adapter.out.persistence.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import vn.viettel.vds.promotion.validation.domain.entity.ReasonCode;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReasonCodeRepository extends MongoRepository<ReasonCode, String> {

    /**
     * Find reason code by ID and tenant (including global codes)
     */
    @Query("{ '_id': ?1, $or: [ { 'tenantId': ?0 }, { 'tenantId': null } ] }")
    Optional<ReasonCode> findByIdAndTenant(String tenantId, String id);

    /**
     * Find reason codes by tenant and category (including global codes)
     */
    @Query("{ $or: [ { 'tenantId': ?0 }, { 'tenantId': null } ], 'category': ?1 }")
    List<ReasonCode> findByTenantAndCategory(String tenantId, String category);

    /**
     * Find reason codes by tenant (including global codes)
     */
    @Query("{ $or: [ { 'tenantId': ?0 }, { 'tenantId': null } ] }")
    Page<ReasonCode> findByTenant(String tenantId, Pageable pageable);

    /**
     * Find reason codes with filters
     */
    @Query("{ " +
           "$and: [ " +
           "  { $or: [ { 'tenantId': ?0 }, { 'tenantId': null } ] }, " +
           "  { $or: [ { 'category': { $exists: false } }, { 'category': { $regex: ?1, $options: 'i' } } ] }, " +
           "  { $or: [ { 'severity': { $exists: false } }, { 'severity': ?2 } ] } " +
           "] }")
    Page<ReasonCode> findWithFilters(String tenantId, String categoryPattern, ReasonCode.Severity severity, Pageable pageable);

    /**
     * Find global reason codes (tenantId is null)
     */
    List<ReasonCode> findByTenantIdIsNull();

    /**
     * Find reason codes by tenant only (excluding global)
     */
    List<ReasonCode> findByTenantId(String tenantId);

    /**
     * Find reason codes by severity
     */
    @Query("{ $or: [ { 'tenantId': ?0 }, { 'tenantId': null } ], 'severity': ?1 }")
    List<ReasonCode> findByTenantAndSeverity(String tenantId, ReasonCode.Severity severity);

    /**
     * Check if reason code exists for tenant (including global)
     */
    @Query(value = "{ '_id': ?1, $or: [ { 'tenantId': ?0 }, { 'tenantId': null } ] }", exists = true)
    boolean existsByIdAndTenant(String tenantId, String id);

    /**
     * Count reason codes by tenant (including global)
     */
    @Query(value = "{ $or: [ { 'tenantId': ?0 }, { 'tenantId': null } ] }", count = true)
    long countByTenant(String tenantId);
}