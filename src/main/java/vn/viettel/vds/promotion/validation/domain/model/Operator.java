package vn.viettel.vds.promotion.validation.domain.model;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.Map;

/**
 * Domain model representing a validation operator.
 * This is a pure domain object without persistence concerns.
 */
@Value
@Builder(toBuilder = true)
public class Operator {
    String id;
    String name;
    Integer operatorVersion;
    String context;
    Map<String, Object> jsonSchema;
    String compilerId;
    OperatorStatus status;

    // Audit fields
    Instant createdAt;
    String createdBy;
    Instant updatedAt;
    String updatedBy;
    Long version;

    public enum OperatorStatus {
        ACTIVE, DEPRECATED
    }
}
