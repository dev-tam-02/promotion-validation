package vn.viettel.vds.promotion.validation.domain.model;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;

/**
 * Domain model representing a rule publishing job.
 * This is a pure domain object without persistence concerns.
 */
@Value
@Builder(toBuilder = true)
public class PublishJob {
    String id;
    String tenantId;
    String ruleId;
    Integer targetVersion;
    JobStatus status;
    String requestedBy;
    Instant requestedAt;
    Instant completedAt;
    CompileJobInfo compile;
    List<String> errors;

    // Audit fields
    Instant createdAt;
    String createdBy;
    Instant updatedAt;
    String updatedBy;
    Long version;

    public enum JobStatus {
        RUNNING, SUCCESS, FAILED
    }

    @Value
    @Builder(toBuilder = true)
    public static class CompileJobInfo {
        String compilerId;
        String operatorsFingerprint;
        List<String> logs;
        String bundleHash;
    }
}
