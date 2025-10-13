package vn.viettel.vds.promotion.validation.application.service;

public enum AuditEvent {
    CREATE("create"),
    UPDATE("update"),
    DEACTIVATE("deactivate");

    private final String action;

    AuditEvent(String action) {
        this.action = action;
    }

    public String getAction() {
        return action;
    }
}
