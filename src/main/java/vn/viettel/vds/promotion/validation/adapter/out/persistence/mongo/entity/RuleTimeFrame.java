package vn.viettel.vds.promotion.validation.adapter.out.persistence.mongo.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "rule_time_frames")
public class RuleTimeFrame {

    @Id
    private String id;

    @Indexed
    private String ruleId;

    @Indexed
    private String timeFrameId;

    private String mode; // "ALLOW" | "DENY" (blackout)

    public RuleTimeFrame() {
    }

    public RuleTimeFrame(String id, String ruleId, String timeFrameId, String mode) {
        this.id = id;
        this.ruleId = ruleId;
        this.timeFrameId = timeFrameId;
        this.mode = mode;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    public String getTimeFrameId() {
        return timeFrameId;
    }

    public void setTimeFrameId(String timeFrameId) {
        this.timeFrameId = timeFrameId;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }
}