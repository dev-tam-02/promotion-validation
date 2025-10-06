package vn.viettel.vds.promotion.validation.domain.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Document(collection = "temporal_policies")
@CompoundIndexes({
        @CompoundIndex(name = "by_tz", def = "{'tenantId': 1, 'tz': 1}"),
        @CompoundIndex(name = "by_name", def = "{'tenantId': 1, 'name': 1}")
})
public class TemporalPolicy {

    @Id
    private String id;

    @Field("tenantId")
    private String tenantId;

    @Field("name")
    private String name;

    @Field("tz")
    private String tz;

    @Field("startTs")
    private Instant startTs;

    @Field("endTs")
    private Instant endTs;

    @Field("rrule")
    private String rrule;

    @Field("rdate")
    private List<String> rdate;

    @Field("exrule")
    private String exrule;

    @Field("exdate")
    private List<String> exdate;

    @Field("timeOfDayWindows")
    private List<TimeOfDayWindow> timeOfDayWindows;

    @Field("metadata")
    private Map<String, Object> metadata;

    @Field("createdAt")
    private Instant createdAt;

    @Field("updatedAt")
    private Instant updatedAt;

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
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

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public static class TimeOfDayWindow {
        @Field("start")
        private String start;

        @Field("end")
        private String end;

        // Getters and setters
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
}