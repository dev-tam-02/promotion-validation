package vn.viettel.vds.promotion.validation.domain.model;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Domain model representing a temporal policy for time-based validation rules.
 * This is a pure domain object without persistence concerns.
 */
@Value
@Builder(toBuilder = true)
public class TemporalPolicy {
    String id;
    String tenantId;
    String name;
    String tz;
    Instant startTs;
    Instant endTs;
    String rrule;
    List<String> rdate;
    String exrule;
    List<String> exdate;
    Map<String, Object> metadata;
    List<TimeOfDayWindow> timeOfDayWindows;

    // Audit fields
    Instant createdAt;
    String createdBy;
    Instant updatedAt;
    String updatedBy;
    Long version;
}
