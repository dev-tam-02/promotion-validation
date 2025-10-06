package vn.viettel.vds.promotion.validation.adapter.out.persistence.mongo.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Document(collection = "rule_versions")
@CompoundIndexes({
        @CompoundIndex(name = "rule_versions_desc", def = "{'tenantId': 1, 'ruleId': 1, 'version': -1}", unique = true),
        @CompoundIndex(name = "by_bundleHash", def = "{'tenantId': 1, 'compile.bundleHash': 1}", sparse = true),
        @CompoundIndex(name = "by_code_version", def = "{'tenantId': 1, 'code': 1, 'version': -1}")
})
public class RuleVersion {

    @Id
    private String id;

    @Field("tenantId")
    private String tenantId;

    @Field("ruleId")
    private String ruleId;

    @Field("code")
    private String code;

    @Field("version")
    private Integer version;

    @Field("logic")
    private Rule.LogicType logic;

    @Field("limits")
    private Map<String, Object> limits;

    @Field("nodes")
    private List<Rule.RuleNode> nodes;

    @Field("operatorsFingerprint")
    private String operatorsFingerprint;

    @Field("timeLinks")
    private List<TimeLink> timeLinks;

    @Field("dsl")
    private Map<String, Object> dsl;

    @Field("publishedAt")
    private Instant publishedAt;

    @Field("publishedBy")
    private String publishedBy;

    @Field("compile")
    private CompileInfo compile;

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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Rule.LogicType getLogic() {
        return logic;
    }

    public void setLogic(Rule.LogicType logic) {
        this.logic = logic;
    }

    public Map<String, Object> getLimits() {
        return limits;
    }

    public void setLimits(Map<String, Object> limits) {
        this.limits = limits;
    }

    public List<Rule.RuleNode> getNodes() {
        return nodes;
    }

    public void setNodes(List<Rule.RuleNode> nodes) {
        this.nodes = nodes;
    }

    public String getOperatorsFingerprint() {
        return operatorsFingerprint;
    }

    public void setOperatorsFingerprint(String operatorsFingerprint) {
        this.operatorsFingerprint = operatorsFingerprint;
    }

    public List<TimeLink> getTimeLinks() {
        return timeLinks;
    }

    public void setTimeLinks(List<TimeLink> timeLinks) {
        this.timeLinks = timeLinks;
    }

    public Map<String, Object> getDsl() {
        return dsl;
    }

    public void setDsl(Map<String, Object> dsl) {
        this.dsl = dsl;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    public String getPublishedBy() {
        return publishedBy;
    }

    public void setPublishedBy(String publishedBy) {
        this.publishedBy = publishedBy;
    }

    public CompileInfo getCompile() {
        return compile;
    }

    public void setCompile(CompileInfo compile) {
        this.compile = compile;
    }

    public static class TimeLink {
        @Field("policyId")
        private String policyId;

        @Field("mode")
        private TimeLinkMode mode;

        // Getters and setters
        public String getPolicyId() {
            return policyId;
        }

        public void setPolicyId(String policyId) {
            this.policyId = policyId;
        }

        public TimeLinkMode getMode() {
            return mode;
        }

        public void setMode(TimeLinkMode mode) {
            this.mode = mode;
        }

        public enum TimeLinkMode {
            ALLOW, DENY
        }
    }

    public static class CompileInfo {
        @Field("status")
        private CompileStatus status;

        @Field("compilerId")
        private String compilerId;

        @Field("bundleHash")
        private String bundleHash;

        @Field("logs")
        private List<String> logs;

        // Getters and setters
        public CompileStatus getStatus() {
            return status;
        }

        public void setStatus(CompileStatus status) {
            this.status = status;
        }

        public String getCompilerId() {
            return compilerId;
        }

        public void setCompilerId(String compilerId) {
            this.compilerId = compilerId;
        }

        public String getBundleHash() {
            return bundleHash;
        }

        public void setBundleHash(String bundleHash) {
            this.bundleHash = bundleHash;
        }

        public List<String> getLogs() {
            return logs;
        }

        public void setLogs(List<String> logs) {
            this.logs = logs;
        }

        public enum CompileStatus {
            SUCCESS, FAILED
        }
    }
}