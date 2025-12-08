package vn.viettel.vds.promotion.validation.adapter.in.messaging.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.viettel.vds.promotion.validation.adapter.in.messaging.validation.FormatCheck;
import vn.viettel.vds.promotion.validation.adapter.in.messaging.validation.LengthCheck;
import vn.viettel.vds.promotion.validation.adapter.in.messaging.validation.ValidDaysOfWeek;

import java.util.List;

/**
 * DTO for validating TimeFrame in SettingValidationRuleCommand.
 * Contains nested DTOs for validityTimeframe and validityHoursPerDay.
 *
 * <p>Structure:</p>
 * <pre>
 * TimeFrameDTO
 * ├── validityTimeframe: ValidityTimeframeDTO (optional)
 * │   ├── startDate (required if validityTimeframe provided)
 * │   └── expirationDate (required if validityTimeframe provided)
 * ├── validityDaysOfWeek: List<Integer> (optional)
 * ├── validityHoursPerDay: List<ValidityHoursPerDayDTO> (optional)
 * │   ├── dayOfWeek (1-7)
 * │   ├── startTime
 * │   └── expirationTime
 * ├── timeFrameId (optional)
 * ├── mode (optional, default: ALLOW)
 * └── timezone (optional, default: UTC)
 * </pre>
 *
 * <p>Validation cascades to nested DTOs via @Valid annotation.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeFrameDTO {

    /**
     * Validity timeframe with start and expiration dates.
     * When provided, both startDate and expirationDate are required.
     * startDate must be before expirationDate.
     */
    @Valid
    private ValidityTimeframeDTO validityTimeframe;

    /**
     * Days of week when the rule is valid.
     * Uses ISO-8601 standard: 1 = Monday, 7 = Sunday.
     * Example: [1, 3, 5] for Monday, Wednesday, Friday.
     * Each value must be between 1-7, no duplicates allowed.
     */
    @ValidDaysOfWeek(message = "CAMPAIGN_DAY_OF_WEEKS_INVALID", groups = FormatCheck.class)
    private List<Integer> validityDaysOfWeek;

    /**
     * Time windows per day when the rule is valid.
     * Each entry specifies dayOfWeek, startTime, and expirationTime.
     * startTime must be before expirationTime for each entry.
     */
    @Valid
    private List<ValidityHoursPerDayDTO> validityHoursPerDay;

    /**
     * Unique identifier for this timeframe configuration.
     * Optional - will be auto-generated if not provided.
     */
    @Size(max = 36, message = "TIME_FRAME_ID_LENGTH_EXCEEDED", groups = LengthCheck.class)
    private String timeFrameId;

    /**
     * Mode of the timeframe constraint.
     * ALLOW: Rule is valid only during specified times.
     * DENY: Rule is blocked during specified times.
     * Default: ALLOW
     */
    @Size(max = 10, message = "TIME_FRAME_MODE_LENGTH_EXCEEDED", groups = LengthCheck.class)
    private String mode;

    /**
     * Timezone for time calculations.
     * IANA timezone format (e.g., "Asia/Ho_Chi_Minh", "UTC").
     * Default: UTC
     */
    @Size(max = 50, message = "TIMEZONE_LENGTH_EXCEEDED", groups = LengthCheck.class)
    private String timezone;
}
