package vn.viettel.vds.promotion.validation.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * A GROUP node in the rule tree.
 * Contains child nodes combined with AND / OR / NONE logic.
 * Corresponds to rule_nodes rows where type = 'GROUP'.
 */
public class GroupNode {

    private final String id;
    private final String ruleId;
    private final String parentId;
    private final GroupLogic groupLogic;
    private final int displayOrder;
    private final Instant createdAt;

    /**
     * Children assembled by RuleTreeAssembler — not persisted directly.
     */
    private final List<Object> children;

    private GroupNode(Builder builder) {
        this.id = builder.id;
        this.ruleId = builder.ruleId;
        this.parentId = builder.parentId;
        this.groupLogic = builder.groupLogic;
        this.displayOrder = builder.displayOrder;
        this.createdAt = builder.createdAt;
        this.children = new ArrayList<>(builder.children);
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

    public GroupLogic getGroupLogic() {
        return groupLogic;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Children can be GroupNode or CondNode.
     */
    public List<Object> getChildren() {
        return children;
    }

    public boolean isRoot() {
        return parentId == null;
    }

    public enum GroupLogic {
        AND, OR, NONE
    }

    public static class Builder {
        private String id;
        private String ruleId;
        private String parentId;
        private GroupLogic groupLogic;
        private int displayOrder = 0;
        private Instant createdAt;
        private List<Object> children = new ArrayList<>();

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

        public Builder groupLogic(GroupLogic groupLogic) {
            this.groupLogic = groupLogic;
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

        public Builder children(List<Object> children) {
            this.children = children != null ? new ArrayList<>(children) : new ArrayList<>();
            return this;
        }

        public Builder addChild(Object child) {
            this.children.add(child);
            return this;
        }

        public GroupNode build() {
            if (id == null || id.isBlank()) {
                throw new IllegalStateException("GroupNode id must not be null");
            }
            if (groupLogic == null) {
                throw new IllegalStateException("GroupNode groupLogic must not be null");
            }
            return new GroupNode(this);
        }
    }
}
