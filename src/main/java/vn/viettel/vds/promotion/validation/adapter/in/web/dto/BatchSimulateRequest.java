package vn.viettel.vds.promotion.validation.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

@Schema(description = "Request for batch rule simulation")
public class BatchSimulateRequest {

    @Schema(description = "Rule version to simulate (null for latest draft)", example = "3")
    @JsonProperty("version")
    private Integer version;

    @Schema(description = "Test cases to run", required = true)
    @NotEmpty(message = "Test cases cannot be empty")
    @Valid
    @JsonProperty("cases")
    private List<TestCase> cases;

    // Getters and setters
    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public List<TestCase> getCases() {
        return cases;
    }

    public void setCases(List<TestCase> cases) {
        this.cases = cases;
    }

    @Schema(description = "Test case definition")
    public static class TestCase {

        @Schema(description = "Test case name", example = "VIP customer with large order", required = true)
        @NotNull(message = "Test case name is required")
        @JsonProperty("name")
        private String name;

        @Schema(description = "Context data for this test case", required = true)
        @NotNull(message = "Context is required")
        @JsonProperty("context")
        private Map<String, Object> context;

        @Schema(description = "Expected result (optional)")
        @JsonProperty("expect")
        private ExpectedResult expect;

        // Getters and setters
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Map<String, Object> getContext() {
            return context;
        }

        public void setContext(Map<String, Object> context) {
            this.context = context;
        }

        public ExpectedResult getExpect() {
            return expect;
        }

        public void setExpect(ExpectedResult expect) {
            this.expect = expect;
        }
    }

    @Schema(description = "Expected simulation result")
    public static class ExpectedResult {

        @Schema(description = "Expected decision", example = "allow", allowableValues = {"allow", "deny", "error"})
        @JsonProperty("decision")
        private String decision;

        @Schema(description = "Expected reason codes", example = "[\"ORDER_TOTAL_MIN\"]")
        @JsonProperty("reasonCodes")
        private List<String> reasonCodes;

        // Getters and setters
        public String getDecision() {
            return decision;
        }

        public void setDecision(String decision) {
            this.decision = decision;
        }

        public List<String> getReasonCodes() {
            return reasonCodes;
        }

        public void setReasonCodes(List<String> reasonCodes) {
            this.reasonCodes = reasonCodes;
        }
    }
}