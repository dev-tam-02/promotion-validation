package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.RuleTemporalLinkEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface RuleTemporalLinkJpaRepository extends JpaRepository<RuleTemporalLinkEntity, String> {

    @Query("SELECT rtl FROM RuleTemporalLinkEntity rtl WHERE rtl.validationRule.id = :ruleId")
    List<RuleTemporalLinkEntity> findByValidationRuleId(@Param("ruleId") String ruleId);

    @Query("SELECT rtl FROM RuleTemporalLinkEntity rtl WHERE rtl.temporalPolicy.id = :policyId")
    List<RuleTemporalLinkEntity> findByTemporalPolicyId(@Param("policyId") String policyId);

    @Query("SELECT rtl FROM RuleTemporalLinkEntity rtl WHERE rtl.validationRule.id = :ruleId AND rtl.temporalPolicy.id = :policyId")
    Optional<RuleTemporalLinkEntity> findByValidationRuleIdAndTemporalPolicyId(@Param("ruleId") String ruleId, @Param("policyId") String policyId);

    @Query("DELETE FROM RuleTemporalLinkEntity rtl WHERE rtl.validationRule.id = :ruleId AND rtl.temporalPolicy.id = :policyId")
    void deleteByValidationRuleIdAndTemporalPolicyId(@Param("ruleId") String ruleId, @Param("policyId") String policyId);

    @Query("SELECT CASE WHEN COUNT(rtl) > 0 THEN true ELSE false END FROM RuleTemporalLinkEntity rtl WHERE rtl.validationRule.id = :ruleId AND rtl.temporalPolicy.id = :policyId")
    boolean existsByValidationRuleIdAndTemporalPolicyId(@Param("ruleId") String ruleId, @Param("policyId") String policyId);

    @Query("SELECT COUNT(rtl) FROM RuleTemporalLinkEntity rtl WHERE rtl.validationRule.id = :ruleId")
    long countByValidationRuleId(@Param("ruleId") String ruleId);
}
