package vn.viettel.vds.promotion.validation.adapter.out.persistence.mongo.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Document(collection = "rules")
@CompoundIndexes({
        @CompoundIndex(name = "uniq_tenant_code", def = "{'tenantId': 1, 'code': 1}", unique = true),
        @CompoundIndex(name = "list_state_time", def = "{'tenantId': 1, 'state': 1, 'updatedAt': -1}")
})
public class Rule {

    @Id
    private String id;

    @Indexed
    @Field("tenantId")
    private String tenantId;

    @Field("code")
    private String code;

    @Field("name")
    private String name;

    @Field("state")
    private RuleState state;

    @Field("latestVersion")
    private Integer latestVersion;

    @Field("logic")
    private LogicType logic;

    @Field("limits")
    private Map<String, Object> limits;

    @Field("nodes")
    private List<RuleNode> nodes;

    @Field("notes")
    private String notes;

    @Field("createdAt")
    private Instant createdAt;

    @Field("createdBy")
    private String createdBy;

    @Field("updatedAt")
    private Instant updatedAt;

    @Field("updatedBy")
    private String updatedBy;

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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public RuleState getState() {
        return state;
    }

    public void setState(RuleState state) {
        this.state = state;
    }

    public Integer getLatestVersion() {
        return latestVersion;
    }

    public void setLatestVersion(Integer latestVersion) {
        this.latestVersion = latestVersion;
    }

    public LogicType getLogic() {
        return logic;
    }

    public void setLogic(LogicType logic) {
        this.logic = logic;
    }

    public Map<String, Object> getLimits() {
        return limits;
    }

    public void setLimits(Map<String, Object> limits) {
        this.limits = limits;
    }

    public List<RuleNode> getNodes() {
        return nodes;
    }

    public void setNodes(List<RuleNode> nodes) {
        this.nodes = nodes;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public enum RuleState {
        DRAFT, PUBLISHED, ARCHIVED
    }

    public enum LogicType {
        ALL, ANY, NONE
    }

    public static class RuleNode {
        @Field("id")
        private String id;

        @Field("type")
        private NodeType type;

        @Field("groupLogic")
        private LogicType groupLogic;

        @Field("operatorName")
        private String operatorName;

        @Field("params")
        private Map<String, Object> params;

        @Field("reasonCode")
        private String reasonCode;

        @Field("children")
        private List<RuleNode> children;

        // Getters and setters
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public NodeType getType() {
            return type;
        }

        public void setType(NodeType type) {
            this.type = type;
        }

        public LogicType getGroupLogic() {
            return groupLogic;
        }

        public void setGroupLogic(LogicType groupLogic) {
            this.groupLogic = groupLogic;
        }

        public String getOperatorName() {
            return operatorName;
        }

        public void setOperatorName(String operatorName) {
            this.operatorName = operatorName;
        }

        public Map<String, Object> getParams() {
            return params;
        }

        public void setParams(Map<String, Object> params) {
            this.params = params;
        }

        public String getReasonCode() {
            return reasonCode;
        }

        public void setReasonCode(String reasonCode) {
            this.reasonCode = reasonCode;
        }

        public List<RuleNode> getChildren() {
            return children;
        }

        public void setChildren(List<RuleNode> children) {
            this.children = children;
        }

        public enum NodeType {
            GROUP, COND
        }
    }
}