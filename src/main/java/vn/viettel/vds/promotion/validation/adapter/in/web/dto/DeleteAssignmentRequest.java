package vn.viettel.vds.promotion.validation.adapter.in.web.dto;

import com.promix.platform.validation.annotations.Id;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTO for deleting validation rule assignment from object.
 * <p>
 * According to SRS PRM_KBNV_API_VALD008:
 * - validation_rule_id: Required, max 36 chars, UUID format
 * - object_id: Required, max 36 chars, UUID format
 */
@Data
public class DeleteAssignmentRequest {

    /**
     * ID của quy tắc kiểm duyệt cần xóa khỏi đối tượng.
     * <p>
     * Validation errors:
     * - VALIDATION_RULE_ID_REQUIRED: Không truyền validation_rule_id
     * - VALIDATION_RULE_ID_EMPTY: Để trống validation_rule_id
     * - VALIDATION_RULE_ID_INVALID: Sai định dạng validation_rule_id
     * - VALIDATION_RULE_ID_LENGTH_EXCEEDED: validation_rule_id vượt quá maxlength
     */
    @NotNull(message = "VALIDATION_RULE_ID_REQUIRED")
    @NotBlank(message = "VALIDATION_RULE_ID_EMPTY")
    @Size(max = 36, message = "VALIDATION_RULE_ID_LENGTH_EXCEEDED")
    @Id(errorCode = "VALIDATION_RULE_ID_INVALID", description = "ID quy tắc kiểm duyệt không đúng định dạng")
    private String validationRuleId;

    /**
     * ID của đối tượng (campaign) cần gỡ bỏ quy tắc.
     * <p>
     * Validation errors:
     * - OBJECT_ID_REQUIRED: Không truyền object_id
     * - OBJECT_ID_EMPTY: Để trống object_id
     * - OBJECT_ID_INVALID: Sai định dạng object_id
     * - OBJECT_ID_LENGTH_EXCEEDED: object_id vượt quá maxlength
     */
    @NotNull(message = "OBJECT_ID_REQUIRED")
    @NotBlank(message = "OBJECT_ID_EMPTY")
    @Size(max = 36, message = "OBJECT_ID_LENGTH_EXCEEDED")
    @Id(errorCode = "OBJECT_ID_INVALID", description = "ID đối tượng không đúng định dạng")
    private String objectId;
}
