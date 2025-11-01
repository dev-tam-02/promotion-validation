package vn.viettel.vds.promotion.validation.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

/**
 * Response DTO chứa thông tin đầy đủ về validation rule assignment.
 * Sử dụng cho endpoint /assignments/{objectType}/{objectId}.
 */
@Schema(description = "Complete validation rule assignment details")
public record ValidationRuleAssignmentResponse(
        @Schema(description = "Assignment ID", example = "019361e7-1234-7000-8000-000000000001")
        String assignmentId,

        @Schema(description = "Validation Rule ID", example = "019361e7-5678-7000-8000-000000000001")
        String validationRuleId,

        @Schema(description = "Validation Rule Name", example = "Campaign Budget Validation")
        String validationRuleName,

        @Schema(description = "Object type (e.g., campaign, product)", example = "campaign")
        String objectType,

        @Schema(description = "Object ID", example = "019361e7-9abc-7000-8000-000000000001")
        String objectId,

        @Schema(description = "Whether the assignment is active", example = "true")
        Boolean active,

        @Schema(description = "Traffic percentage (0-100)", example = "100")
        Integer trafficPercent,

        @Schema(description = "Priority of the rule", example = "1")
        Integer priority,

        @Schema(description = "Assignment notes")
        String notes,

        @Schema(description = "Creation timestamp")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
        OffsetDateTime createdAt,

        @Schema(description = "Last update timestamp")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
        OffsetDateTime updatedAt,

        @Schema(description = "Created by user", example = "system")
        String createdBy,

        @Schema(description = "Last modified by user", example = "admin")
        String lastModifiedBy
) {
}
