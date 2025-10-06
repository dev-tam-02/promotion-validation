package vn.viettel.vds.promotion.validation.adapter.out.persistence.mongo.repository;

import com.promix.platform.autoconfigure.mongo.condition.ConditionalOnPromixMongo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.mongo.entity.ValidationRule;

import java.util.List;
import java.util.Optional;

@Repository
@ConditionalOnPromixMongo
public interface ValidationRuleMongoRepository extends MongoRepository<ValidationRule, String> {

    /**
     * Find rule by code
     */
    Optional<ValidationRule> findByCode(String code);

    /**
     * Find rules by state
     */
    List<ValidationRule> findByState(String state);

    /**
     * Find rules by state with pagination
     */
    Page<ValidationRule> findByState(String state, Pageable pageable);

    /**
     * Find published rules by version
     */
    List<ValidationRule> findByStateAndVersionGreaterThan(String state, Integer version);

    /**
     * Check if rule exists by code
     */
    boolean existsByCode(String code);

    /**
     * Find latest version by code
     */
    Optional<ValidationRule> findTopByCodeOrderByVersionDesc(String code);

    /**
     * Find rules by state ordered by version
     */
    List<ValidationRule> findByStateOrderByVersionDesc(String state);
}