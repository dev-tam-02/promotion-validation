package vn.viettel.vds.promotion.validation.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Batch simulation result")
public class BatchSimulationResponse {

    @Schema(description = "Simulation statistics")
    @JsonProperty("stats")
    private Stats stats;

    @Schema(description = "Individual test case results")
    @JsonProperty("results")
    private List<CaseResult> results;

    @Schema(description = "Batch simulation statistics")
    public static class Stats {

        @Schema(description = "Number of passed test cases", example = "8")
        @JsonProperty("pass")
        private int pass;

        @Schema(description = "Number of failed test cases", example = "2")
        @JsonProperty("fail")
        private int fail;

        public Stats() {}

        public Stats(int pass, int fail) {
            this.pass = pass;
            this.fail = fail;
        }

        // Getters and setters
        public int getPass() { return pass; }
        public void setPass(int pass) { this.pass = pass; }

        public int getFail() { return fail; }
        public void setFail(int fail) { this.fail = fail; }
    }

    @Schema(description = "Individual test case result")
    public static class CaseResult {

        @Schema(description = "Test case name", example = "VIP customer with large order")
        @JsonProperty("name")
        private String name;

        @Schema(description = "Actual decision", example = "allow", allowableValues = {"allow", "deny", "error"})
        @JsonProperty("decision")
        private String decision;

        @Schema(description = "Actual reason codes", example = "[\"ORDER_TOTAL_MIN\"]")
        @JsonProperty("reasonCodes")
        private List<String> reasonCodes;

        @Schema(description = "Whether test case passed", example = "true")
        @JsonProperty("ok")
        private boolean ok;

        @Schema(description = "Execution explanation")
        @JsonProperty("explain")
        private List<String> explain;

        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getDecision() { return decision; }
        public void setDecision(String decision) { this.decision = decision; }

        public List<String> getReasonCodes() { return reasonCodes; }
        public void setReasonCodes(List<String> reasonCodes) { this.reasonCodes = reasonCodes; }

        public boolean isOk() { return ok; }
        public void setOk(boolean ok) { this.ok = ok; }

        public List<String> getExplain() { return explain; }
        public void setExplain(List<String> explain) { this.explain = explain; }
    }

    // Getters and setters
    public Stats getStats() { return stats; }
    public void setStats(Stats stats) { this.stats = stats; }

    public List<CaseResult> getResults() { return results; }
    public void setResults(List<CaseResult> results) { this.results = results; }
}