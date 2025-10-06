package vn.viettel.vds.promotion.validation.domain.model;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.Map;

/**
 * Domain model representing an audit log entry.
 * This is a pure domain object without persistence concerns.
 */
@Value
@Builder(toBuilder = true)
public class AuditLog {
    String id;
    String tenantId;
    String actor;
    AuditAction action;
    AuditTarget target;
    Map<String, Object> diff;
    Instant at;

    // Audit fields
    Instant createdAt;
    String createdBy;
    Instant updatedAt;
    String updatedBy;
    Long version;

    public enum AuditAction {
        RULE_CREATE, RULE_EDIT, RULE_PUBLISH, OP_CREATE, ASSIGN_UPDATE
    }

    @Value
    @Builder(toBuilder = true)
    public static class AuditTarget {
        String type;
        String id;
    }
}
