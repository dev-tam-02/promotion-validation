package vn.viettel.vds.promotion.validation.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Domain model đại diện cho việc gán validation rule cho object (campaign, voucher, etc.).
 * Đây là pure domain object không chứa logic persistence.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ValidationRuleAssignment {

    /**
     * ID của assignment (UUID)
     */
    private String id;

    /**
     * ID của validation rule được gán
     */
    private String validationRuleId;

    /**
     * ID của object được gán (campaign, voucher, etc.)
     */
    private String objectId;

    /**
     * Loại object (CAMPAIGN, VOUCHER, etc.)
     */
    private String objectType;

    /**
     * Version cho optimistic locking
     */
    private Long version;

    // Audit fields
    private OffsetDateTime createdAt;
    private String createdBy;
    private OffsetDateTime lastModifiedAt;
    private String lastModifiedBy;

    // Soft delete fields
    private OffsetDateTime deletedAt;
    private String deletedBy;
    private Boolean deleted;

    /**
     * Kiểm tra xem assignment đã bị xóa chưa
     */
    public boolean isDeleted() {
        return Boolean.TRUE.equals(deleted);
    }

    /**
     * Thực hiện soft delete
     */
    public ValidationRuleAssignment markAsDeleted(String deletedBy) {
        return this.toBuilder()
                .deleted(true)
                .deletedAt(OffsetDateTime.now())
                .deletedBy(deletedBy)
                .lastModifiedAt(OffsetDateTime.now())
                .lastModifiedBy(deletedBy)
                .build();
    }
}
