package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository;

import com.promix.platform.data.jpa.autoconfigure.condition.ConditionalOnPromixJpa;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.ValidationRuleEntity;

import java.util.List;
import java.util.Optional;

@ConditionalOnPromixJpa
@Repository
@Primary
public interface ValidationRuleJpaRepository extends JpaRepository<ValidationRuleEntity, String> {

    /**
     * Find validation rule by ID with nodes eagerly loaded.
     */
    @Override
    @EntityGraph(attributePaths = {"nodes"})
    Optional<ValidationRuleEntity> findById(String id);

    /**
     * Find validation rule by code.
     */
    Optional<ValidationRuleEntity> findByCode(String code);

    /**
     * Find validation rules by state.
     */
    List<ValidationRuleEntity> findByState(String state);

    /**
     * Find validation rules by state ordered by version desc.
     */
    List<ValidationRuleEntity> findByStateOrderByVersionDesc(String state);

    /**
     * Find latest version of validation rule by code.
     */
    @Query("SELECT v FROM ValidationRuleEntity v WHERE v.code = :code ORDER BY v.version DESC")
    Optional<ValidationRuleEntity> findLatestVersionByCode(@Param("code") String code);

    /**
     * Find published validation rules.
     */
    @Query("SELECT v FROM ValidationRuleEntity v WHERE v.state = 'published' ORDER BY v.publishedAt DESC")
    List<ValidationRuleEntity> findPublishedRules();

    /**
     * Find validation rules by code and state.
     */
    List<ValidationRuleEntity> findByCodeAndState(String code, String state);

    /**
     * Check if code exists.
     */
    boolean existsByCode(String code);
}