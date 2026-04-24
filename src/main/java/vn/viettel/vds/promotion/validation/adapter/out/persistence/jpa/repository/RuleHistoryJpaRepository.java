package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository;

import com.promix.platform.data.jpa.autoconfigure.condition.ConditionalOnPromixJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.ValidationRuleHistoryEntity;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for the {@code validation_rules_history} table.
 */
@ConditionalOnPromixJpa
@Repository
public interface RuleHistoryJpaRepository extends JpaRepository<ValidationRuleHistoryEntity, String> {

    /**
     * Return all history entries for a rule, ordered by version ascending.
     */
    List<ValidationRuleHistoryEntity> findByRuleIdOrderByRuleVersionAsc(String ruleId);

    /**
     * Return the history entry matching a specific rule version.
     */
    Optional<ValidationRuleHistoryEntity> findByRuleIdAndRuleVersion(String ruleId, long ruleVersion);
}
