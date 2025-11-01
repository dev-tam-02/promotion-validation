package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.ValidationRuleAssignmentDeletedEntity;

/**
 * JPA Repository cho ValidationRuleAssignmentDeletedEntity.
 * Quản lý persistence của deleted assignments (archive).
 */
@Repository
public interface ValidationRuleAssignmentDeletedJpaRepository
        extends JpaRepository<ValidationRuleAssignmentDeletedEntity, String> {

    // Các query methods sẽ được thêm sau nếu cần
}
