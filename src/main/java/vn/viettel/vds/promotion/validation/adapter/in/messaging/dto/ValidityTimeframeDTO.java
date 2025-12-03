package vn.viettel.vds.promotion.validation.adapter.in.messaging.dto;

import jakarta.validation.GroupSequence;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.viettel.vds.promotion.validation.adapter.in.messaging.validation.FormatCheck;
import vn.viettel.vds.promotion.validation.adapter.in.messaging.validation.ValidISO8601Duration;
import vn.viettel.vds.promotion.validation.adapter.in.messaging.validation.ValidTimeRange;

import java.time.Instant;

/**
 * DTO for validating ValidityTimeframe in timeframe configuration.
 * Contains validation rules for start date and expiration date relationship.
 *
 * <p>Validation order:</p>
 * <ol>
 *   <li>FormatCheck: @ValidTimeRange validates startDate < expirationDate (TIME_RANGE_INVALID error)</li>
 * </ol>
 *
 * <p>Business rules:</p>
 * <ul>
 *   <li>startDate is optional - if not provided, no date-based validation applies</li>
 *   <li>expirationDate is optional - if not provided, no date-based validation applies</li>
 *   <li>If both are provided, startDate must be strictly before expirationDate</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@GroupSequence({
    FormatCheck.class,
    ValidityTimeframeDTO.class
})
@ValidTimeRange(
    startField = "startDate",
    endField = "expirationDate",
    message = "CAMPAIGN_START_DATE_BEFORE_NOW",
    groups = FormatCheck.class
)
public class ValidityTimeframeDTO {

    /**
     * Start date of the validity period.
     * Optional - if provided along with expirationDate, must be before expirationDate.
     */
    private Instant startDate;

    /**
     * Expiration date of the validity period.
     * Optional - if provided along with startDate, must be after startDate.
     */
    private Instant expirationDate;

    /**
     * Optional interval specification (e.g., "P1D" for 1 day).
     * ISO 8601 duration format.
     */
    @ValidISO8601Duration(message = "INTERVAL_INVALID_FORMAT", groups = FormatCheck.class)
    private String interval;

    /**
     * Optional duration specification (e.g., "PT1H" for 1 hour).
     * ISO 8601 duration format.
     */
    @ValidISO8601Duration(message = "DURATION_INVALID_FORMAT", groups = FormatCheck.class)
    private String duration;

    /**
     * Optional activity duration after publishing (e.g., "P7D" for 7 days).
     * ISO 8601 duration format.
     */
    @ValidISO8601Duration(message = "ACTIVITY_DURATION_INVALID_FORMAT", groups = FormatCheck.class)
    private String activityDurationAfterPublishing;
}
