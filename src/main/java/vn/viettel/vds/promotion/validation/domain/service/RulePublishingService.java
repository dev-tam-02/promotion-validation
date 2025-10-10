package vn.viettel.vds.promotion.validation.domain.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.viettel.vds.promotion.validation.adapter.out.integration.ValidationEngineClient;
import vn.viettel.vds.promotion.validation.adapter.out.integration.dto.*;
import vn.viettel.vds.promotion.validation.domain.model.Rule;
import vn.viettel.vds.promotion.validation.application.port.out.RulePersistencePort;
import vn.viettel.vds.promotion.validation.application.port.out.ValidationRuleEntityPersistencePort;
import vn.viettel.vds.promotion.validation.domain.model.Rule;
import vn.viettel.vds.promotion.validation.domain.model.RuleNode;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class RulePublishingService {

    private static final Logger logger = LoggerFactory.getLogger(RulePublishingService.class);

    private final ValidationEngineClient validationEngineClient;
    private final RulePersistencePort rulePersistencePort;
    private final ValidationRuleEntityPersistencePort validationRuleEntityPersistencePort;
    private final vn.viettel.vds.promotion.validation.config.TenantProperties tenantProperties;

    public RulePublishingService(ValidationEngineClient validationEngineClient,
                                 RulePersistencePort rulePersistencePort,
                                 ValidationRuleEntityPersistencePort validationRuleEntityPersistencePort,
                                 vn.viettel.vds.promotion.validation.config.TenantProperties tenantProperties) {
        this.validationEngineClient = validationEngineClient;
        this.rulePersistencePort = rulePersistencePort;
        this.validationRuleEntityPersistencePort = validationRuleEntityPersistencePort;
        this.tenantProperties = tenantProperties;
    }

    public RulePublishResult publishRule(String ruleId) {
        logger.info("Publishing rule: ruleId={}", ruleId);

        try {
            // Find the rule to publish
            Rule rule = rulePersistencePort.findById(ruleId)
                    .orElseThrow(() -> new IllegalArgumentException("Rule not found: " + ruleId));

            // Validate rule before publishing
            validateRuleForPublishing(rule);

            // Compile rule in validation-engine
            CompileResponse compileResponse = compileRuleInEngine(rule);

            if (!compileResponse.isOk()) {
                logger.error("Rule compilation failed: ruleId={}, errors={}", ruleId, compileResponse.getErrors());
                return RulePublishResult.failed(ruleId, "Compilation failed: " + String.join(", ", compileResponse.getErrors()));
            }

            // Warm up the compiled bundle
            warmup(compileResponse);

            // Verify the bundle is working
            boolean verificationResult = verifyRuleExecution(rule, compileResponse.getBundleHash());

            if (!verificationResult) {
                logger.error("Rule verification failed: ruleId={}", ruleId);
                return RulePublishResult.failed(ruleId, "Rule verification failed");
            }

            // Update rule status to published
            rule.setState(Rule.RuleState.PUBLISHED);
            // Note: bundleHash and artifactSize not available in current entity
            // rule.setBundleHash(compileResponse.getBundleHash());
            // rule.setArtifactSize(compileResponse.getArtifactSize());
            rule.setUpdatedAt(Instant.now());

            rulePersistencePort.save(rule);

            // Create Rule from published Rule for SettingValidationRuleCommandHandler
            Rule validationRule = createValidationRuleFromRule(rule, compileResponse.getBundleHash());
            validationRuleEntityPersistencePort.save(validationRule);

            logger.info("Rule published successfully: ruleId={}, bundleHash={}, validationRuleId={}",
                    ruleId, compileResponse.getBundleHash(), validationRule.getId());

            return RulePublishResult.success(ruleId, compileResponse.getBundleHash(), compileResponse.getArtifactSize());

        } catch (Exception e) {
            logger.error("Failed to publish rule: ruleId={}", ruleId, e);
            return RulePublishResult.failed(ruleId, "Publishing failed: " + e.getMessage());
        }
    }

    public List<RulePublishResult> publishRuleBatch(List<String> ruleIds) {
        logger.info("Publishing rule batch: count={}", ruleIds.size());

        List<RulePublishResult> results = new ArrayList<>();

        for (String ruleId : ruleIds) {
            try {
                RulePublishResult result = publishRule(ruleId);
                results.add(result);

                // If critical rule fails, consider stopping batch
                if (!result.isSuccess() && isCriticalRule(ruleId)) {
                    logger.warn("Critical rule failed in batch, stopping: ruleId={}", ruleId);
                    break;
                }

            } catch (Exception e) {
                logger.error("Failed to publish rule in batch: ruleId={}", ruleId, e);
                results.add(RulePublishResult.failed(ruleId, "Batch publish failed: " + e.getMessage()));
            }
        }

        logger.info("Batch publishing completed: total={}, successful={}, failed={}",
                results.size(),
                results.stream().mapToLong(r -> r.isSuccess() ? 1 : 0).sum(),
                results.stream().mapToLong(r -> r.isSuccess() ? 0 : 1).sum());

        return results;
    }

    public RulePublishResult unpublishRule(String ruleId) {
        logger.info("Unpublishing rule: ruleId={}", ruleId);

        try {
            Rule rule = rulePersistencePort.findById(ruleId)
                    .orElseThrow(() -> new IllegalArgumentException("Rule not found: " + ruleId));

            if (Rule.RuleState.PUBLISHED != rule.getState()) {
                return RulePublishResult.failed(ruleId, "Rule is not published, cannot unpublish");
            }

            // Update rule status
            rule.setState(Rule.RuleState.DRAFT);
            // Note: bundleHash not available in current entity
            // rule.setBundleHash(null);
            rule.setUpdatedAt(Instant.now());

            rulePersistencePort.save(rule);

            logger.info("Rule unpublished successfully: ruleId={}", ruleId);
            return RulePublishResult.success(ruleId, null, 0L);

        } catch (Exception e) {
            logger.error("Failed to unpublish rule: ruleId={}", ruleId, e);
            return RulePublishResult.failed(ruleId, "Unpublishing failed: " + e.getMessage());
        }
    }

    public RuleDeploymentStatus getDeploymentStatus(String ruleId) {
        try {
            Rule rule = rulePersistencePort.findById(ruleId)
                    .orElseThrow(() -> new IllegalArgumentException("Rule not found: " + ruleId));

            if (Rule.RuleState.PUBLISHED != rule.getState()) {
                return new RuleDeploymentStatus(ruleId, "NOT_DEPLOYED", false, null);
            }

            // Note: bundleHash not available in current entity, would need to enhance model
            // For now, assume deployed if published
            return new RuleDeploymentStatus(
                    ruleId,
                    "DEPLOYED",
                    true,
                    null // bundleHash not available
            );

        } catch (Exception e) {
            logger.error("Failed to get deployment status: ruleId={}", ruleId, e);
            return new RuleDeploymentStatus(ruleId, "ERROR", false, null);
        }
    }

    private void validateRuleForPublishing(Rule rule) {
        // Check rule state - cannot publish DRAFT rules
        if (rule.getState() == Rule.RuleState.DRAFT) {
            throw new IllegalArgumentException("Cannot publish rule in DRAFT state. Rule must be activated first.");
        }

        if (rule.getNodes() == null || rule.getNodes().isEmpty()) {
            throw new IllegalArgumentException("Rule has no nodes defined");
        }

        // Validate node structure
        validateNodeStructure(rule.getNodes());
    }

    private void validateNodeStructure(List<RuleNode> nodes) {
        Map<String, RuleNode> nodeMap = new HashMap<>();
        for (RuleNode node : nodes) {
            if (node.getId() == null || node.getId().isBlank()) {
                throw new IllegalArgumentException("Node must have an ID");
            }
            if (node.getType() == null) {
                throw new IllegalArgumentException("Node must have a type");
            }
            nodeMap.put(node.getId(), node);
        }

        // Validate references
        for (RuleNode node : nodes) {
            if (RuleNode.NodeType.GROUP.equals(node.getType()) && node.getChildren() != null) {
                for (RuleNode childNode : node.getChildren()) {
                    if (!nodeMap.containsKey(childNode.getId())) {
                        throw new IllegalArgumentException("Node references non-existent child: " + childNode.getId());
                    }
                }
            }
        }
    }

    private CompileResponse compileRuleInEngine(Rule rule) {
        List<Map<String, Object>> nodesMaps = convertNodesToMaps(rule.getNodes());

        // Determine version - use latestVersion if available, otherwise use ruleVersion or default to 1
        Integer version = rule.getLatestVersion();
        if (version == null) {
            version = rule.getRuleVersion() != null ? rule.getRuleVersion().intValue() : 1;
        }

        CompileRequest compileRequest = new CompileRequest();
        compileRequest.setTenantId(tenantProperties.getDefaultTenantId());
        compileRequest.setRuleId(rule.getId());
        compileRequest.setVersion(version);
        compileRequest.setNodes(nodesMaps);
        compileRequest.setOperatorsFingerprint(generateOperatorFingerprint(rule.getNodes()));
        compileRequest.setCompilerId(tenantProperties.getCompilerId());

        // Set limits if available
        if (rule.getLimits() != null) {
            CompileRequest.Limits limits = new CompileRequest.Limits();
            limits.setPerCustomer(rule.getLimits().getPerCustomer());
            limits.setPerDay(rule.getLimits().getPerDay());
            compileRequest.setLimits(limits);
        }

        // Set source information
        CompileRequest.Source source = new CompileRequest.Source();
        source.setRuleVersionId(rule.getId() + "-v" + version);
        source.setSnapshotHash(generateSnapshotHash(rule));
        compileRequest.setSource(source);

        // Time links can be added later if needed
        compileRequest.setTimeLinks(null);

        return validationEngineClient.compile(compileRequest);
    }

    private List<RuleNodeDto> convertToNodeDtos(List<RuleNode> nodes) {
        List<RuleNodeDto> dtos = new ArrayList<>();

        for (RuleNode node : nodes) {
            RuleNodeDto dto = new RuleNodeDto();
            dto.setId(node.getId());
            dto.setType(node.getType().name());
            dto.setGroupLogic(node.getGroupLogic() != null ? node.getGroupLogic().name() : null);
            dto.setOperatorName(node.getOperatorName());
            // dto.setOperatorVersion(node.getOperatorVersion()); // Not available in current model
            dto.setParams(node.getParams());
            dto.setReasonCode(node.getReasonCode());

            // Convert children nodes to IDs
            if (node.getChildren() != null) {
                List<String> childIds = new ArrayList<>();
                for (RuleNode child : node.getChildren()) {
                    childIds.add(child.getId());
                }
                dto.setChildren(childIds);
            }
            // dto.setOrder(node.getOrder()); // Not available in current model

            dtos.add(dto);
        }

        return dtos;
    }

    private List<Map<String, Object>> convertNodesToMaps(List<RuleNode> nodes) {
        List<Map<String, Object>> nodeMaps = new ArrayList<>();

        for (RuleNode node : nodes) {
            Map<String, Object> nodeMap = new HashMap<>();
            nodeMap.put("id", node.getId());
            nodeMap.put("type", node.getType() != null ? node.getType().name() : null);

            if (node.getGroupLogic() != null) {
                nodeMap.put("groupLogic", node.getGroupLogic().name());
            }

            if (node.getOperatorName() != null) {
                nodeMap.put("operatorName", node.getOperatorName());
            }

            if (node.getParams() != null) {
                nodeMap.put("params", node.getParams());
            }

            if (node.getReasonCode() != null) {
                nodeMap.put("reasonCode", node.getReasonCode());
            }

            // Convert children to IDs
            if (node.getChildren() != null && !node.getChildren().isEmpty()) {
                List<String> childIds = new ArrayList<>();
                for (RuleNode child : node.getChildren()) {
                    childIds.add(child.getId());
                }
                nodeMap.put("children", childIds);
            }

            nodeMaps.add(nodeMap);
        }

        return nodeMaps;
    }

    private String generateOperatorFingerprint(List<RuleNode> nodes) {
        // Generate a fingerprint based on operators used
        StringBuilder fingerprint = new StringBuilder();
        for (RuleNode node : nodes) {
            if (node.getOperatorName() != null) {
                fingerprint.append(node.getOperatorName())
                        .append(":")
                        .append("latest") // operatorVersion not available in current model
                        .append(";");
            }
        }
        return fingerprint.toString().isEmpty() ? "default-fingerprint" : fingerprint.toString();
    }

    private String generateSnapshotHash(Rule rule) {
        // Generate a hash based on rule content for versioning
        Integer version = rule.getLatestVersion();
        if (version == null) {
            version = rule.getRuleVersion() != null ? rule.getRuleVersion().intValue() : 1;
        }

        StringBuilder content = new StringBuilder();
        content.append(rule.getId());
        content.append("|");
        content.append(version);
        content.append("|");
        if (rule.getLogic() != null) {
            content.append(rule.getLogic().name());
        }
        content.append("|");
        if (rule.getNodes() != null) {
            content.append(rule.getNodes().size());
        }

        // Simple hash - in production, use proper hashing algorithm like SHA-256
        return Integer.toHexString(content.toString().hashCode());
    }

    private void warmup(CompileResponse compileResponse) {
        if (compileResponse.getArtifactBytes() != null) {
            WarmupRequest warmupRequest = new WarmupRequest(
                    compileResponse.getBundleHash(),
                    compileResponse.getArtifactBytes()
            );

            validationEngineClient.warmup(warmupRequest);
            logger.debug("Bundle warmed up: bundleHash={}", compileResponse.getBundleHash());
        }
    }

    private boolean verifyRuleExecution(Rule rule, String bundleHash) {
        try {
            // Create a simple test execution to verify the rule works
            ExecuteRequest testRequest = createTestExecuteRequest(rule, bundleHash);
            ExecuteResponse response = validationEngineClient.execute(testRequest);

            boolean isValid = response.isOk() &&
                    (response.getDecision().equals("ALLOW") || response.getDecision().equals("DENY"));

            logger.debug("Rule verification completed: ruleId={}, valid={}, decision={}",
                    rule.getId(), isValid, response.getDecision());

            return isValid;

        } catch (Exception e) {
            logger.error("Rule verification failed: ruleId={}", rule.getId(), e);
            return false;
        }
    }

    private ExecuteRequest createTestExecuteRequest(Rule rule, String bundleHash) {
        ExecuteRequest request = new ExecuteRequest();
        request.setBundleHash(bundleHash);

        // Create minimal test data
        CustomerDto customer = new CustomerDto();
        customer.setId("test-customer");
        customer.setTier(1); // Using tier level instead of string

        OrderDto order = new OrderDto();
        order.setId("test-order");
        order.setTotal(java.math.BigDecimal.valueOf(100000.0));
        order.setCurrency("VND");

        CandidateDto candidate = new CandidateDto();
        candidate.setId("test-candidate");
        candidate.setType("voucher");
        // candidate.setCode("TEST"); // Method not available in DTO

        ExecutionContextDto context = new ExecutionContextDto();
        // context.setTimestamp(System.currentTimeMillis()); // Method not available in DTO

        request.setCustomer(customer);
        request.setOrder(order);
        request.setCandidate(candidate);
        request.setExecutionContext(context);

        return request;
    }

    private boolean isCriticalRule(String ruleId) {
        // Implement logic to determine if a rule is critical
        // For now, assume all rules are non-critical
        return false;
    }

    private Rule createValidationRuleFromRule(Rule rule, String bundleHash) {
        // Convert nodes
        List<RuleNode> validationNodes = null;
        if (rule.getNodes() != null) {
            validationNodes = rule.getNodes().stream()
                    .map(this::convertRuleNodeToValidationNode)
                    .collect(Collectors.toList());
        }

        // Set timestamps
        Instant now = Instant.now();

        // Build new Rule with published state
        Rule validationRule = Rule.builder()
                .id(rule.getId())
                .code(rule.getCode())
                .name(rule.getName())
                .state(Rule.RuleState.PUBLISHED)
                .ruleVersion(rule.getRuleVersion())
                .latestVersion(rule.getLatestVersion())
                .logic(rule.getLogic())
                .limits(rule.getLimits())
                .nodes(validationNodes)
                .publishedAt(now)
                .publishedBy("rule-publishing-service")
                .createdAt(rule.getCreatedAt())
                .createdBy(rule.getCreatedBy())
                .updatedAt(now)
                .updatedBy("rule-publishing-service")
                .build();

        logger.info("Created Rule from Rule: ruleId={}, validationRuleId={}",
                rule.getId(), validationRule.getId());

        return validationRule;
    }

    private RuleNode convertRuleNodeToValidationNode(RuleNode ruleNode) {
        // Convert children recursively
        List<RuleNode> convertedChildren = new ArrayList<>();
        if (ruleNode.getChildren() != null) {
            convertedChildren = ruleNode.getChildren().stream()
                    .map(this::convertRuleNodeToValidationNode)
                    .collect(Collectors.toList());
        }

        return RuleNode.builder()
                .nodeId(ruleNode.getId())
                .type(ruleNode.getType())
                .groupLogic(ruleNode.getGroupLogic())
                .children(convertedChildren)
                .operatorName(ruleNode.getOperatorName())
                .params(ruleNode.getParams())
                .reasonCode(ruleNode.getReasonCode())
                .build();
    }

    // Result classes
    public static class RulePublishResult {
        private final String ruleId;
        private final boolean success;
        private final String bundleHash;
        private final Long artifactSize;
        private final String errorMessage;

        private RulePublishResult(String ruleId, boolean success, String bundleHash,
                                  Long artifactSize, String errorMessage) {
            this.ruleId = ruleId;
            this.success = success;
            this.bundleHash = bundleHash;
            this.artifactSize = artifactSize;
            this.errorMessage = errorMessage;
        }

        public static RulePublishResult success(String ruleId, String bundleHash, Long artifactSize) {
            return new RulePublishResult(ruleId, true, bundleHash, artifactSize, null);
        }

        public static RulePublishResult failed(String ruleId, String errorMessage) {
            return new RulePublishResult(ruleId, false, null, null, errorMessage);
        }

        // Getters
        public String getRuleId() {
            return ruleId;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getBundleHash() {
            return bundleHash;
        }

        public Long getArtifactSize() {
            return artifactSize;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    public static class RuleDeploymentStatus {
        private final String ruleId;
        private final String status;
        private final boolean deployed;
        private final String bundleHash;

        public RuleDeploymentStatus(String ruleId, String status, boolean deployed, String bundleHash) {
            this.ruleId = ruleId;
            this.status = status;
            this.deployed = deployed;
            this.bundleHash = bundleHash;
        }

        // Getters
        public String getRuleId() {
            return ruleId;
        }

        public String getStatus() {
            return status;
        }

        public boolean isDeployed() {
            return deployed;
        }

        public String getBundleHash() {
            return bundleHash;
        }
    }
}