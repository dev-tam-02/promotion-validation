package vn.viettel.vds.promotion.validation.domain.model;

/**
 * Enumeration representing the status of a rule
 */
public enum RuleStatus {
    DRAFT("Draft", "Rule is being created or edited"),
    PENDING_REVIEW("Pending Review", "Rule is waiting for review"),
    APPROVED("Approved", "Rule has been approved but not published"),
    PUBLISHED("Published", "Rule is active and published"),
    DEPRECATED("Deprecated", "Rule is deprecated but still accessible"),
    ARCHIVED("Archived", "Rule is archived and not accessible");

    private final String displayName;
    private final String description;

    RuleStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public boolean canTransitionTo(RuleStatus newStatus) {
        switch (this) {
            case DRAFT:
                return newStatus == PENDING_REVIEW || newStatus == ARCHIVED;
            case PENDING_REVIEW:
                return newStatus == APPROVED || newStatus == DRAFT || newStatus == ARCHIVED;
            case APPROVED:
                return newStatus == PUBLISHED || newStatus == DRAFT || newStatus == ARCHIVED;
            case PUBLISHED:
                return newStatus == DEPRECATED || newStatus == ARCHIVED;
            case DEPRECATED:
                return newStatus == ARCHIVED || newStatus == PUBLISHED;
            case ARCHIVED:
                return false; // Cannot transition from ARCHIVED
            default:
                return false;
        }
    }

    public boolean isEditable() {
        return this == DRAFT || this == PENDING_REVIEW;
    }

    public boolean isActive() {
        return this == PUBLISHED;
    }
}