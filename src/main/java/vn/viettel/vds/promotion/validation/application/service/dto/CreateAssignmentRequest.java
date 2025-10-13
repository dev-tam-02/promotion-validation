package vn.viettel.vds.promotion.validation.application.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.viettel.vds.promotion.validation.domain.model.Assignment;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SuppressWarnings("unused")
public class CreateAssignmentRequest {
    private String tenantId;
    private String ruleId;
    private String subjectType;
    private String subjectKey;
    private Boolean active;
    private Instant validFrom;
    private Instant validTo;
    private Integer trafficPercent;
    private Assignment.StickyKeyStrategy stickyKeyStrategy;
    private String createdBy;
}
