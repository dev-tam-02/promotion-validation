package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.AssignmentEntity;

import java.util.List;

@Repository
public interface AssignmentJpaRepository extends JpaRepository<AssignmentEntity, String> {

    // Updated to use entityType/entityId instead of subject.type/subject.key
    @Query("SELECT a FROM AssignmentEntity a WHERE " +
            "a.entityType = :entityType AND a.entityId = :entityId")
    List<AssignmentEntity> findByEntityTypeAndEntityId(
            @Param("entityType") String entityType,
            @Param("entityId") String entityId
    );

    // Updated to use entityType/entityId with active flag
    @Query("SELECT a FROM AssignmentEntity a WHERE " +
            "a.entityType = :entityType AND a.entityId = :entityId AND a.active = :active")
    List<AssignmentEntity> findByEntityTypeAndEntityIdAndActive(
            @Param("entityType") String entityType,
            @Param("entityId") String entityId,
            @Param("active") Boolean active
    );

    // Removed tenantId, using createdAt for ordering instead of assignmentVersion
    @Query("SELECT a FROM AssignmentEntity a WHERE a.ruleId = :ruleId ORDER BY a.createdAt DESC")
    List<AssignmentEntity> findByRuleIdOrderByCreatedAtDesc(@Param("ruleId") String ruleId);

    // Simplified active check (validFrom/validTo removed from schema)
    @Query("SELECT a FROM AssignmentEntity a WHERE a.active = :active")
    List<AssignmentEntity> findByActive(@Param("active") Boolean active);

    // Removed - assignmentVersion field no longer exists
    // Optional<AssignmentEntity> findByTenantIdAndRuleIdAndVersion(...)

    // Removed - assignmentVersion field no longer exists
    // Integer findMaxVersionByRuleId(...)

    // Simplified to only check ruleId
    boolean existsByRuleId(String ruleId);

    // Check duplicate assignment by entityType and entityId
    boolean existsByEntityTypeAndEntityId(String entityType, String entityId);

    // Removed tenantId parameter
    @Query("SELECT COUNT(a) FROM AssignmentEntity a WHERE a.active = :active")
    long countByActive(@Param("active") Boolean active);
}
