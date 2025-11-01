package vn.viettel.vds.promotion.validation.application.port.in;

import vn.viettel.vds.promotion.validation.adapter.in.web.dto.ValidationRuleAssignmentResponse;

/**
 * Use case để lấy thông tin validation rule assignment theo object type và object ID.
 * Sử dụng cho endpoint /assignments/{objectType}/{objectId}.
 */
public interface GetValidationRuleAssignmentUseCase {

    /**
     * Lấy validation rule assignment details cho object.
     *
     * @param objectType Loại object (campaign, product, etc.)
     * @param objectId   ID của object
     * @return Response chứa đầy đủ thông tin assignment
     * @throws com.promix.platform.core.exception.NotFoundException nếu không tìm thấy assignment
     */
    ValidationRuleAssignmentResponse getAssignment(String objectType, String objectId);
}
