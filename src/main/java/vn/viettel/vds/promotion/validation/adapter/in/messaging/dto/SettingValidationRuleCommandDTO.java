package vn.viettel.vds.promotion.validation.adapter.in.messaging.dto;

import com.promix.platform.validation.annotations.Id;
import jakarta.validation.GroupSequence;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
 * DTO for validating SettingValidationRuleCommand in validation service.
 * Contains validation rules specific to validation service processing requirements.
 *
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
    SettingValidationRuleCommandDTO.class
})
public class SettingValidationRuleCommandDTO {

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

    /**
     * Rule ID is optional. If provided, validates format (UUID) and max length.
     */
    @Size(max = 36, message = "RULE_ID_LENGTH_EXCEEDED", groups = LengthCheck.class)
    @Id(errorCode = "RULE_ID_INVALID", groups = FormatCheck.class)
    private String ruleId;

    @NotNull(message = "OBJECT_TYPE_REQUIRED", groups = RequiredCheck.class)
    @NotBlank(message = "OBJECT_TYPE_EMPTY", groups = EmptyCheck.class)
    @Size(max = 10, message = "OBJECT_TYPE_LENGTH_EXCEEDED", groups = LengthCheck.class)
    private String objectType;

    @NotNull(message = "OBJECT_ID_REQUIRED", groups = RequiredCheck.class)
    @NotBlank(message = "OBJECT_ID_EMPTY", groups = EmptyCheck.class)
    @Size(max = 36, message = "OBJECT_ID_LENGTH_EXCEEDED", groups = LengthCheck.class)
    @Id(errorCode = "OBJECT_ID_INVALID", groups = FormatCheck.class)
    private String objectId;

    @NotNull(message = "ACTIVE_REQUIRED", groups = RequiredCheck.class)
    private Boolean active;

    @NotNull(message = "TRAFFIC_PERCENT_REQUIRED", groups = RequiredCheck.class)
    @Min(value = 0, message = "TRAFFIC_PERCENT_INVALID", groups = FormatCheck.class)
    @Max(value = 100, message = "TRAFFIC_PERCENT_INVALID", groups = FormatCheck.class)
    private Integer trafficPercent;

    @NotNull(message = "PRIORITY_REQUIRED", groups = RequiredCheck.class)
    @Min(value = 0, message = "PRIORITY_INVALID", groups = FormatCheck.class)
    private Integer priority;

    // ============= Optional Fields (No validation) =============

    private String notes;

    // ============= TimeFrame Fields (Nested validation) =============

    /**
     * Timeframe configuration for the validation rule.
     * Contains validityTimeframe (start/expiration dates) and validityHoursPerDay.
     * Validation cascades to nested DTOs.
     */
    @Valid
    private TimeFrameDTO timeframe;
}
