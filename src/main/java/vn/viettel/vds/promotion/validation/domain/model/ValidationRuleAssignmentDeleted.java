package vn.viettel.vds.promotion.validation.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Domain model đại diện cho bản ghi assignment đã bị xóa (archive).
 * Được lưu trong validation_rules_assignment_deleted table.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ValidationRuleAssignmentDeleted {

    /**
     * ID của bản ghi archive (UUID mới)
     */
    private String id;

    /**
     * ID của assignment gốc đã bị xóa
     */
    private String assignmentId;

    /**
     * ID của validation rule
     */
    private String validationRuleId;

    /**
     * ID của object
     */
    private String objectId;

    /**
     * Loại object
     */
    private String objectType;

    /**
     * Version cuối cùng trước khi xóa
     */
    private Long version;

    // Original audit fields
    private OffsetDateTime createdAt;
    private String createdBy;

    // Deletion audit fields
    private OffsetDateTime deletedAt;
    private String deletedBy;

    /**
     * Lý do xóa (optional)
     */
    private String reason;

    /**
     * Tạo deleted record từ assignment
     */
    public static ValidationRuleAssignmentDeleted fromAssignment(
            ValidationRuleAssignment assignment,
            String newId,
            String deletedBy,
            String reason) {
        return ValidationRuleAssignmentDeleted.builder()
                .id(newId)
                .assignmentId(assignment.getId())
                .validationRuleId(assignment.getValidationRuleId())
                .objectId(assignment.getObjectId())
                .objectType(assignment.getObjectType())
                .version(assignment.getVersion())
                .createdAt(assignment.getCreatedAt())
                .createdBy(assignment.getCreatedBy())
                .deletedAt(OffsetDateTime.now())
                .deletedBy(deletedBy)
                .reason(reason)
                .build();
    }
}
