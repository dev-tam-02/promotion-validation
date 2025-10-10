package vn.viettel.vds.promotion.validation.application.port.in.command;

import jakarta.validation.constraints.NotBlank;

/**
 * Command for publishing a validation rule
 */
public class PublishRuleCommand {

    @NotBlank(message = "Rule ID is required")
    private final String ruleId;

    @NotBlank(message = "Published by is required")
    private final String publishedBy;

    private final String comment;

    public PublishRuleCommand(
            String ruleId,
            String publishedBy,
            String comment
    ) {
        this.ruleId = ruleId;
        this.publishedBy = publishedBy;
        this.comment = comment;
    }

    // Builder
    public static Builder builder() {
        return new Builder();
    }

    public void validate() {
        if (ruleId == null || ruleId.trim().isEmpty()) {
            throw new IllegalArgumentException("Rule ID is required");
        }
        if (publishedBy == null || publishedBy.trim().isEmpty()) {
            throw new IllegalArgumentException("Published by is required");
        }
    }

    // Getters
    public String getRuleId() {
        return ruleId;
    }

    public String getPublishedBy() {
        return publishedBy;
    }

    public String getComment() {
        return comment;
    }

    public static class Builder {
        private String ruleId;
        private String publishedBy;
        private String comment;

        public Builder ruleId(String ruleId) {
            this.ruleId = ruleId;
            return this;
        }

        public Builder publishedBy(String publishedBy) {
            this.publishedBy = publishedBy;
            return this;
        }

        public Builder comment(String comment) {
            this.comment = comment;
            return this;
        }

        public PublishRuleCommand build() {
            return new PublishRuleCommand(ruleId, publishedBy, comment);
        }
    }
}