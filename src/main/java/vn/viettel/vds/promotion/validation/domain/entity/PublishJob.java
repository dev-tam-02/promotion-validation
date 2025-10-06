package vn.viettel.vds.promotion.validation.domain.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.List;

@Document(collection = "publish_jobs")
@CompoundIndexes({
        @CompoundIndex(name = "by_rule_ver", def = "{'tenantId': 1, 'ruleId': 1, 'targetVersion': -1}"),
        @CompoundIndex(name = "by_status_time", def = "{'tenantId': 1, 'status': 1, 'requestedAt': -1}")
})
public class PublishJob {

    @Id
    private String id;

    @Field("tenantId")
    private String tenantId;

    @Field("ruleId")
    private String ruleId;

    @Field("targetVersion")
    private Integer targetVersion;

    @Field("status")
    private JobStatus status;

    @Field("requestedBy")
    private String requestedBy;

    @Field("requestedAt")
    private Instant requestedAt;

    @Field("completedAt")
    private Instant completedAt;

    @Field("compile")
    private CompileJobInfo compile;

    @Field("errors")
    private List<String> errors;

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

    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    public Integer getTargetVersion() {
        return targetVersion;
    }

    public void setTargetVersion(Integer targetVersion) {
        this.targetVersion = targetVersion;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(String requestedBy) {
        this.requestedBy = requestedBy;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(Instant requestedAt) {
        this.requestedAt = requestedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public CompileJobInfo getCompile() {
        return compile;
    }

    public void setCompile(CompileJobInfo compile) {
        this.compile = compile;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public enum JobStatus {
        RUNNING, SUCCESS, FAILED
    }

    public static class CompileJobInfo {
        @Field("compilerId")
        private String compilerId;

        @Field("operatorsFingerprint")
        private String operatorsFingerprint;

        @Field("logs")
        private List<String> logs;

        @Field("bundleHash")
        private String bundleHash;

        // Getters and setters
        public String getCompilerId() {
            return compilerId;
        }

        public void setCompilerId(String compilerId) {
            this.compilerId = compilerId;
        }

        public String getOperatorsFingerprint() {
            return operatorsFingerprint;
        }

        public void setOperatorsFingerprint(String operatorsFingerprint) {
            this.operatorsFingerprint = operatorsFingerprint;
        }

        public List<String> getLogs() {
            return logs;
        }

        public void setLogs(List<String> logs) {
            this.logs = logs;
        }

        public String getBundleHash() {
            return bundleHash;
        }

        public void setBundleHash(String bundleHash) {
            this.bundleHash = bundleHash;
        }
    }
}