package vn.viettel.vds.promotion.validation.adapter.out.persistence.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Document(collection = "temporal_policies")
public class TemporalPolicy {

    @Id
    private String id;

    private String name;

    private String tz;

    private Instant startTs;

    private Instant endTs;

    private String rrule; // RRULE string

    private List<String> rdate; // List of RDATE strings

    private String exrule; // EXRULE string

    private List<String> exdate; // List of EXDATE strings

    private List<TimeOfDayWindow> timeOfDayWindows;

    private Map<String, Object> metadata;

    private Instant createdAt;

    public static class TimeOfDayWindow {
        private String start; // "HH:mm"
        private String end; // "HH:mm"

        public TimeOfDayWindow() {
        }

        public TimeOfDayWindow(String start, String end) {
            this.start = start;
            this.end = end;
        }

        public String getStart() {
            return start;
        }

        public void setStart(String start) {
            this.start = start;
        }

        public String getEnd() {
            return end;
        }

        public void setEnd(String end) {
            this.end = end;
        }
    }

    public TemporalPolicy() {
    }

    public TemporalPolicy(String id, String name, String tz) {
        this.id = id;
        this.name = name;
        this.tz = tz;
        this.createdAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTz() {
        return tz;
    }

    public void setTz(String tz) {
        this.tz = tz;
    }

    public Instant getStartTs() {
        return startTs;
    }

    public void setStartTs(Instant startTs) {
        this.startTs = startTs;
    }

    public Instant getEndTs() {
        return endTs;
    }

    public void setEndTs(Instant endTs) {
        this.endTs = endTs;
    }

    public String getRrule() {
        return rrule;
    }

    public void setRrule(String rrule) {
        this.rrule = rrule;
    }

    public List<String> getRdate() {
        return rdate;
    }

    public void setRdate(List<String> rdate) {
        this.rdate = rdate;
    }

    public String getExrule() {
        return exrule;
    }

    public void setExrule(String exrule) {
        this.exrule = exrule;
    }

    public List<String> getExdate() {
        return exdate;
    }

    public void setExdate(List<String> exdate) {
        this.exdate = exdate;
    }

    public List<TimeOfDayWindow> getTimeOfDayWindows() {
        return timeOfDayWindows;
    }

    public void setTimeOfDayWindows(List<TimeOfDayWindow> timeOfDayWindows) {
        this.timeOfDayWindows = timeOfDayWindows;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}