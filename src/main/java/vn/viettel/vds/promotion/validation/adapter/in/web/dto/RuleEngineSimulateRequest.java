package vn.viettel.vds.promotion.validation.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * Request body for {@code POST /v1/rules/{id}/simulate}.
 *
 * <p>The {@code facts} map is passed directly to pp-rule-engine and should be keyed
 * by fact-type: {@code order}, {@code customer}, etc.
 */
@Schema(description = "Facts to evaluate against the rule via Drools engine SIMULATE mode")
public class RuleEngineSimulateRequest {

    @Schema(
            description = "Flat fact map keyed by fact-type (order, customer, …). "
                    + "Values may be nested maps or primitives — the engine deserialises them "
                    + "into the appropriate domain objects.",
            example = "{\"order\": {\"total\": 600000, \"currency\": \"VND\"}, "
                    + "\"customer\": {\"id\": \"c1\", \"segments\": [\"seg_vip\"]}}")
    @NotNull(message = "facts must not be null")
    private Map<String, Object> facts;

    public Map<String, Object> getFacts() {
        return facts;
    }

    public void setFacts(Map<String, Object> facts) {
        this.facts = facts;
    }
}
