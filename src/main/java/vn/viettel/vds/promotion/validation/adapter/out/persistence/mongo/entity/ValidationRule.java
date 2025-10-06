package vn.viettel.vds.promotion.validation.adapter.out.persistence.mongo.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Document(collection = "validation_rules")
@CompoundIndex(def = "{'state': 1, 'version': -1}")
public class ValidationRule {

    @Id
    private String id;

    @Indexed(unique = true)
    private String code;

    private String name;

    private String state; // "draft" | "published" | "archived"

    private Integer version;

    private String logic; // root logic for implicit top-level

    private UsageLimits limits;

    private Map<String, Object> dsl; // optional raw DSL snapshot for audit

    private List<RuleNode> nodes; // embedded rule tree

    private Instant publishedAt;

    private String publishedBy;

    private Instant createdAt;

    private String createdBy;

    private Instant updatedAt;

    private String updatedBy;

    public ValidationRule() {
    }

    public ValidationRule(String id, String code, String name, String state, Integer version) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.state = state;
        this.version = version;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getLogic() {
        return logic;
    }

    public void setLogic(String logic) {
        this.logic = logic;
    }

    public UsageLimits getLimits() {
        return limits;
    }

    public void setLimits(UsageLimits limits) {
        this.limits = limits;
    }

    public Map<String, Object> getDsl() {
        return dsl;
    }

    public void setDsl(Map<String, Object> dsl) {
        this.dsl = dsl;
    }

    public List<RuleNode> getNodes() {
        return nodes;
    }

    public void setNodes(List<RuleNode> nodes) {
        this.nodes = nodes;
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

    public static class UsageLimits {
        private Integer perCodeTotal;
        private Integer perCustomer;
        private Integer perDay;

        public UsageLimits() {
        }

        public UsageLimits(Integer perCodeTotal, Integer perCustomer, Integer perDay) {
            this.perCodeTotal = perCodeTotal;
            this.perCustomer = perCustomer;
            this.perDay = perDay;
        }

        public Integer getPerCodeTotal() {
            return perCodeTotal;
        }

        public void setPerCodeTotal(Integer perCodeTotal) {
            this.perCodeTotal = perCodeTotal;
        }

        public Integer getPerCustomer() {
            return perCustomer;
        }

        public void setPerCustomer(Integer perCustomer) {
            this.perCustomer = perCustomer;
        }

        public Integer getPerDay() {
            return perDay;
        }

        public void setPerDay(Integer perDay) {
            this.perDay = perDay;
        }
    }

    public static class RuleNode {
        private String id;
        private String type; // "GROUP" | "COND"
        private String groupLogic; // "ALL" | "ANY" | "NONE" (for GROUP type)
        private List<String> children; // child node IDs (for GROUP type)
        private Integer order;
        private String operatorName; // (for COND type)
        private Map<String, Object> params; // (for COND type)
        private String reasonCode; // (for COND type)

        public RuleNode() {
        }

        public RuleNode(String id, String type) {
            this.id = id;
            this.type = type;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getGroupLogic() {
            return groupLogic;
        }

        public void setGroupLogic(String groupLogic) {
            this.groupLogic = groupLogic;
        }

        public List<String> getChildren() {
            return children;
        }

        public void setChildren(List<String> children) {
            this.children = children;
        }

        public Integer getOrder() {
            return order;
        }

        public void setOrder(Integer order) {
            this.order = order;
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
    }
}