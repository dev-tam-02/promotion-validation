package vn.viettel.vds.promotion.validation.adapter.in.web.dto;

import com.promix.platform.validation.annotations.Id;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import vn.viettel.vds.promotion.validation.adapter.in.web.dto.ValidationGroups.EmptyCheck;
import vn.viettel.vds.promotion.validation.adapter.in.web.dto.ValidationGroups.FormatCheck;
import vn.viettel.vds.promotion.validation.adapter.in.web.dto.ValidationGroups.LengthCheck;
import vn.viettel.vds.promotion.validation.adapter.in.web.dto.ValidationGroups.RequiredCheck;

/**
 * Request DTO for deleting validation rule assignment from object.
 * <p>
 * According to SRS PRM_KBNV_API_VALD008:
 * - validation_rule_id: Required, max 36 chars, UUID format
 * - object_id: Required, max 36 chars, UUID format
 * <p>
 * Validation uses Fail-Fast pattern with Group Sequence:
 * 1. RequiredCheck → @NotNull → {FIELD}_REQUIRED
 * 2. EmptyCheck    → @NotBlank → {FIELD}_EMPTY
 * 3. LengthCheck   → @Size     → {FIELD}_LENGTH_EXCEEDED
 * 4. FormatCheck   → @Id       → {FIELD}_INVALID
 */
@Data
public class DeleteAssignmentRequest {

    /**
     * ID của quy tắc kiểm duyệt cần xóa khỏi đối tượng.
     * <p>
     * Validation errors (in order):
     * - VALIDATION_RULE_ID_REQUIRED: Không truyền validation_rule_id (null)
     * - VALIDATION_RULE_ID_EMPTY: Để trống validation_rule_id (blank)
     * - VALIDATION_RULE_ID_LENGTH_EXCEEDED: validation_rule_id vượt quá 36 ký tự
     * - VALIDATION_RULE_ID_INVALID: Sai định dạng UUID
     */
    @NotNull(message = "VALIDATION_RULE_ID_REQUIRED", groups = RequiredCheck.class)
    @NotBlank(message = "VALIDATION_RULE_ID_EMPTY", groups = EmptyCheck.class)
    @Size(max = 36, message = "VALIDATION_RULE_ID_LENGTH_EXCEEDED", groups = LengthCheck.class)
    @Id(errorCode = "VALIDATION_RULE_ID_INVALID", description = "ID quy tắc kiểm duyệt không đúng định dạng", groups = FormatCheck.class)
    private String validationRuleId;

    /**
     * ID của đối tượng (campaign) cần gỡ bỏ quy tắc.
     * <p>
     * Validation errors (in order):
     * - OBJECT_ID_REQUIRED: Không truyền object_id (null)
     * - OBJECT_ID_EMPTY: Để trống object_id (blank)
     * - OBJECT_ID_LENGTH_EXCEEDED: object_id vượt quá 36 ký tự
     * - OBJECT_ID_INVALID: Sai định dạng UUID
     */
    @NotNull(message = "OBJECT_ID_REQUIRED", groups = RequiredCheck.class)
    @NotBlank(message = "OBJECT_ID_EMPTY", groups = EmptyCheck.class)
    @Size(max = 36, message = "OBJECT_ID_LENGTH_EXCEEDED", groups = LengthCheck.class)
    @Id(errorCode = "OBJECT_ID_INVALID", description = "ID đối tượng không đúng định dạng", groups = FormatCheck.class)
    private String objectId;
}
