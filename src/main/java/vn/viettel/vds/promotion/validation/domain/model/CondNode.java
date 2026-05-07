package vn.viettel.vds.promotion.validation.domain.model;

import java.time.Instant;
import java.util.Map;

/**
 * A COND (condition) node in the rule tree.
 * Leaf node that holds an operator reference and its typed parameters.
 * Corresponds to rule_nodes rows where type = 'COND'.
 */
public class CondNode {

    private final String id;
    private final String ruleId;
    private final String parentId;
    private final String operatorName;
    private final Map<String, Object> params;
    private final String reasonCode;
    private final int displayOrder;
    private final Instant createdAt;

    private CondNode(Builder builder) {
        this.id = builder.id;
        this.ruleId = builder.ruleId;
        this.parentId = builder.parentId;
        this.operatorName = builder.operatorName;
        this.params = builder.params;
        this.reasonCode = builder.reasonCode;
        this.displayOrder = builder.displayOrder;
        this.createdAt = builder.createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getId() {
        return id;
    }

    public String getRuleId() {
        return ruleId;
    }

    public String getParentId() {
        return parentId;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public static class Builder {
        private String id;
        private String ruleId;
        private String parentId;
        private String operatorName;
        private Map<String, Object> params;
        private String reasonCode;
        private int displayOrder = 0;
        private Instant createdAt;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder ruleId(String ruleId) {
            this.ruleId = ruleId;
            return this;
        }

        public Builder parentId(String parentId) {
            this.parentId = parentId;
            return this;
        }

        public Builder operatorName(String operatorName) {
            this.operatorName = operatorName;
            return this;
        }

        public Builder params(Map<String, Object> params) {
            this.params = params;
            return this;
        }

        public Builder reasonCode(String reasonCode) {
            this.reasonCode = reasonCode;
            return this;
        }

        public Builder displayOrder(int displayOrder) {
            this.displayOrder = displayOrder;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public CondNode build() {
            if (id == null || id.isBlank()) {
                throw new IllegalStateException("CondNode id must not be null");
            }
            if (operatorName == null || operatorName.isBlank()) {
                throw new IllegalStateException("CondNode operatorName must not be null");
            }
            return new CondNode(this);
        }
    }
}
