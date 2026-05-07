package vn.viettel.vds.promotion.validation.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * DTO representation of a {@link vn.viettel.vds.promotion.validation.domain.model.LintReport}.
 * Included in create / update rule responses so the admin UI can surface issues inline.
 */
@Schema(description = "Lint analysis report returned after rule save")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LintReportDto {

    @Schema(description = "Non-blocking issues the admin should review")
    @JsonProperty("warnings")
    private List<LintIssueDto> warnings;

    @Schema(description = "Blocking issues that prevented the save (should be empty on success)")
    @JsonProperty("errors")
    private List<LintIssueDto> errors;

    public LintReportDto() {
    }

    public LintReportDto(List<LintIssueDto> warnings, List<LintIssueDto> errors) {
        this.warnings = warnings;
        this.errors = errors;
    }

    public List<LintIssueDto> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<LintIssueDto> warnings) {
        this.warnings = warnings;
    }

    public List<LintIssueDto> getErrors() {
        return errors;
    }

    public void setErrors(List<LintIssueDto> errors) {
        this.errors = errors;
    }

    // -------------------------------------------------------------------------

    @Schema(description = "Single lint issue")
    public static class LintIssueDto {

        @Schema(description = "Severity level", example = "WARNING", allowableValues = {"WARNING", "ERROR"})
        @JsonProperty("severity")
        private String severity;

        @Schema(description = "Machine-readable code", example = "TAUTOLOGY",
                allowableValues = {"REDUNDANCY", "TAUTOLOGY", "CONTRADICTION", "UNREACHABLE"})
        @JsonProperty("code")
        private String code;

        @Schema(description = "Human-readable description for admin UI")
        @JsonProperty("message")
        private String message;

        @Schema(description = "ID of the offending node (null when issue spans multiple nodes)")
        @JsonProperty("nodeId")
        private String nodeId;

        public LintIssueDto() {
        }

        public LintIssueDto(String severity, String code, String message, String nodeId) {
            this.severity = severity;
            this.code = code;
            this.message = message;
            this.nodeId = nodeId;
        }

        public String getSeverity() {
            return severity;
        }

        public void setSeverity(String severity) {
            this.severity = severity;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getNodeId() {
            return nodeId;
        }

        public void setNodeId(String nodeId) {
            this.nodeId = nodeId;
        }
    }
}
