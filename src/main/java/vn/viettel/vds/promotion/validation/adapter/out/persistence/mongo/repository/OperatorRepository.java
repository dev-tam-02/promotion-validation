package vn.viettel.vds.promotion.validation.adapter.out.persistence.mongo.repository;

import com.promix.platform.autoconfigure.mongo.condition.ConditionalOnPromixMongo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import vn.viettel.vds.promotion.validation.domain.entity.Operator;

import java.util.List;
import java.util.Optional;

@Repository
@ConditionalOnPromixMongo
public interface OperatorRepository extends MongoRepository<Operator, String> {

    /**
     * Find operator by tenant, name and version
     */
    Optional<Operator> findByTenantIdAndNameAndVersion(String tenantId, String name, Integer version);

    /**
     * Find latest version of operator by tenant and name
     */
    Optional<Operator> findFirstByTenantIdAndNameOrderByVersionDesc(String tenantId, String name);

    /**
     * Find operators by tenant and context
     */
    List<Operator> findByTenantIdAndContext(String tenantId, String context);

    /**
     * Find operators by tenant, context and status
     */
    Page<Operator> findByTenantIdAndContextAndStatus(String tenantId, String context, Operator.OperatorStatus status, Pageable pageable);

    /**
     * Find active operators by tenant
     */
    List<Operator> findByTenantIdAndStatus(String tenantId, Operator.OperatorStatus status);

    /**
     * Find all versions of an operator
     */
    List<Operator> findByTenantIdAndNameOrderByVersionDesc(String tenantId, String name);

    /**
     * Find operators by tenant with optional filters
     */
    @Query("{ " +
            "$and: [ " +
            "  { $or: [ { 'tenantId': ?0 }, { 'tenantId': null } ] }, " +
            "  { $or: [ { 'context': { $exists: false } }, { 'context': { $regex: ?1, $options: 'i' } } ] }, " +
            "  { $or: [ { 'status': { $exists: false } }, { 'status': ?2 } ] } " +
            "] }")
    Page<Operator> findWithFilters(String tenantId, String contextPattern, Operator.OperatorStatus status, Pageable pageable);

    /**
     * Check if operator exists by tenant, name and version
     */
    boolean existsByTenantIdAndNameAndVersion(String tenantId, String name, Integer version);

    /**
     * Find global operators (tenantId is null)
     */
    List<Operator> findByTenantIdIsNullAndStatus(Operator.OperatorStatus status);

    /**
     * Count operators by tenant and status
     */
    long countByTenantIdAndStatus(String tenantId, Operator.OperatorStatus status);
}