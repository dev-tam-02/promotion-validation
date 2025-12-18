package vn.viettel.vds.promotion.validation.adapter.in.messaging.dto;

import jakarta.validation.GroupSequence;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.viettel.vds.promotion.validation.adapter.in.messaging.validation.*;

/**
 * DTO for validating ValidityHoursPerDay in timeframe configuration.
 * Contains validation rules for daily time windows.
 *
 * <p>Validation order:</p>
 * <ol>
 *   <li>RequiredCheck: @NotNull validations ({FIELD}_REQUIRED errors) - only for dayOfWeek</li>
 *   <li>LengthCheck: @Size validations ({FIELD}_LENGTH_EXCEEDED errors)</li>
 *   <li>FormatCheck: @ValidTimeOfDayRange validates startTime < expirationTime (TIME_OF_DAY_RANGE_INVALID error)</li>
 * </ol>
 *
 * <p>Business rules:</p>
 * <ul>
 *   <li>dayOfWeek must be between 1 (Monday) and 7 (Sunday) - REQUIRED</li>
 *   <li>startTime and expirationTime are OPTIONAL</li>
 *   <li>If provided, startTime and expirationTime must be in valid time format (HH:mm or HH:mm:ss)</li>
 *   <li>If both provided, startTime must be strictly before expirationTime</li>
 * </ul>
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
        ValidityHoursPerDayDTO.class
})
@ValidTimeOfDayRange(
        startField = "startTime",
        endField = "expirationTime",
        message = "TIME_OF_DAY_RANGE_INVALID",
        groups = FormatCheck.class
)
public class ValidityHoursPerDayDTO {

    /**
     * Day of week (1 = Monday, 7 = Sunday).
     * Uses ISO-8601 standard where Monday is 1.
     */
    @NotNull(message = "DAY_OF_WEEK_REQUIRED", groups = RequiredCheck.class)
    @Min(value = 1, message = "DAY_OF_WEEK_INVALID", groups = FormatCheck.class)
    @Max(value = 7, message = "DAY_OF_WEEK_INVALID", groups = FormatCheck.class)
    private Integer dayOfWeek;

    /**
     * Start time of the validity window.
     * Supported formats: "HH:mm", "HH:mm:ss", "HH:mm:ss+TZ"
     * Example: "09:00", "09:00:00", "09:00:00+07:00"
     * Optional field - if not provided, defaults to start of day.
     */
    @Size(max = 20, message = "START_TIME_LENGTH_EXCEEDED", groups = LengthCheck.class)
    private String startTime;

    /**
     * Expiration time of the validity window.
     * Supported formats: "HH:mm", "HH:mm:ss", "HH:mm:ss+TZ"
     * Example: "18:00", "18:00:00", "18:00:00+07:00"
     * Optional field - if not provided, defaults to end of day.
     */
    @Size(max = 20, message = "EXPIRATION_TIME_LENGTH_EXCEEDED", groups = LengthCheck.class)
    private String expirationTime;
}
