package vn.viettel.vds.promotion.validation.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Response from engine-backed SIMULATE evaluation ({@code POST /v1/rules/{id}/simulate}).
 *
 * @param verdict        overall ALLOW or DENY
 * @param trace          per-node trace entries (one per bound Drools declaration that fired)
 * @param matchedNodes   declaration IDs / rule names that satisfied their conditions
 * @param unmatchedNodes rule names whose agenda matches were cancelled
 * @param reasonCodes    reason codes emitted by fired DENY rules
 */
@Schema(description = "Engine-backed SIMULATE evaluation result with per-node trace")
public record RuleEngineSimulateResponse(
        @Schema(description = "Overall verdict", example = "ALLOW")
        String verdict,

        @Schema(description = "Per-node trace; one entry per bound Drools declaration that fired")
        List<TraceEntry> trace,

        @Schema(description = "Declaration IDs / rule names whose conditions were satisfied")
        List<String> matchedNodes,

        @Schema(description = "Rule names whose agenda matches were cancelled before firing")
        List<String> unmatchedNodes,

        @Schema(description = "Reason codes from fired DENY rules")
        List<String> reasonCodes
) {

    /**
     * Single trace entry from the Drools engine's {@code AgendaEventListener}.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Single per-node trace entry from the Drools agenda event listener")
    public record TraceEntry(
            @Schema(description = "Node / declaration identifier", example = "$order")
            String nodeId,

            @Schema(description = "Node type: COND or GROUP", example = "COND")
            String type,

            @Schema(description = "Operator or logical function name", example = "order.total.gte")
            String operator,

            @Schema(description = "Whether this node evaluated to true", example = "true")
            boolean result,

            @Schema(description = "Reason code if the node caused DENY; null when result=true")
            String reason
    ) {
    }
}
