package vn.viettel.vds.promotion.validation.application.port.out;

import vn.viettel.vds.promotion.validation.domain.model.ValidationRuleAssignment;
import vn.viettel.vds.promotion.validation.domain.model.ValidationRuleAssignmentDeleted;

import java.util.Optional;

/**
 * Outbound port cho ValidationRuleAssignment persistence operations.
 * Định nghĩa interface để application layer tương tác với persistence layer.
 */
public interface ValidationRuleAssignmentPort {

    /**
     * Tìm assignment theo validation rule ID và object ID.
     *
     * @param validationRuleId ID của validation rule
     * @param objectId         ID của object
     * @return Optional chứa domain model nếu tìm thấy
     */
    Optional<ValidationRuleAssignment> findByValidationRuleIdAndObjectId(
            String validationRuleId,
            String objectId
    );

    /**
     * Lưu assignment (create hoặc update).
     *
     * @param assignment Domain model cần lưu
     * @return Domain model đã được lưu
     */
    ValidationRuleAssignment save(ValidationRuleAssignment assignment);

    /**
     * Xóa assignment (hard delete - không khuyến khích sử dụng).
     *
     * @param assignment Domain model cần xóa
     */
    void delete(ValidationRuleAssignment assignment);

    /**
     * Lưu deleted assignment vào archive table.
     *
     * @param deleted Domain model của deleted assignment
     * @return Domain model đã được lưu
     */
    ValidationRuleAssignmentDeleted saveDeleted(ValidationRuleAssignmentDeleted deleted);

    /**
     * Kiểm tra assignment có tồn tại không.
     *
     * @param validationRuleId ID của validation rule
     * @param objectId         ID của object
     * @return true nếu tồn tại, false nếu không
     */
    boolean exists(String validationRuleId, String objectId);
}
