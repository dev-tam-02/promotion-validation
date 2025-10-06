package vn.viettel.vds.promotion.validation.domain.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.Map;

@Document(collection = "golden_tests")
@CompoundIndex(name = "by_rule_version", def = "{'tenantId': 1, 'ruleId': 1, 'version': 1}")
public class GoldenTest {

    @Id
    private String id;

    @Field("tenantId")
    private String tenantId;

    @Field("ruleId")
    private String ruleId;

    @Field("version")
    private Integer version;

    @Field("name")
    private String name;

    @Field("input")
    private Map<String, Object> input;

    @Field("expect")
    private TestExpectation expect;

    @Field("lastRun")
    private Instant lastRun;

    @Field("lastResult")
    private TestResult lastResult;

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

    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Object> getInput() {
        return input;
    }

    public void setInput(Map<String, Object> input) {
        this.input = input;
    }

    public TestExpectation getExpect() {
        return expect;
    }

    public void setExpect(TestExpectation expect) {
        this.expect = expect;
    }

    public Instant getLastRun() {
        return lastRun;
    }

    public void setLastRun(Instant lastRun) {
        this.lastRun = lastRun;
    }

    public TestResult getLastResult() {
        return lastResult;
    }

    public void setLastResult(TestResult lastResult) {
        this.lastResult = lastResult;
    }

    public enum TestResult {
        PASS, FAIL
    }

    public static class TestExpectation {
        @Field("decision")
        private String decision;

        @Field("reasonCodes")
        private java.util.List<String> reasonCodes;

        // Getters and setters
        public String getDecision() {
            return decision;
        }

        public void setDecision(String decision) {
            this.decision = decision;
        }

        public java.util.List<String> getReasonCodes() {
            return reasonCodes;
        }

        public void setReasonCodes(java.util.List<String> reasonCodes) {
            this.reasonCodes = reasonCodes;
        }
    }
}