package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.ValidationRuleAssignmentEntity;

import java.util.Optional;

/**
 * JPA Repository cho ValidationRuleAssignmentEntity.
 * Quản lý persistence của validation rule assignments.
 */
@Repository
public interface ValidationRuleAssignmentJpaRepository extends JpaRepository<ValidationRuleAssignmentEntity, String> {

    /**
     * Tìm assignment theo validation_rule_id và object_id (chưa bị xóa).
     *
     * @param validationRuleId ID của validation rule
     * @param objectId         ID của object
     * @return Optional chứa entity nếu tìm thấy
     */
    @Query("SELECT a FROM ValidationRuleAssignmentEntity a " +
            "WHERE a.validationRuleId = :validationRuleId " +
            "AND a.objectId = :objectId " +
            "AND a.deleted = false")
    Optional<ValidationRuleAssignmentEntity> findByValidationRuleIdAndObjectIdAndDeletedFalse(
            @Param("validationRuleId") String validationRuleId,
            @Param("objectId") String objectId
    );

    /**
     * Kiểm tra xem assignment có tồn tại không (chưa bị xóa).
     *
     * @param validationRuleId ID của validation rule
     * @param objectId         ID của object
     * @return true nếu tồn tại, false nếu không
     */
    @Query("SELECT COUNT(a) > 0 FROM ValidationRuleAssignmentEntity a " +
            "WHERE a.validationRuleId = :validationRuleId " +
            "AND a.objectId = :objectId " +
            "AND a.deleted = false")
    boolean existsByValidationRuleIdAndObjectIdAndDeletedFalse(
            @Param("validationRuleId") String validationRuleId,
            @Param("objectId") String objectId
    );
}
