package vn.viettel.vds.promotion.validation.adapter.in.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for delete assignment operation.
 * <p>
 * According to SRS PRM_KBNV_API_VALD008:
 * - code: Mã kết quả
 * - message: Mô tả kết quả
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeleteAssignmentResponse {

    /**
     * Mã kết quả xử lý.
     * Success: DELETED_ASSIGNMENT_VALIDATION_SUCCESS
     */
    private String code;

    /**
     * Mô tả chi tiết kết quả.
     */
    private String message;

    /**
     * ID của assignment đã bị xóa (optional, for reference).
     */
    private String assignmentId;

    /**
     * Create success response.
     */
    public static DeleteAssignmentResponse success(String assignmentId) {
        return DeleteAssignmentResponse.builder()
                .code("DELETED_ASSIGNMENT_VALIDATION_SUCCESS")
                .message("Quy tắc kiểm duyệt đã được gỡ bỏ khỏi đối tượng thành công")
                .assignmentId(assignmentId)
                .build();
    }
}
