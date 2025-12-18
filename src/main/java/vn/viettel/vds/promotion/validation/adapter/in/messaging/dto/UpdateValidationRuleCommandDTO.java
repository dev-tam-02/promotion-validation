package vn.viettel.vds.promotion.validation.adapter.in.messaging.dto;

import com.promix.platform.validation.annotations.Id;
import jakarta.validation.GroupSequence;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.viettel.vds.promotion.validation.adapter.in.messaging.validation.*;

/**
 * DTO for validating UpdateValidationRuleCommand in validation service.
 * Contains validation rules specific to validation service processing requirements.
 * <p>
 * Validation order:
 * 1. RequiredCheck: @NotNull validations ({FIELD}_REQUIRED errors)
 * 2. EmptyCheck: @NotBlank validations ({FIELD}_EMPTY errors)
 * 3. LengthCheck: @Size validations ({FIELD}_LENGTH_EXCEEDED errors)
 * 4. FormatCheck: Custom validators ({FIELD}_INVALID errors)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@GroupSequence({
        RequiredCheck.class,
        EmptyCheck.class,
        LengthCheck.class,
        FormatCheck.class,
        UpdateValidationRuleCommandDTO.class
})
@ValidAssignmentIdOrObjectReference(groups = RequiredCheck.class)
public class UpdateValidationRuleCommandDTO {

    // ============= Command Envelope Fields =============

    @NotNull(message = "COMMAND_ID_REQUIRED", groups = RequiredCheck.class)
    @NotBlank(message = "COMMAND_ID_EMPTY", groups = EmptyCheck.class)
    @Size(max = 36, message = "COMMAND_ID_LENGTH_EXCEEDED", groups = LengthCheck.class)
    @Id(errorCode = "COMMAND_ID_INVALID", groups = FormatCheck.class)
    private String id;

    @NotNull(message = "COMMAND_TYPE_REQUIRED", groups = RequiredCheck.class)
    @NotBlank(message = "COMMAND_TYPE_EMPTY", groups = EmptyCheck.class)
    @Size(max = 255, message = "COMMAND_TYPE_LENGTH_EXCEEDED", groups = LengthCheck.class)
    private String type;

    @NotNull(message = "SOURCE_REQUIRED", groups = RequiredCheck.class)
    @NotBlank(message = "SOURCE_EMPTY", groups = EmptyCheck.class)
    @Size(max = 255, message = "SOURCE_LENGTH_EXCEEDED", groups = LengthCheck.class)
    private String source;

    @NotNull(message = "SUBJECT_REQUIRED", groups = RequiredCheck.class)
    @NotBlank(message = "SUBJECT_EMPTY", groups = EmptyCheck.class)
    @Size(max = 36, message = "SUBJECT_LENGTH_EXCEEDED", groups = LengthCheck.class)
    @Id(errorCode = "SUBJECT_INVALID", groups = FormatCheck.class)
    private String subject;

    // ============= Payload Fields =============

    // assignmentId is conditionally required:
    // - Required if objectId or objectType is missing
    // - Optional if both objectId AND objectType are provided
    // Validation is handled by @ValidAssignmentIdOrObjectReference at class level
    @Size(max = 36, message = "ASSIGNMENT_ID_LENGTH_EXCEEDED", groups = LengthCheck.class)
    @Id(errorCode = "ASSIGNMENT_ID_INVALID", groups = FormatCheck.class)
    private String assignmentId;

    @Size(max = 36, message = "RULE_ID_LENGTH_EXCEEDED", groups = LengthCheck.class)
    @Id(errorCode = "RULE_ID_INVALID", groups = FormatCheck.class)
    private String ruleId;

    @Size(max = 50, message = "OBJECT_TYPE_LENGTH_EXCEEDED", groups = LengthCheck.class)
    private String objectType;

    @Size(max = 36, message = "OBJECT_ID_LENGTH_EXCEEDED", groups = LengthCheck.class)
    @Id(errorCode = "OBJECT_ID_INVALID", groups = FormatCheck.class)
    private String objectId;

    private Boolean active;

    @Min(value = 0, message = "TRAFFIC_PERCENT_INVALID", groups = FormatCheck.class)
    @Max(value = 100, message = "TRAFFIC_PERCENT_INVALID", groups = FormatCheck.class)
    private Integer trafficPercent;

    @Min(value = 0, message = "PRIORITY_INVALID", groups = FormatCheck.class)
    private Integer priority;

    // ============= Optional Fields (No validation) =============

    private String notes;

    @NotNull(message = "UPDATED_BY_REQUIRED", groups = RequiredCheck.class)
    @NotBlank(message = "UPDATED_BY_EMPTY", groups = EmptyCheck.class)
    @Size(max = 255, message = "UPDATED_BY_LENGTH_EXCEEDED", groups = LengthCheck.class)
    private String updatedBy;

    @Size(max = 500, message = "REASON_LENGTH_EXCEEDED", groups = LengthCheck.class)
    private String reason;
}
