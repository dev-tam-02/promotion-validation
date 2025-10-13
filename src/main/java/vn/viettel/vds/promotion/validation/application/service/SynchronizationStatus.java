package vn.viettel.vds.promotion.validation.application.service;

import lombok.Getter;

@Getter
@SuppressWarnings("unused")
public class SynchronizationStatus {
    private final long activeRuleAssignments;
    private final boolean validationEngineHealthy;
    private final String status;

    public SynchronizationStatus(long activeRuleAssignments, boolean validationEngineHealthy, String status) {
        this.activeRuleAssignments = activeRuleAssignments;
        this.validationEngineHealthy = validationEngineHealthy;
        this.status = status;
    }
}
