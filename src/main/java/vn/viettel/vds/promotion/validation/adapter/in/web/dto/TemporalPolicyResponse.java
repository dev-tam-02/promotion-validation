package vn.viettel.vds.promotion.validation.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Schema(description = "Temporal policy response")
public class TemporalPolicyResponse {

    @Schema(description = "Temporal policy ID", example = "tp_default_weekend_promo")
    @JsonProperty("id")
    private String id;

    @Schema(description = "Temporal policy name", example = "Weekend Promotion Hours")
    @JsonProperty("name")
    private String name;

    @Schema(description = "Timezone", example = "Asia/Ho_Chi_Minh")
    @JsonProperty("timezone")
    private String timezone;

    @Schema(description = "Start timestamp for the policy period")
    @JsonProperty("startTs")
    private Instant startTs;

    @Schema(description = "End timestamp for the policy period")
    @JsonProperty("endTs")
    private Instant endTs;

    @Schema(description = "RRULE string for recurring patterns", example = "FREQ=WEEKLY;BYDAY=SA,SU")
    @JsonProperty("rrule")
    private String rrule;

    @Schema(description = "List of RDATE strings for specific inclusion dates")
    @JsonProperty("rdate")
    private List<String> rdate;

    @Schema(description = "EXRULE string for recurring exclusion patterns")
    @JsonProperty("exrule")
    private String exrule;

    @Schema(description = "List of EXDATE strings for specific exclusion dates")
    @JsonProperty("exdate")
    private List<String> exdate;

    @Schema(description = "Metadata for additional information")
    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    @Schema(description = "Time of day windows")
    @JsonProperty("timeOfDayWindows")
    private List<TimeOfDayWindowDto> timeOfDayWindows;

    @Schema(description = "Link mode (ALLOW or DENY)", example = "ALLOW")
    @JsonProperty("mode")
    private String mode;

    @Schema(description = "Creation timestamp")
    @JsonProperty("createdAt")
    private Instant createdAt;

    @Schema(description = "Last update timestamp")
    @JsonProperty("updatedAt")
    private Instant updatedAt;

    @Schema(description = "Version for optimistic locking")
    @JsonProperty("version")
    private Long version;

    // Getters and setters

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

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
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

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public List<TimeOfDayWindowDto> getTimeOfDayWindows() {
        return timeOfDayWindows;
    }

    public void setTimeOfDayWindows(List<TimeOfDayWindowDto> timeOfDayWindows) {
        this.timeOfDayWindows = timeOfDayWindows;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
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

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    @Schema(description = "Time of day window DTO")
    public static class TimeOfDayWindowDto {
        @Schema(description = "Start time in HH:mm format", example = "09:00")
        @JsonProperty("start")
        private String start;

        @Schema(description = "End time in HH:mm format", example = "17:00")
        @JsonProperty("end")
        private String end;

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
