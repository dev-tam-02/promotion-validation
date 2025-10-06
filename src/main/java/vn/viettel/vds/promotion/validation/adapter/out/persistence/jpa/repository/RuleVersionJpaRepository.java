package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.RuleVersionEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface RuleVersionJpaRepository extends JpaRepository<RuleVersionEntity, String> {

    Optional<RuleVersionEntity> findByTenantIdAndRuleIdAndRuleVersion(
            String tenantId,
            String ruleId,
            Integer ruleVersion
    );

    Optional<RuleVersionEntity> findByTenantIdAndCodeAndRuleVersion(
            String tenantId,
            String code,
            Integer ruleVersion
    );

    List<RuleVersionEntity> findByTenantIdAndRuleIdOrderByRuleVersionDesc(
            String tenantId,
            String ruleId
    );

    @Query("SELECT rv FROM RuleVersionEntity rv WHERE rv.tenantId = :tenantId " +
            "AND rv.ruleId = :ruleId ORDER BY rv.ruleVersion DESC")
    List<RuleVersionEntity> findAllVersionsByRule(
            @Param("tenantId") String tenantId,
            @Param("ruleId") String ruleId
    );

    @Query("SELECT rv FROM RuleVersionEntity rv WHERE rv.tenantId = :tenantId " +
            "AND rv.compile.bundleHash = :bundleHash")
    Optional<RuleVersionEntity> findByBundleHash(
            @Param("tenantId") String tenantId,
            @Param("bundleHash") String bundleHash
    );

    @Query("SELECT COALESCE(MAX(rv.ruleVersion), 0) FROM RuleVersionEntity rv " +
            "WHERE rv.tenantId = :tenantId AND rv.ruleId = :ruleId")
    Integer findMaxVersionByRuleId(
            @Param("tenantId") String tenantId,
            @Param("ruleId") String ruleId
    );

    boolean existsByTenantIdAndRuleIdAndRuleVersion(
            String tenantId,
            String ruleId,
            Integer ruleVersion
    );
}
