package vn.viettel.vds.promotion.validation.domain.model;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Domain model representing a version of a validation rule.
 * This is a pure domain object without persistence concerns.
 */
@Value
@Builder(toBuilder = true)
public class RuleVersion {
    String id;
    String ruleId;
    String code;
    Integer ruleVersion;
    LogicType logic;
    Map<String, Object> limits;
    List<Map<String, Object>> nodes;
    String operatorsFingerprint;
    Map<String, Object> dsl;
    Instant publishedAt;
    String publishedBy;
    CompileInfo compile;

    // Audit fields
    Instant createdAt;
    String createdBy;
    Instant updatedAt;
    String updatedBy;
    Long version;

    public enum LogicType {
        ALL, ANY, NONE
    }

    @Value
    @Builder(toBuilder = true)
    public static class CompileInfo {
        CompileStatus status;
        String compilerId;
        String bundleHash;
        List<String> logs;

        public enum CompileStatus {
            SUCCESS, FAILED
        }
    }
}
