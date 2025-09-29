package vn.viettel.vds.promotion.validation.adapter.out.persistence.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "time_exceptions")
@CompoundIndex(def = "{'timeFrameId': 1, 'fromTs': 1}")
public class TimeException {

    @Id
    private String id;

    private String timeFrameId;

    private Instant fromTs;

    private Instant toTs;

    private String mode; // "DENY" | "ALLOW"

    private String reason;

    public TimeException() {
    }

    public TimeException(String id, String timeFrameId, Instant fromTs, Instant toTs, String mode, String reason) {
        this.id = id;
        this.timeFrameId = timeFrameId;
        this.fromTs = fromTs;
        this.toTs = toTs;
        this.mode = mode;
        this.reason = reason;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTimeFrameId() {
        return timeFrameId;
    }

    public void setTimeFrameId(String timeFrameId) {
        this.timeFrameId = timeFrameId;
    }

    public Instant getFromTs() {
        return fromTs;
    }

    public void setFromTs(Instant fromTs) {
        this.fromTs = fromTs;
    }

    public Instant getToTs() {
        return toTs;
    }

    public void setToTs(Instant toTs) {
        this.toTs = toTs;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}