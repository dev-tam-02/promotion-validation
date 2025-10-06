package vn.viettel.vds.promotion.validation.adapter.out.persistence.mongo.repository;

import com.promix.platform.autoconfigure.mongo.condition.ConditionalOnPromixMongo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import vn.viettel.vds.promotion.validation.domain.entity.RuleVersion;

import java.util.List;
import java.util.Optional;

@ConditionalOnPromixMongo
@Repository
public interface RuleVersionRepository extends MongoRepository<RuleVersion, String> {

    /**
     * Find rule version by tenant, rule ID and version
     */
    Optional<RuleVersion> findByTenantIdAndRuleIdAndVersion(String tenantId, String ruleId, Integer version);

    /**
     * Find all versions for a rule ordered by version descending
     */
    List<RuleVersion> findByTenantIdAndRuleIdOrderByVersionDesc(String tenantId, String ruleId);

    /**
     * Find latest version for a rule
     */
    Optional<RuleVersion> findFirstByTenantIdAndRuleIdOrderByVersionDesc(String tenantId, String ruleId);

    /**
     * Find versions by tenant and rule with pagination
     */
    Page<RuleVersion> findByTenantIdAndRuleId(String tenantId, String ruleId, Pageable pageable);

    /**
     * Find rule version by bundle hash
     */
    @Query("{ 'tenantId': ?0, 'compile.bundleHash': ?1 }")
    Optional<RuleVersion> findByTenantIdAndBundleHash(String tenantId, String bundleHash);

    /**
     * Find rule versions by tenant and code with version ordering
     */
    List<RuleVersion> findByTenantIdAndCodeOrderByVersionDesc(String tenantId, String code);

    /**
     * Get next version number for a rule
     */
    @Query(value = "{ 'tenantId': ?0, 'ruleId': ?1 }",
            fields = "{ 'version': 1 }",
            sort = "{ 'version': -1 }")
    Optional<RuleVersion> findTopVersionByTenantIdAndRuleId(String tenantId, String ruleId);

    /**
     * Find rule versions with specific operators fingerprint
     */
    @Query("{ 'tenantId': ?0, 'operatorsFingerprint': ?1 }")
    List<RuleVersion> findByTenantIdAndOperatorsFingerprint(String tenantId, String operatorsFingerprint);

    /**
     * Count versions for a rule
     */
    long countByTenantIdAndRuleId(String tenantId, String ruleId);
}