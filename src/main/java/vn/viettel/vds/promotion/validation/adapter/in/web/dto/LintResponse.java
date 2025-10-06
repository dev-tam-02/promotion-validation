package vn.viettel.vds.promotion.validation.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Response from rule linting/validation")
public class LintResponse {

    @Schema(description = "Whether validation passed", example = "true")
    @JsonProperty("ok")
    private boolean ok;

    @Schema(description = "List of validation issues")
    @JsonProperty("issues")
    private List<LintIssue> issues;

    // Getters and setters
    public boolean isOk() {
        return ok;
    }

    public void setOk(boolean ok) {
        this.ok = ok;
    }

    public List<LintIssue> getIssues() {
        return issues;
    }

    public void setIssues(List<LintIssue> issues) {
        this.issues = issues;
    }

    @Schema(description = "Validation issue")
    public static class LintIssue {

        @Schema(description = "Path to the problematic field", example = "nodes[0].params.amount")
        @JsonProperty("path")
        private String path;

        @Schema(description = "Issue description", example = "Amount must be a positive number")
        @JsonProperty("message")
        private String message;

        @Schema(description = "Related operator name", example = "order.total.gte")
        @JsonProperty("operator")
        private String operator;

        public LintIssue() {
        }

        public LintIssue(String path, String message, String operator) {
            this.path = path;
            this.message = message;
            this.operator = operator;
        }

        // Getters and setters
        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getOperator() {
            return operator;
        }

        public void setOperator(String operator) {
            this.operator = operator;
        }
    }
}