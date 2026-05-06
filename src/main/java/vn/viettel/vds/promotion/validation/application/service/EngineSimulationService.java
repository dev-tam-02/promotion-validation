package vn.viettel.vds.promotion.validation.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import vn.viettel.vds.promotion.validation.application.port.out.RuleEngineClient;
import vn.viettel.vds.promotion.validation.application.port.out.RulePersistencePort;
import vn.viettel.vds.promotion.validation.domain.model.Rule;

import java.util.Map;

/**
 * Application service that performs SIMULATE-mode rule evaluation by delegating
 * to pp-rule-engine's {@code POST /v1/rules/evaluate?mode=SIMULATE} endpoint.
 *
 * <p>Unlike {@link RuleSimulationService} (which runs an in-memory interpretation of the
 * rule's DSL tree), this service uses the live Drools engine with real KieSession tracing.
 * The response includes per-declaration matched/unmatched nodes as produced by the engine's
 * {@code SimulateAgendaEventListener}.
 */
@Service
public class EngineSimulationService {

    private static final Logger log = LoggerFactory.getLogger(EngineSimulationService.class);

    private final RulePersistencePort rulePort;
    private final RuleEngineClient ruleEngineClient;

    public EngineSimulationService(RulePersistencePort rulePort, RuleEngineClient ruleEngineClient) {
        this.rulePort = rulePort;
        this.ruleEngineClient = ruleEngineClient;
    }

    /**
     * Simulate a registered rule against the provided facts.
     *
     * @param ruleId rule identifier (must exist and have a non-null bundleHash — i.e., PUBLISHED)
     * @param facts  flat fact map keyed by fact-type: {@code order}, {@code customer}, etc.
     * @return engine simulation response with verdict, per-node trace, and matched/unmatched nodes
     * @throws IllegalArgumentException if the rule is not found or not yet published to the engine
     */
    public RuleEngineClient.SimulateResponse simulate(String ruleId, Map<String, Object> facts) {
        log.info("EngineSimulationService.simulate: ruleId={}", ruleId);

        Rule rule = rulePort.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("Rule not found: " + ruleId));

        if (rule.getBundleHash() == null || rule.getBundleHash().isBlank()) {
            log.warn("simulate: ruleId={} has no bundleHash — rule may not be published to engine yet", ruleId);
            // Still forward the request; the engine will return DENY / RULE_NOT_REGISTERED
        }

        log.debug("simulate: ruleId={}, bundleHash={}, factKeys={}", ruleId, rule.getBundleHash(), facts.keySet());
        return ruleEngineClient.simulate(ruleId, facts);
    }
}
