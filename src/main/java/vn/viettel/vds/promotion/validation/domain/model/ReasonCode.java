package vn.viettel.vds.promotion.validation.domain.model;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.Map;

/**
 * Domain model representing a validation reason code.
 * This is a pure domain object without persistence concerns.
 */
@Value
@Builder(toBuilder = true)
public class ReasonCode {
    String id;
    String tenantId;
    String category;
    Severity severity;
    Map<String, Object> labels;

    // Audit fields
    Instant createdAt;
    String createdBy;
    Instant updatedAt;
    String updatedBy;
    Long version;

    public enum Severity {
        INFO, WARN, ERROR
    }
}
