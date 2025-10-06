package vn.viettel.vds.promotion.validation.domain.model;

import vn.viettel.vds.promotion.validation.domain.valueobject.*;

import java.time.Instant;
import java.util.*;

/**
 * Core domain entity representing a validation rule
 * This is the aggregate root for rule management
 */
public class RuleAggregate {
    private final RuleId id;
    private final TenantId tenantId;
    private final RuleCode code;
    private final Instant createdAt;
    private RuleName name;
    private String description;
    private LogicType logicType;
    private List<RuleNode> nodes;
    private RuleStatus status;
    private Version version;
    private Instant updatedAt;
    private String createdBy;
    private String updatedBy;
    private Instant publishedAt;
    private String publishedBy;

    // Constructor for creating new rule
    public RuleAggregate(TenantId tenantId, RuleCode code, RuleName name, LogicType logicType, String createdBy) {
        this.id = RuleId.generate();
        this.tenantId = Objects.requireNonNull(tenantId, "TenantId cannot be null");
        this.code = Objects.requireNonNull(code, "RuleCode cannot be null");
        this.name = Objects.requireNonNull(name, "RuleName cannot be null");
        this.logicType = Objects.requireNonNull(logicType, "LogicType cannot be null");
        this.nodes = new ArrayList<>();
        this.status = RuleStatus.DRAFT;
        this.version = Version.initial();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.createdBy = Objects.requireNonNull(createdBy, "CreatedBy cannot be null");
        this.updatedBy = createdBy;
    }

    // Constructor for reconstituting from persistence
    private RuleAggregate(Builder builder) {
        this.id = builder.id;
        this.tenantId = builder.tenantId;
        this.code = builder.code;
        this.name = builder.name;
        this.description = builder.description;
        this.logicType = builder.logicType;
        this.nodes = new ArrayList<>(builder.nodes);
        this.status = builder.status;
        this.version = builder.version;
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;
        this.createdBy = builder.createdBy;
        this.updatedBy = builder.updatedBy;
        this.publishedAt = builder.publishedAt;
        this.publishedBy = builder.publishedBy;
    }

    // Business Methods

    // Builder for reconstitution from persistence
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Evaluate this rule against the given context
     */
    public ValidationResult evaluate(ValidationContext context) {
        if (status != RuleStatus.PUBLISHED) {
            return ValidationResult.skipped(id, "Rule is not published");
        }

        try {
            boolean result = evaluateNodes(context);
            String message = result ? "Rule passed" : "Rule failed";
            return result ?
                    ValidationResult.passed(id.getValue(), message) :
                    ValidationResult.failed(id.getValue(), message);
        } catch (Exception e) {
            return ValidationResult.error(id.getValue(), "Error evaluating rule: " + e.getMessage());
        }
    }

    private boolean evaluateNodes(ValidationContext context) {
        if (nodes.isEmpty()) {
            return true; // No conditions means always pass
        }

        // Convert ValidationContext to the format expected by RuleNode
        Map<String, Object> contextData = new HashMap<>();

        // Extract customer data if present
        if (context.getCustomer() != null) {
            contextData.put("customerId", context.getCustomer().getCustomerId());
            contextData.put("segment", context.getCustomer().getSegment());
            contextData.put("tier", context.getCustomer().getTier());
            contextData.put("totalPurchaseAmount", context.getCustomer().getTotalPurchaseAmount());
            contextData.put("transactionCount", context.getCustomer().getTransactionCount());
        }

        // Extract order data if present
        if (context.getOrder() != null) {
            contextData.put("orderId", context.getOrder().getOrderId());
            contextData.put("orderValue", context.getOrder().getOrderValue());
            contextData.put("itemCount", context.getOrder().getItemCount());
            contextData.put("channel", context.getOrder().getChannel());
        }

        // Add metadata
        if (context.getMetadata() != null) {
            contextData.putAll(context.getMetadata());
        }

        // Create node evaluation context
        vn.viettel.vds.promotion.validation.domain.model.ValidationContext nodeContext =
                vn.viettel.vds.promotion.validation.domain.model.ValidationContext.of(contextData);

        switch (logicType) {
            case AND:
                return nodes.stream().allMatch(node -> node.evaluate(nodeContext));
            case OR:
                return nodes.stream().anyMatch(node -> node.evaluate(nodeContext));
            case NOT:
                if (nodes.size() != 1) {
                    throw new IllegalStateException("NOT logic requires exactly one node");
                }
                return !nodes.get(0).evaluate(nodeContext);
            case XOR:
                long trueCount = nodes.stream()
                        .filter(node -> node.evaluate(nodeContext))
                        .count();
                return trueCount == 1;
            default:
                throw new UnsupportedOperationException("Logic type not supported: " + logicType);
        }
    }

    /**
     * Update rule details
     */
    public void update(RuleName name, String description, LogicType logicType, List<RuleNode> nodes, String updatedBy) {
        if (!canBeModified()) {
            throw new IllegalStateException("Rule cannot be modified in status: " + status);
        }

        this.name = Objects.requireNonNull(name, "Name cannot be null");
        this.description = description;
        this.logicType = Objects.requireNonNull(logicType, "LogicType cannot be null");
        this.nodes = new ArrayList<>(nodes != null ? nodes : Collections.emptyList());
        this.updatedAt = Instant.now();
        this.updatedBy = Objects.requireNonNull(updatedBy, "UpdatedBy cannot be null");

        // Increment patch version for updates
        this.version = version.incrementPatch();
    }

    /**
     * Submit rule for review
     */
    public void submitForReview(String submittedBy) {
        if (status != RuleStatus.DRAFT) {
            throw new IllegalStateException("Only draft rules can be submitted for review");
        }

        this.status = RuleStatus.PENDING_REVIEW;
        this.updatedAt = Instant.now();
        this.updatedBy = submittedBy;
    }

    /**
     * Approve the rule
     */
    public void approve(String approvedBy) {
        if (status != RuleStatus.PENDING_REVIEW) {
            throw new IllegalStateException("Only rules pending review can be approved");
        }

        this.status = RuleStatus.APPROVED;
        this.updatedAt = Instant.now();
        this.updatedBy = approvedBy;
    }

    /**
     * Reject the rule back to draft
     */
    public void reject(String rejectedBy, String reason) {
        if (status != RuleStatus.PENDING_REVIEW) {
            throw new IllegalStateException("Only rules pending review can be rejected");
        }

        this.status = RuleStatus.DRAFT;
        this.updatedAt = Instant.now();
        this.updatedBy = rejectedBy;
        // Could store rejection reason in audit log
    }

    /**
     * Publish the rule
     */
    public void publish(String publishedBy) {
        if (status != RuleStatus.APPROVED) {
            throw new IllegalStateException("Only approved rules can be published");
        }

        this.status = RuleStatus.PUBLISHED;
        this.publishedAt = Instant.now();
        this.publishedBy = publishedBy;
        this.updatedAt = Instant.now();
        this.updatedBy = publishedBy;

        // Increment minor version on publish
        this.version = version.incrementMinor();
    }

    /**
     * Deprecate the rule
     */
    public void deprecate(String deprecatedBy) {
        if (status != RuleStatus.PUBLISHED) {
            throw new IllegalStateException("Only published rules can be deprecated");
        }

        this.status = RuleStatus.DEPRECATED;
        this.updatedAt = Instant.now();
        this.updatedBy = deprecatedBy;
    }

    /**
     * Archive the rule
     */
    public void archive(String archivedBy) {
        if (status == RuleStatus.ARCHIVED) {
            throw new IllegalStateException("Rule is already archived");
        }

        this.status = RuleStatus.ARCHIVED;
        this.updatedAt = Instant.now();
        this.updatedBy = archivedBy;
    }

    /**
     * Check if rule can be modified
     */
    public boolean canBeModified() {
        return status.isEditable();
    }

    /**
     * Check if rule is active
     */
    public boolean isActive() {
        return status.isActive();
    }

    /**
     * Clone this rule as a new draft
     */
    public RuleAggregate cloneAsDraft(String clonedBy) {
        RuleAggregate cloned = new RuleAggregate(tenantId, RuleCode.of(code.getValue() + "_COPY"), name, logicType, clonedBy);
        cloned.description = this.description + " (Copy)";
        cloned.nodes = new ArrayList<>(this.nodes);
        return cloned;
    }

    // Getters
    public RuleId getId() {
        return id;
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public RuleCode getCode() {
        return code;
    }

    public RuleName getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public LogicType getLogicType() {
        return logicType;
    }

    public List<RuleNode> getNodes() {
        return Collections.unmodifiableList(nodes);
    }

    public RuleStatus getStatus() {
        return status;
    }

    public Version getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public String getPublishedBy() {
        return publishedBy;
    }

    public static class Builder {
        private RuleId id;
        private TenantId tenantId;
        private RuleCode code;
        private RuleName name;
        private String description;
        private LogicType logicType;
        private List<RuleNode> nodes = new ArrayList<>();
        private RuleStatus status;
        private Version version;
        private Instant createdAt;
        private Instant updatedAt;
        private String createdBy;
        private String updatedBy;
        private Instant publishedAt;
        private String publishedBy;

        public Builder id(RuleId id) {
            this.id = id;
            return this;
        }

        public Builder tenantId(TenantId tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder code(RuleCode code) {
            this.code = code;
            return this;
        }

        public Builder name(RuleName name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder logicType(LogicType logicType) {
            this.logicType = logicType;
            return this;
        }

        public Builder nodes(List<RuleNode> nodes) {
            this.nodes = nodes != null ? nodes : new ArrayList<>();
            return this;
        }

        public Builder status(RuleStatus status) {
            this.status = status;
            return this;
        }

        public Builder version(Version version) {
            this.version = version;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Builder createdBy(String createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        public Builder updatedBy(String updatedBy) {
            this.updatedBy = updatedBy;
            return this;
        }

        public Builder publishedAt(Instant publishedAt) {
            this.publishedAt = publishedAt;
            return this;
        }

        public Builder publishedBy(String publishedBy) {
            this.publishedBy = publishedBy;
            return this;
        }

        public RuleAggregate build() {
            return new RuleAggregate(this);
        }
    }
}