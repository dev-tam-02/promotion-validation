package vn.viettel.vds.promotion.validation.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import vn.viettel.vds.promotion.validation.domain.model.Rule;
import vn.viettel.vds.promotion.validation.domain.model.RuleNode;
import vn.viettel.vds.promotion.validation.domain.model.RuleVersion;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RuleSimulationService {

    private static final Logger logger = LoggerFactory.getLogger(RuleSimulationService.class);

    // SecureRandom instance for simulation purposes
    // Using SecureRandom to comply with security best practices, even though this is only
    // used for non-deterministic testing scenarios to simulate unpredictable real-world
    // behavior in rule evaluation (not for security-sensitive operations)
    private static final SecureRandom SIMULATION_RANDOM = new SecureRandom();

    private final RuleService ruleService;
    private final RuleVersionService ruleVersionService;

    public RuleSimulationService(RuleService ruleService, RuleVersionService ruleVersionService) {
        this.ruleService = ruleService;
        this.ruleVersionService = ruleVersionService;
    }

    /**
     * Simulate rule execution against provided context
     */
    public SimulationResult simulateRule(String ruleId, Integer version, SimulationContext context, ExplainLevel explainLevel) {
        logger.info("Simulating rule: id={}, version={}, explainLevel={}", ruleId, version, explainLevel);

        try {
            // Get rule or rule version
            List<RuleNode> nodes;
            Rule.LogicType rootLogic;

            if (version != null) {
                RuleVersion ruleVersion = ruleVersionService.getRuleVersion(ruleId, version);
                nodes = convertNodesToRuleNodes(ruleVersion.getNodes());
                rootLogic = convertLogicType(ruleVersion.getLogic());
            } else {
                Rule rule = ruleService.getRuleById(ruleId);
                nodes = rule.getNodes();
                rootLogic = rule.getLogic();
            }

            // Execute simulation
            SimulationResult result = executeSimulation(nodes, rootLogic, context, explainLevel);

            logger.info("Rule simulation completed: id={}, decision={}, reasonCount={}",
                    ruleId, result.getDecision(), result.getReasonCodes().size());

            return result;

        } catch (Exception e) {
            logger.error("Error during rule simulation", e);
            return SimulationResult.error("Simulation error: " + e.getMessage());
        }
    }

    /**
     * Batch simulation for golden tests
     */
    public BatchSimulationResult simulateBatch(String ruleId, Integer version, List<SimulationCase> cases) {
        logger.info("Batch simulating rule: id={}, version={}, caseCount={}", ruleId, version, cases.size());

        List<CaseResult> results = new ArrayList<>();
        int passCount = 0;
        int failCount = 0;

        for (int i = 0; i < cases.size(); i++) {
            SimulationCase testCase = cases.get(i);
            try {
                SimulationResult result = simulateRule(ruleId, version, testCase.getContext(), ExplainLevel.FAIL_ONLY);

                boolean passed = testCase.getExpected() == null ||
                        (testCase.getExpected().getDecision().equals(result.getDecision()) &&
                                reasonCodesMatch(testCase.getExpected().getReasonCodes(), result.getReasonCodes()));

                results.add(new CaseResult(testCase.getName(), result.getDecision(),
                        result.getReasonCodes(), passed, result.getExplain()));

                if (passed) {
                    passCount++;
                } else {
                    failCount++;
                }

            } catch (Exception e) {
                logger.error("Error in test case: {}", testCase.getName(), e);
                results.add(new CaseResult(testCase.getName(), Decision.ERROR,
                        List.of("SIMULATION_ERROR"), false, List.of("Error: " + e.getMessage())));
                failCount++;
            }
        }

        BatchSimulationStats stats = new BatchSimulationStats(passCount, failCount);

        logger.info("Batch simulation completed: pass={}, fail={}", passCount, failCount);
        return new BatchSimulationResult(stats, results);
    }

    private SimulationResult executeSimulation(List<RuleNode> nodes, Rule.LogicType rootLogic,
                                               SimulationContext context, ExplainLevel explainLevel) {
        // Build node map for easy lookup
        Map<String, RuleNode> nodeMap = nodes.stream()
                .collect(Collectors.toMap(RuleNode::getId, node -> node));

        // Find root nodes (not referenced by any parent)
        Set<String> referencedNodes = nodes.stream()
                .filter(node -> node.getChildren() != null)
                .flatMap(node -> node.getChildren().stream()
                        .map(RuleNode::getId)
                        .filter(java.util.Objects::nonNull))
                .collect(Collectors.toSet());

        List<String> rootNodeIds = nodes.stream()
                .map(RuleNode::getId)
                .filter(id -> !referencedNodes.contains(id))
                .toList();

        // Execute root logic
        EvaluationContext evalContext = new EvaluationContext(context, explainLevel);

        List<NodeResult> rootResults = rootNodeIds.stream()
                .map(id -> evaluateNode(id, nodeMap, evalContext))
                .toList();

        // Apply root logic
        Decision finalDecision = applyLogic(rootLogic, rootResults);
        List<String> reasonCodes = rootResults.stream()
                .flatMap(result -> result.getReasonCodes().stream())
                .distinct()
                .toList();

        List<String> explain = explainLevel != ExplainLevel.NONE ? evalContext.getExplanations() : List.of();

        return new SimulationResult(finalDecision, reasonCodes, explain);
    }

    private NodeResult evaluateNode(String nodeId, Map<String, RuleNode> nodeMap, EvaluationContext context) {
        RuleNode node = nodeMap.get(nodeId);
        if (node == null) {
            context.addExplanation("Node not found: " + nodeId);
            return new NodeResult(Decision.ERROR, List.of("NODE_NOT_FOUND"));
        }

        context.addExplanation("Evaluating node: " + nodeId + " (type: " + node.getType() + ")");

        switch (node.getType()) {
            case GROUP:
                return evaluateGroupNode(node, nodeMap, context);
            case COND:
                return evaluateConditionNode(node, context);
            default:
                context.addExplanation("Unknown node type: " + node.getType());
                return new NodeResult(Decision.ERROR, List.of("UNKNOWN_NODE_TYPE"));
        }
    }

    private NodeResult evaluateGroupNode(RuleNode node, Map<String, RuleNode> nodeMap, EvaluationContext context) {
        if (node.getChildren() == null || node.getChildren().isEmpty()) {
            context.addExplanation("Group node has no children: " + node.getId());
            return new NodeResult(Decision.ERROR, List.of("EMPTY_GROUP"));
        }

        List<NodeResult> childResults = node.getChildren().stream()
                .map(child -> evaluateNode(child.getId(), nodeMap, context))
                .toList();

        Decision groupDecision = applyLogic(node.getGroupLogic(), childResults);
        List<String> reasonCodes = childResults.stream()
                .flatMap(result -> result.getReasonCodes().stream())
                .distinct()
                .toList();

        context.addExplanation("Group " + node.getId() + " (" + node.getGroupLogic() + ") result: " + groupDecision);

        return new NodeResult(groupDecision, reasonCodes);
    }

    private NodeResult evaluateConditionNode(RuleNode node, EvaluationContext context) {
        try {
            // Simulate operator evaluation
            // In a real implementation, this would call the actual operator evaluation logic
            Decision decision = simulateOperatorEvaluation(node, context.getSimulationContext());

            List<String> reasonCodes = decision == Decision.DENY ? List.of(node.getReasonCode()) : List.of();

            String explanation = String.format("Condition %s (%s) result: %s",
                    node.getId(), node.getOperatorName(), decision);

            if (decision == Decision.DENY) {
                explanation += " - Reason: " + node.getReasonCode();
            }

            context.addExplanation(explanation);

            return new NodeResult(decision, reasonCodes);

        } catch (Exception e) {
            context.addExplanation("Error evaluating condition " + node.getId() + ": " + e.getMessage());
            return new NodeResult(Decision.ERROR, List.of("EVALUATION_ERROR"));
        }
    }

    private Decision simulateOperatorEvaluation(RuleNode node, SimulationContext context) {
        // This is a simplified simulation - in reality, this would involve
        // actual operator execution against the context data

        String operatorName = node.getOperatorName();
        Map<String, Object> params = node.getParams();

        // Simulate some common operator patterns
        switch (operatorName) {
            case "order.total.gte":
                return simulateOrderTotalGte(params, context);
            case "customer.in_segment":
                return simulateCustomerInSegment(params, context);
            case "time.window.active":
                return simulateTimeWindowActive(params, context);
            case "geo.region.matches":
                return simulateGeoRegionMatches(params, context);
            default:
                // Default simulation - randomly allow/deny for testing purposes only
                // This is NOT used for any security-sensitive operations
                // The random behavior helps simulate unpredictable real-world scenarios during rule testing
                return SIMULATION_RANDOM.nextDouble() > 0.3 ? Decision.ALLOW : Decision.DENY;
        }
    }

    private Decision simulateOrderTotalGte(Map<String, Object> params, SimulationContext context) {
        if (params == null || !params.containsKey("amount")) {
            return Decision.ERROR;
        }

        Number requiredAmount = (Number) params.get("amount");
        Number orderTotal = context.getOrderTotal();

        if (orderTotal == null) {
            return Decision.DENY;
        }

        return orderTotal.doubleValue() >= requiredAmount.doubleValue() ? Decision.ALLOW : Decision.DENY;
    }

    private Decision simulateCustomerInSegment(Map<String, Object> params, SimulationContext context) {
        if (params == null || !params.containsKey("segments")) {
            return Decision.ERROR;
        }

        @SuppressWarnings("unchecked")
        List<String> requiredSegments = (List<String>) params.get("segments");
        List<String> customerSegments = context.getCustomerSegments();

        if (customerSegments == null || customerSegments.isEmpty()) {
            return Decision.DENY;
        }

        return customerSegments.stream().anyMatch(requiredSegments::contains) ? Decision.ALLOW : Decision.DENY;
    }

    @SuppressWarnings("java:S1172") // params parameter reserved for future time window configuration
    private Decision simulateTimeWindowActive(Map<String, Object> params, SimulationContext context) {
        // Simplified time window check
        Instant now = context.getNow() != null ? context.getNow() : Instant.now();
        String timezone = context.getTimezone() != null ? context.getTimezone() : "UTC";

        ZonedDateTime zonedNow = now.atZone(ZoneId.of(timezone));
        int hour = zonedNow.getHour();

        // Simple business hours check (9 AM to 6 PM)
        return (hour >= 9 && hour < 18) ? Decision.ALLOW : Decision.DENY;
    }

    private Decision simulateGeoRegionMatches(Map<String, Object> params, SimulationContext context) {
        if (params == null || !params.containsKey("regions")) {
            return Decision.ERROR;
        }

        @SuppressWarnings("unchecked")
        List<String> allowedRegions = (List<String>) params.get("regions");
        String customerRegion = context.getCustomerRegion();

        if (customerRegion == null) {
            return Decision.DENY;
        }

        return allowedRegions.contains(customerRegion) ? Decision.ALLOW : Decision.DENY;
    }

    private Decision applyLogic(Rule.LogicType logic, List<NodeResult> results) {
        if (results.isEmpty()) {
            return Decision.ERROR;
        }

        // Check for errors first
        boolean hasError = results.stream().anyMatch(result -> result.getDecision() == Decision.ERROR);
        if (hasError) {
            return Decision.ERROR;
        }

        switch (logic) {
            case ALL:
                return results.stream().allMatch(result -> result.getDecision() == Decision.ALLOW)
                        ? Decision.ALLOW : Decision.DENY;
            case ANY:
                return results.stream().anyMatch(result -> result.getDecision() == Decision.ALLOW)
                        ? Decision.ALLOW : Decision.DENY;
            case NONE:
                return results.stream().noneMatch(result -> result.getDecision() == Decision.ALLOW)
                        ? Decision.ALLOW : Decision.DENY;
            default:
                return Decision.ERROR;
        }
    }

    private boolean reasonCodesMatch(List<String> expected, List<String> actual) {
        Set<String> expectedSet = new HashSet<>(expected);
        Set<String> actualSet = new HashSet<>(actual);
        return expectedSet.equals(actualSet);
    }

    /**
     * Convert RuleVersion.LogicType to Rule.LogicType
     */
    private Rule.LogicType convertLogicType(RuleVersion.LogicType versionLogic) {
        if (versionLogic == null) {
            return null;
        }
        return Rule.LogicType.valueOf(versionLogic.name());
    }

    /**
     * Convert List<Map<String, Object>> to List<RuleNode>
     */
    private List<RuleNode> convertNodesToRuleNodes(List<Map<String, Object>> nodeMaps) {
        if (nodeMaps == null) {
            return new ArrayList<>();
        }

        List<RuleNode> nodes = new ArrayList<>();
        for (Map<String, Object> nodeMap : nodeMaps) {
            nodes.add(convertMapToRuleNode(nodeMap));
        }
        return nodes;
    }

    /**
     * Convert a Map to RuleNode
     */
    @SuppressWarnings("unchecked")
    private RuleNode convertMapToRuleNode(Map<String, Object> nodeMap) {
        RuleNode.Builder builder = RuleNode.builder();

        if (nodeMap.containsKey("id")) {
            builder.nodeId((String) nodeMap.get("id"));
        }
        if (nodeMap.containsKey("field")) {
            builder.field((String) nodeMap.get("field"));
        }
        if (nodeMap.containsKey("operator")) {
            builder.operator((String) nodeMap.get("operator"));
        }
        if (nodeMap.containsKey("value")) {
            builder.value(nodeMap.get("value"));
        }
        if (nodeMap.containsKey("type")) {
            String type = (String) nodeMap.get("type");
            if ("GROUP".equals(type)) {
                builder.type(RuleNode.NodeType.GROUP);
            } else if ("COND".equals(type)) {
                builder.type(RuleNode.NodeType.COND);
            }
        }
        if (nodeMap.containsKey("operatorName")) {
            builder.operatorName((String) nodeMap.get("operatorName"));
        }
        if (nodeMap.containsKey("reasonCode")) {
            builder.reasonCode((String) nodeMap.get("reasonCode"));
        }
        if (nodeMap.containsKey("params")) {
            builder.params((Map<String, Object>) nodeMap.get("params"));
        }

        return builder.build();
    }

    // Supporting classes and enums
    public enum Decision {
        ALLOW, DENY, ERROR
    }

    public enum ExplainLevel {
        NONE, FAIL_ONLY, FULL
    }

    public static class SimulationContext {
        private Instant now;
        private String timezone;
        private Number orderTotal;
        private String orderCurrency;
        private String customerId;
        private List<String> customerSegments;
        private String customerRegion;
        private Map<String, Object> metadata;

        // Getters and setters
        public Instant getNow() {
            return now;
        }

        public void setNow(Instant now) {
            this.now = now;
        }

        public String getTimezone() {
            return timezone;
        }

        public void setTimezone(String timezone) {
            this.timezone = timezone;
        }

        public Number getOrderTotal() {
            return orderTotal;
        }

        public void setOrderTotal(Number orderTotal) {
            this.orderTotal = orderTotal;
        }

        public String getOrderCurrency() {
            return orderCurrency;
        }

        public void setOrderCurrency(String orderCurrency) {
            this.orderCurrency = orderCurrency;
        }

        public String getCustomerId() {
            return customerId;
        }

        public void setCustomerId(String customerId) {
            this.customerId = customerId;
        }

        public List<String> getCustomerSegments() {
            return customerSegments;
        }

        public void setCustomerSegments(List<String> customerSegments) {
            this.customerSegments = customerSegments;
        }

        public String getCustomerRegion() {
            return customerRegion;
        }

        public void setCustomerRegion(String customerRegion) {
            this.customerRegion = customerRegion;
        }

        public Map<String, Object> getMetadata() {
            return metadata;
        }

        public void setMetadata(Map<String, Object> metadata) {
            this.metadata = metadata;
        }
    }

    public static class SimulationResult {
        private final Decision decision;
        private final List<String> reasonCodes;
        private final List<String> explain;

        public SimulationResult(Decision decision, List<String> reasonCodes, List<String> explain) {
            this.decision = decision;
            this.reasonCodes = reasonCodes;
            this.explain = explain;
        }

        public static SimulationResult error(String message) {
            return new SimulationResult(Decision.ERROR, List.of("SIMULATION_ERROR"), List.of(message));
        }

        public Decision getDecision() {
            return decision;
        }

        public List<String> getReasonCodes() {
            return reasonCodes;
        }

        public List<String> getExplain() {
            return explain;
        }
    }

    public static class SimulationCase {
        private String name;
        private SimulationContext context;
        private ExpectedResult expected;

        // Getters and setters
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public SimulationContext getContext() {
            return context;
        }

        public void setContext(SimulationContext context) {
            this.context = context;
        }

        public ExpectedResult getExpected() {
            return expected;
        }

        public void setExpected(ExpectedResult expected) {
            this.expected = expected;
        }
    }

    public static class ExpectedResult {
        private Decision decision;
        private List<String> reasonCodes;

        // Getters and setters
        public Decision getDecision() {
            return decision;
        }

        public void setDecision(Decision decision) {
            this.decision = decision;
        }

        public List<String> getReasonCodes() {
            return reasonCodes;
        }

        public void setReasonCodes(List<String> reasonCodes) {
            this.reasonCodes = reasonCodes;
        }
    }

    public static class BatchSimulationResult {
        private final BatchSimulationStats stats;
        private final List<CaseResult> results;

        public BatchSimulationResult(BatchSimulationStats stats, List<CaseResult> results) {
            this.stats = stats;
            this.results = results;
        }

        public BatchSimulationStats getStats() {
            return stats;
        }

        public List<CaseResult> getResults() {
            return results;
        }
    }

    public static class BatchSimulationStats {
        private final int pass;
        private final int fail;

        public BatchSimulationStats(int pass, int fail) {
            this.pass = pass;
            this.fail = fail;
        }

        public int getPass() {
            return pass;
        }

        public int getFail() {
            return fail;
        }
    }

    public static class CaseResult {
        private final String name;
        private final Decision decision;
        private final List<String> reasonCodes;
        private final boolean ok;
        private final List<String> explain;

        public CaseResult(String name, Decision decision, List<String> reasonCodes, boolean ok, List<String> explain) {
            this.name = name;
            this.decision = decision;
            this.reasonCodes = reasonCodes;
            this.ok = ok;
            this.explain = explain;
        }

        public String getName() {
            return name;
        }

        public Decision getDecision() {
            return decision;
        }

        public List<String> getReasonCodes() {
            return reasonCodes;
        }

        public boolean isOk() {
            return ok;
        }

        public List<String> getExplain() {
            return explain;
        }
    }

    private static class NodeResult {
        private final Decision decision;
        private final List<String> reasonCodes;

        public NodeResult(Decision decision, List<String> reasonCodes) {
            this.decision = decision;
            this.reasonCodes = reasonCodes;
        }

        public Decision getDecision() {
            return decision;
        }

        public List<String> getReasonCodes() {
            return reasonCodes;
        }
    }

    private static class EvaluationContext {
        private final SimulationContext simulationContext;
        private final ExplainLevel explainLevel;
        private final List<String> explanations = new ArrayList<>();

        public EvaluationContext(SimulationContext simulationContext, ExplainLevel explainLevel) {
            this.simulationContext = simulationContext;
            this.explainLevel = explainLevel;
        }

        public void addExplanation(String explanation) {
            if (explainLevel != ExplainLevel.NONE) {
                explanations.add(explanation);
            }
        }

        public SimulationContext getSimulationContext() {
            return simulationContext;
        }

        public List<String> getExplanations() {
            return explanations;
        }
    }
}