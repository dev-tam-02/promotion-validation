package vn.viettel.vds.promotion.validation.adapter.in.messaging.dto;

import com.promix.platform.validation.annotations.Id;
import jakarta.validation.GroupSequence;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.viettel.vds.promotion.validation.adapter.in.messaging.validation.EmptyCheck;
import vn.viettel.vds.promotion.validation.adapter.in.messaging.validation.FormatCheck;
import vn.viettel.vds.promotion.validation.adapter.in.messaging.validation.LengthCheck;
import vn.viettel.vds.promotion.validation.adapter.in.messaging.validation.RequiredCheck;

/**
 * DTO for validating RollbackValidationRuleCommand in validation service.
 * Contains validation rules specific to rollback command processing requirements.
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
        RollbackValidationRuleCommandDTO.class
})
public class RollbackValidationRuleCommandDTO {

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

    @NotNull(message = "CAMPAIGN_ID_REQUIRED", groups = RequiredCheck.class)
    @NotBlank(message = "CAMPAIGN_ID_EMPTY", groups = EmptyCheck.class)
    @Size(max = 36, message = "CAMPAIGN_ID_LENGTH_EXCEEDED", groups = LengthCheck.class)
    @Id(errorCode = "CAMPAIGN_ID_INVALID", groups = FormatCheck.class)
    private String campaignId;

    // validationRuleId is optional when rollbackAll=true
    // Only validated when provided (not null/blank)
    @Size(max = 36, message = "VALIDATION_RULE_ID_LENGTH_EXCEEDED", groups = LengthCheck.class)
    private String validationRuleId;

    @NotNull(message = "ROLLBACK_ALL_REQUIRED", groups = RequiredCheck.class)
    private Boolean rollbackAll;

    // ============= Optional Fields (No validation) =============

    private String rollbackReason;
    private String correlationId;
}
