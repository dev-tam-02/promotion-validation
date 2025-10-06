package vn.viettel.vds.promotion.validation.adapter.out.persistence.mongo.repository;

import com.promix.platform.autoconfigure.mongo.condition.ConditionalOnPromixMongo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.mongo.entity.RuleDocument;

import java.util.List;
import java.util.Optional;

/**
 * MongoDB repository for RuleDocument
 */
@Repository
@ConditionalOnPromixMongo
public interface RuleDocumentRepository extends MongoRepository<RuleDocument, String> {

    /**
     * Find rule by tenant and code
     */
    Optional<RuleDocument> findByTenantIdAndCode(String tenantId, String code);

    /**
     * Find rules by tenant ID
     */
    List<RuleDocument> findByTenantId(String tenantId);

    /**
     * Find rules by tenant ID with pagination
     */
    Page<RuleDocument> findByTenantId(String tenantId, Pageable pageable);

    /**
     * Find rules by tenant ID and status
     */
    List<RuleDocument> findByTenantIdAndStatus(String tenantId, String status);

    /**
     * Find rules by tenant ID and status with pagination
     */
    Page<RuleDocument> findByTenantIdAndStatus(String tenantId, String status, Pageable pageable);

    /**
     * Find rules by tenant ID and name containing (case insensitive)
     */
    Page<RuleDocument> findByTenantIdAndNameContainingIgnoreCase(String tenantId, String name, Pageable pageable);

    /**
     * Find rules by tenant ID, status and name containing (case insensitive)
     */
    Page<RuleDocument> findByTenantIdAndStatusAndNameContainingIgnoreCase(
            String tenantId,
            String status,
            String name,
            Pageable pageable
    );

    /**
     * Check if rule exists by tenant and code
     */
    boolean existsByTenantIdAndCode(String tenantId, String code);

    /**
     * Count rules by tenant and status
     */
    long countByTenantIdAndStatus(String tenantId, String status);

    /**
     * Find rules by tenant ordered by created date desc
     */
    List<RuleDocument> findByTenantIdOrderByCreatedAtDesc(String tenantId);

    /**
     * Find rules by status across all tenants
     */
    List<RuleDocument> findByStatus(String status);
}