package vn.viettel.vds.promotion.validation.adapter.in.messaging.dto;

import jakarta.validation.GroupSequence;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.viettel.vds.promotion.validation.adapter.in.messaging.validation.FormatCheck;
import vn.viettel.vds.promotion.validation.adapter.in.messaging.validation.RequiredCheck;
import vn.viettel.vds.promotion.validation.adapter.in.messaging.validation.ValidTimeRange;

import java.time.Instant;

/**
 * DTO for validating ValidityTimeframe in timeframe configuration.
 * Contains validation rules for start date and expiration date relationship.
 *
 * <p>Validation order:</p>
 * <ol>
 *   <li>RequiredCheck: @NotNull validations ({FIELD}_REQUIRED errors)</li>
 *   <li>FormatCheck: @ValidTimeRange validates startDate < expirationDate (TIME_RANGE_INVALID error)</li>
 * </ol>
 *
 * <p>Business rules:</p>
 * <ul>
 *   <li>startDate is required when validityTimeframe is provided</li>
 *   <li>expirationDate is required when validityTimeframe is provided</li>
 *   <li>startDate must be strictly before expirationDate</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@GroupSequence({
    RequiredCheck.class,
    FormatCheck.class,
    ValidityTimeframeDTO.class
})
@ValidTimeRange(
    startField = "startDate",
    endField = "expirationDate",
    message = "TIME_RANGE_INVALID",
    groups = FormatCheck.class
)
public class ValidityTimeframeDTO {

    /**
     * Start date of the validity period.
     * Must be before expirationDate.
     */
    @NotNull(message = "START_DATE_REQUIRED", groups = RequiredCheck.class)
    private Instant startDate;

    /**
     * Expiration date of the validity period.
     * Must be after startDate.
     */
    @NotNull(message = "EXPIRATION_DATE_REQUIRED", groups = RequiredCheck.class)
    private Instant expirationDate;

    /**
     * Optional interval specification (e.g., "P1D" for 1 day).
     * ISO 8601 duration format.
     */
    private String interval;

    /**
     * Optional duration specification (e.g., "PT1H" for 1 hour).
     * ISO 8601 duration format.
     */
    private String duration;

    /**
     * Optional activity duration after publishing (e.g., "P7D" for 7 days).
     * ISO 8601 duration format.
     */
    private String activityDurationAfterPublishing;
}
