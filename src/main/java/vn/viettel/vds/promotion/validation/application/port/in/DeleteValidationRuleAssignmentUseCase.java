package vn.viettel.vds.promotion.validation.application.port.in;

/**
 * Inbound port (use case) cho việc xóa validation rule assignment.
 * Định nghĩa contract để REST controller (adapter) gọi vào application layer.
 */
public interface DeleteValidationRuleAssignmentUseCase {

    /**
     * Xóa (soft delete) validation rule assignment.
     *
     * @param validationRuleId ID của validation rule
     * @param objectId         ID của object (campaign, voucher, etc.)
     * @throws vn.viettel.vds.promotion.validation.domain.exception.ValidationRuleNotFoundException
     *         nếu validation rule không tồn tại
     * @throws vn.viettel.vds.promotion.validation.domain.exception.AssignmentNotFoundException
     *         nếu assignment không tồn tại
     */
    void deleteAssignment(String validationRuleId, String objectId);
}
