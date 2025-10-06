package vn.viettel.vds.promotion.validation.domain.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.viettel.vds.promotion.validation.adapter.out.integration.ValidationEngineClient;
import vn.viettel.vds.promotion.validation.adapter.out.integration.dto.*;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.mongo.entity.ValidationRule;
import vn.viettel.vds.promotion.validation.application.port.out.RulePersistencePort;
import vn.viettel.vds.promotion.validation.application.port.out.ValidationRuleEntityPersistencePort;
import vn.viettel.vds.promotion.validation.domain.entity.Rule;

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

    public RulePublishingService(ValidationEngineClient validationEngineClient,
                                 RulePersistencePort rulePersistencePort,
                                 ValidationRuleEntityPersistencePort validationRuleEntityPersistencePort) {
        this.validationEngineClient = validationEngineClient;
        this.rulePersistencePort = rulePersistencePort;
        this.validationRuleEntityPersistencePort = validationRuleEntityPersistencePort;
    }

    public RulePublishResult publishRule(String tenantId, String ruleId) {
        logger.info("Publishing rule: tenantId={}, ruleId={}", tenantId, ruleId);

        try {
            // Find the rule to publish
            Rule rule = rulePersistencePort.findByTenantIdAndCode(tenantId, ruleId)
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

            // Create ValidationRule from published Rule for SettingValidationRuleCommandHandler
            ValidationRule validationRule = createValidationRuleFromRule(rule, compileResponse.getBundleHash());
            validationRuleEntityPersistencePort.save(validationRule);

            logger.info("Rule published successfully: ruleId={}, bundleHash={}, validationRuleId={}",
                    ruleId, compileResponse.getBundleHash(), validationRule.getId());

            return RulePublishResult.success(ruleId, compileResponse.getBundleHash(), compileResponse.getArtifactSize());

        } catch (Exception e) {
            logger.error("Failed to publish rule: tenantId={}, ruleId={}", tenantId, ruleId, e);
            return RulePublishResult.failed(ruleId, "Publishing failed: " + e.getMessage());
        }
    }

    public List<RulePublishResult> publishRuleBatch(String tenantId, List<String> ruleIds) {
        logger.info("Publishing rule batch: tenantId={}, count={}", tenantId, ruleIds.size());

        List<RulePublishResult> results = new ArrayList<>();

        for (String ruleId : ruleIds) {
            try {
                RulePublishResult result = publishRule(tenantId, ruleId);
                results.add(result);

                // If critical rule fails, consider stopping batch
                if (!result.isSuccess() && isCriticalRule(tenantId, ruleId)) {
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

    public RulePublishResult unpublishRule(String tenantId, String ruleId) {
        logger.info("Unpublishing rule: tenantId={}, ruleId={}", tenantId, ruleId);

        try {
            Rule rule = rulePersistencePort.findByTenantIdAndCode(tenantId, ruleId)
                    .orElseThrow(() -> new IllegalArgumentException("Rule not found: " + ruleId));

            if (rule.getState() != Rule.RuleState.PUBLISHED) {
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
            logger.error("Failed to unpublish rule: tenantId={}, ruleId={}", tenantId, ruleId, e);
            return RulePublishResult.failed(ruleId, "Unpublishing failed: " + e.getMessage());
        }
    }

    public RuleDeploymentStatus getDeploymentStatus(String tenantId, String ruleId) {
        try {
            Rule rule = rulePersistencePort.findByTenantIdAndCode(tenantId, ruleId)
                    .orElseThrow(() -> new IllegalArgumentException("Rule not found: " + ruleId));

            if (rule.getState() != Rule.RuleState.PUBLISHED) {
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
            logger.error("Failed to get deployment status: tenantId={}, ruleId={}", tenantId, ruleId, e);
            return new RuleDeploymentStatus(ruleId, "ERROR", false, null);
        }
    }

    private void validateRuleForPublishing(Rule rule) {
        if (rule.getNodes() == null || rule.getNodes().isEmpty()) {
            throw new IllegalArgumentException("Rule has no nodes defined");
        }

        if (rule.getTenantId() == null || rule.getTenantId().isBlank()) {
            throw new IllegalArgumentException("Rule must have a tenant ID");
        }

        // Validate node structure
        validateNodeStructure(rule.getNodes());
    }

    private void validateNodeStructure(List<Rule.RuleNode> nodes) {
        Map<String, Rule.RuleNode> nodeMap = new HashMap<>();
        for (Rule.RuleNode node : nodes) {
            if (node.getId() == null || node.getId().isBlank()) {
                throw new IllegalArgumentException("Node must have an ID");
            }
            if (node.getType() == null) {
                throw new IllegalArgumentException("Node must have a type");
            }
            nodeMap.put(node.getId(), node);
        }

        // Validate references
        for (Rule.RuleNode node : nodes) {
            if (Rule.RuleNode.NodeType.GROUP.equals(node.getType()) && node.getChildren() != null) {
                for (Rule.RuleNode childNode : node.getChildren()) {
                    if (!nodeMap.containsKey(childNode.getId())) {
                        throw new IllegalArgumentException("Node references non-existent child: " + childNode.getId());
                    }
                }
            }
        }
    }

    private CompileResponse compileRuleInEngine(Rule rule) {
        List<RuleNodeDto> nodeDtos = convertToNodeDtos(rule.getNodes());

        CompileRequest compileRequest = new CompileRequest();
        compileRequest.setTenantId(rule.getTenantId());
        compileRequest.setRuleId(rule.getId());
        compileRequest.setVersion(rule.getLatestVersion());
        compileRequest.setNodes(nodeDtos);
        compileRequest.setOperatorsFingerprint(generateOperatorFingerprint(rule.getNodes()));

        return validationEngineClient.compile(compileRequest);
    }

    private List<RuleNodeDto> convertToNodeDtos(List<Rule.RuleNode> nodes) {
        List<RuleNodeDto> dtos = new ArrayList<>();

        for (Rule.RuleNode node : nodes) {
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
                for (Rule.RuleNode child : node.getChildren()) {
                    childIds.add(child.getId());
                }
                dto.setChildren(childIds);
            }
            // dto.setOrder(node.getOrder()); // Not available in current model

            dtos.add(dto);
        }

        return dtos;
    }

    private String generateOperatorFingerprint(List<Rule.RuleNode> nodes) {
        // Generate a fingerprint based on operators used
        StringBuilder fingerprint = new StringBuilder();
        for (Rule.RuleNode node : nodes) {
            if (node.getOperatorName() != null) {
                fingerprint.append(node.getOperatorName())
                        .append(":")
                        .append("latest") // operatorVersion not available in current model
                        .append(";");
            }
        }
        return fingerprint.toString();
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
        context.setTenantId(rule.getTenantId());
        // context.setTimestamp(System.currentTimeMillis()); // Method not available in DTO

        request.setCustomer(customer);
        request.setOrder(order);
        request.setCandidate(candidate);
        request.setExecutionContext(context);

        return request;
    }

    private boolean isCriticalRule(String tenantId, String ruleId) {
        // Implement logic to determine if a rule is critical
        // For now, assume all rules are non-critical
        return false;
    }

    private ValidationRule createValidationRuleFromRule(Rule rule, String bundleHash) {
        ValidationRule validationRule = new ValidationRule();

        // Use rule ID as ValidationRule ID for SettingValidationRuleCommandHandler lookup
        validationRule.setId(rule.getId());
        validationRule.setCode(rule.getCode());
        validationRule.setName(rule.getName());
        validationRule.setState("published");
        validationRule.setVersion(rule.getLatestVersion());
        validationRule.setLogic(rule.getLogic() != null ? rule.getLogic().name() : null);

        // Convert limits
        if (rule.getLimits() != null) {
            Map<String, Object> limitsMap = rule.getLimits();
            ValidationRule.UsageLimits limits = new ValidationRule.UsageLimits(
                    (Integer) limitsMap.get("perCodeTotal"),
                    (Integer) limitsMap.get("perCustomer"),
                    (Integer) limitsMap.get("perDay")
            );
            validationRule.setLimits(limits);
        }

        // Convert nodes
        if (rule.getNodes() != null) {
            List<ValidationRule.RuleNode> validationNodes = rule.getNodes().stream()
                    .map(this::convertRuleNodeToValidationNode)
                    .collect(Collectors.toList());
            validationRule.setNodes(validationNodes);
        }

        // Set timestamps
        Instant now = Instant.now();
        validationRule.setPublishedAt(now);
        validationRule.setPublishedBy("rule-publishing-service");
        validationRule.setCreatedAt(rule.getCreatedAt());
        validationRule.setCreatedBy(rule.getCreatedBy());
        validationRule.setUpdatedAt(now);
        validationRule.setUpdatedBy("rule-publishing-service");

        logger.info("Created ValidationRule from Rule: ruleId={}, validationRuleId={}",
                rule.getId(), validationRule.getId());

        return validationRule;
    }

    private ValidationRule.RuleNode convertRuleNodeToValidationNode(Rule.RuleNode ruleNode) {
        ValidationRule.RuleNode validationNode = new ValidationRule.RuleNode();
        validationNode.setId(ruleNode.getId());
        validationNode.setType(ruleNode.getType() != null ? ruleNode.getType().name() : null);
        validationNode.setGroupLogic(ruleNode.getGroupLogic() != null ? ruleNode.getGroupLogic().name() : null);

        // Convert children from List<RuleNode> to List<String> (IDs only)
        if (ruleNode.getChildren() != null) {
            List<String> childIds = ruleNode.getChildren().stream()
                    .map(Rule.RuleNode::getId)
                    .collect(Collectors.toList());
            validationNode.setChildren(childIds);
        }

        validationNode.setOperatorName(ruleNode.getOperatorName());
        validationNode.setParams(ruleNode.getParams());
        validationNode.setReasonCode(ruleNode.getReasonCode());
        // Note: Rule.RuleNode doesn't have order field, so we skip it
        return validationNode;
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