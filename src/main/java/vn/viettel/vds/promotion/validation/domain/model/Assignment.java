package vn.viettel.vds.promotion.validation.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Domain model representing a rule assignment to a subject.
 * This is a pure domain object without persistence concerns.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Assignment {
    private String id;
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
     * Strategy for sticky key in assignment
     */
    public enum StickyKeyStrategy {
        CUSTOMER_ID,
        ORDER_ID,
        DEVICE_ID
    }

    /**
     * Subject embeddable - represents the target of the assignment
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Subject {
        private String type;
        private String key;
    }
}
