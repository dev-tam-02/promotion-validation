package vn.viettel.vds.promotion.validation.adapter.in.web;

import com.promix.platform.web.annotation.ResponseWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.viettel.vds.promotion.validation.adapter.in.web.dto.RuleEngineSimulateRequest;
import vn.viettel.vds.promotion.validation.adapter.in.web.dto.RuleEngineSimulateResponse;
import vn.viettel.vds.promotion.validation.application.port.out.RuleEngineClient.SimulateResponse;
import vn.viettel.vds.promotion.validation.application.service.EngineSimulationService;

import java.util.List;

/**
 * Exposes {@code POST /v1/rules/{id}/simulate} — engine-backed SIMULATE evaluation.
 *
 * <p>Unlike the in-memory simulation (removed from {@link RuleController}), this endpoint
 * delegates to pp-rule-engine with {@code mode=SIMULATE}, triggering real Drools KieSession
 * execution with an {@code AgendaEventListener} that captures per-declaration trace entries.
 *
 * <p>Response fields:
 * <ul>
 *   <li>{@code verdict} — overall ALLOW / DENY.</li>
 *   <li>{@code trace} — per-node trace (one entry per bound Drools declaration that fired).</li>
 *   <li>{@code matchedNodes} — declaration IDs / rule names that satisfied their conditions.</li>
 *   <li>{@code unmatchedNodes} — rule names whose matches were cancelled.</li>
 *   <li>{@code reasonCodes} — reason codes emitted by fired DENY rules.</li>
 * </ul>
 */
@RestController
@ResponseWrapper
@RequestMapping("${spring.application.context-path}/v1/rules")
@Tag(name = "Rules", description = "Rule management API")
public class RuleSimulationController {

    private static final Logger logger = LoggerFactory.getLogger(RuleSimulationController.class);

    private final EngineSimulationService engineSimulationService;

    public RuleSimulationController(EngineSimulationService engineSimulationService) {
        this.engineSimulationService = engineSimulationService;
    }

    @Operation(
            summary = "Simulate rule against Drools engine (SIMULATE mode)",
            description = "Evaluates a published rule against the provided facts using the live Drools "
                    + "KieSession with an AgendaEventListener. Returns per-node trace showing which "
                    + "patterns matched and which did not, along with the overall verdict.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Simulation completed"),
            @ApiResponse(responseCode = "400", description = "Invalid facts payload"),
            @ApiResponse(responseCode = "404", description = "Rule not found")
    })
    @PostMapping("/{id}/simulate")
    public ResponseEntity<RuleEngineSimulateResponse> simulate(
            @Parameter(description = "Rule ID") @PathVariable String id,
            @Valid @RequestBody RuleEngineSimulateRequest request) {

        logger.info("POST /v1/rules/{}/simulate — factKeys={}", id, request.getFacts().keySet());

        SimulateResponse engineResp = engineSimulationService.simulate(id, request.getFacts());

        List<RuleEngineSimulateResponse.TraceEntry> trace = engineResp.trace().stream()
                .map(t -> new RuleEngineSimulateResponse.TraceEntry(
                        t.nodeId(), t.type(), t.operator(), t.result(), t.reason()))
                .toList();

        RuleEngineSimulateResponse response = new RuleEngineSimulateResponse(
                engineResp.verdict(),
                trace,
                engineResp.matchedNodes(),
                engineResp.unmatchedNodes(),
                engineResp.reasonCodes()
        );

        logger.info("Simulation completed: ruleId={}, verdict={}, matchedNodes={}",
                id, response.verdict(), response.matchedNodes().size());

        return ResponseEntity.ok(response);
    }
}
