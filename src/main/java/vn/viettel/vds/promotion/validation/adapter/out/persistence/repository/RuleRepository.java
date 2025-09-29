package vn.viettel.vds.promotion.validation.adapter.out.persistence.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import vn.viettel.vds.promotion.validation.domain.entity.Rule;

import java.util.List;
import java.util.Optional;

@Repository
public interface RuleRepository extends MongoRepository<Rule, String> {

    /**
     * Find rule by tenant and code
     */
    Optional<Rule> findByTenantIdAndCode(String tenantId, String code);

    /**
     * Find rules by tenant and state with pagination
     */
    Page<Rule> findByTenantIdAndState(String tenantId, Rule.RuleState state, Pageable pageable);

    /**
     * Find rules by tenant with optional filters
     */
    @Query("{ 'tenantId': ?0, " +
           "$and: [ " +
           "  { $or: [ { 'state': { $exists: false } }, { 'state': ?1 } ] }, " +
           "  { $or: [ { 'code': { $exists: false } }, { 'code': { $regex: ?2, $options: 'i' } } ] }, " +
           "  { $or: [ { 'name': { $exists: false } }, { 'name': { $regex: ?3, $options: 'i' } } ] } " +
           "] }")
    Page<Rule> findByTenantIdWithFilters(String tenantId, Rule.RuleState state, String codePattern, String namePattern, Pageable pageable);

    /**
     * Find all rules by tenant ordered by update time
     */
    List<Rule> findByTenantIdOrderByUpdatedAtDesc(String tenantId);

    /**
     * Check if rule exists by tenant and code
     */
    boolean existsByTenantIdAndCode(String tenantId, String code);

    /**
     * Count rules by tenant and state
     */
    long countByTenantIdAndState(String tenantId, Rule.RuleState state);

    /**
     * Find rules by tenant and state not equal
     */
    List<Rule> findByTenantIdAndStateNot(String tenantId, Rule.RuleState state);
}