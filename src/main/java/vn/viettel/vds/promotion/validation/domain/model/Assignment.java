package vn.viettel.vds.promotion.validation.domain.model;

import lombok.Builder;
import lombok.Data;
import lombok.Value;

import java.time.Instant;

/**
 * Domain model representing a rule assignment to a subject.
 * This is a pure domain object without persistence concerns.
 */
@Data
@Builder(toBuilder = true)
public class Assignment {
    private String id;
    private String tenantId;
    private String ruleId;
    private Integer ruleVersionPinned;
    private Subject subject;
    private Integer assignmentVersion;
    private Boolean active;
    private Instant validFrom;
    private Instant validTo;
    private Integer trafficPercent;
    private StickyKeyStrategy stickyKeyStrategy;

    // Audit fields
    private Instant createdAt;
    private String createdBy;
    private Instant updatedAt;
    private String updatedBy;
    private Long version;

    /**
     * Subject embeddable - represents the target of the assignment
     */
    @Data
    public static class Subject {
        private String type;
        private String key;
    }

    /**
     * Strategy for sticky key in assignment
     */
    public enum StickyKeyStrategy {
        CUSTOMER_ID,
        ORDER_ID,
        DEVICE_ID
    }
}
