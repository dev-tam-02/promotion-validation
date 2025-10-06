package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.RuleTemporalLinkEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface RuleTemporalLinkJpaRepository extends JpaRepository<RuleTemporalLinkEntity, String> {

    List<RuleTemporalLinkEntity> findByValidationRule_Id(String ruleId);

    List<RuleTemporalLinkEntity> findByTemporalPolicy_Id(String policyId);

    Optional<RuleTemporalLinkEntity> findByValidationRule_IdAndTemporalPolicy_Id(String ruleId, String policyId);

    void deleteByValidationRule_IdAndTemporalPolicy_Id(String ruleId, String policyId);

    boolean existsByValidationRule_IdAndTemporalPolicy_Id(String ruleId, String policyId);

    long countByValidationRule_Id(String ruleId);
}
