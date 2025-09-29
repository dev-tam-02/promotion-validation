package vn.viettel.vds.promotion.validation.adapter.out.persistence.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import vn.viettel.vds.promotion.validation.domain.entity.Assignment;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface AssignmentRepository extends MongoRepository<Assignment, String> {

    /**
     * Find assignment by tenant and subject
     */
    Optional<Assignment> findByTenantIdAndSubjectTypeAndSubjectKey(String tenantId, String subjectType, String subjectKey);

    /**
     * Find active assignments by tenant and subject
     */
    @Query("{ 'tenantId': ?0, 'subject.type': ?1, 'subject.key': ?2, 'active': true }")
    List<Assignment> findActiveByTenantIdAndSubject(String tenantId, String subjectType, String subjectKey);

    /**
     * Find assignments by tenant and rule ID
     */
    List<Assignment> findByTenantIdAndRuleIdOrderByAssignmentVersionDesc(String tenantId, String ruleId);

    /**
     * Find assignments by tenant with optional filters
     */
    @Query("{ 'tenantId': ?0, " +
           "$and: [ " +
           "  { $or: [ { 'subject.type': { $exists: false } }, { 'subject.type': ?1 } ] }, " +
           "  { $or: [ { 'subject.key': { $exists: false } }, { 'subject.key': { $regex: ?2, $options: 'i' } } ] }, " +
           "  { $or: [ { 'active': { $exists: false } }, { 'active': ?3 } ] }, " +
           "  { $or: [ { 'ruleId': { $exists: false } }, { 'ruleId': ?4 } ] } " +
           "] }")
    Page<Assignment> findWithFilters(String tenantId, String subjectType, String subjectKeyPattern, Boolean active, String ruleId, Pageable pageable);

    /**
     * Find active assignments valid at specific time
     */
    @Query("{ 'tenantId': ?0, 'active': true, " +
           "$and: [ " +
           "  { $or: [ { 'validFrom': null }, { 'validFrom': { $lte: ?1 } } ] }, " +
           "  { $or: [ { 'validTo': null }, { 'validTo': { $gte: ?1 } } ] } " +
           "] }")
    List<Assignment> findActiveAtTime(String tenantId, Instant time);

    /**
     * Find overlapping assignments for subject
     */
    @Query("{ 'tenantId': ?0, 'subject.type': ?1, 'subject.key': ?2, 'active': true, " +
           "$and: [ " +
           "  { $or: [ { 'validFrom': null }, { 'validFrom': { $lt: ?4 } } ] }, " +
           "  { $or: [ { 'validTo': null }, { 'validTo': { $gt: ?3 } } ] } " +
           "] }")
    List<Assignment> findOverlappingAssignments(String tenantId, String subjectType, String subjectKey, Instant validFrom, Instant validTo);

    /**
     * Find assignments by rule ID with pagination
     */
    Page<Assignment> findByTenantIdAndRuleId(String tenantId, String ruleId, Pageable pageable);

    /**
     * Check if active assignment exists for subject
     */
    @Query(value = "{ 'tenantId': ?0, 'subject.type': ?1, 'subject.key': ?2, 'active': true }", exists = true)
    boolean existsActiveAssignmentForSubject(String tenantId, String subjectType, String subjectKey);

    /**
     * Count assignments by tenant and active status
     */
    long countByTenantIdAndActive(String tenantId, Boolean active);

    /**
     * Find latest assignment version for rule
     */
    Optional<Assignment> findFirstByTenantIdAndRuleIdOrderByAssignmentVersionDesc(String tenantId, String ruleId);

    /**
     * Check if assignment exists by subject type and key
     */
    boolean existsBySubjectTypeAndSubjectKey(String subjectType, String subjectKey);

    /**
     * Find assignment by subject type and key
     */
    @Query("{ 'subject.type': ?0, 'subject.key': ?1 }")
    Optional<Assignment> findBySubjectTypeAndSubjectKey(String subjectType, String subjectKey);

    /**
     * Find all assignments by subject type and key
     */
    @Query("{ 'subject.type': ?0, 'subject.key': ?1 }")
    List<Assignment> findAllBySubjectTypeAndSubjectKey(String subjectType, String subjectKey);
}