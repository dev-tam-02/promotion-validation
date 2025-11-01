package vn.viettel.vds.promotion.validation.domain.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.viettel.vds.promotion.validation.adapter.out.integration.ValidationEngineClient;
import vn.viettel.vds.promotion.validation.adapter.out.integration.dto.*;
import vn.viettel.vds.promotion.validation.application.port.out.RulePersistencePort;
import vn.viettel.vds.promotion.validation.domain.exception.BundleWarmupException;
import vn.viettel.vds.promotion.validation.domain.exception.RuleCompilationException;
import vn.viettel.vds.promotion.validation.domain.exception.RuleExecutionException;
import vn.viettel.vds.promotion.validation.domain.model.Rule;
import vn.viettel.vds.promotion.validation.domain.model.RuleNode;
import vn.viettel.vds.promotion.validation.domain.model.RuleVersion;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class RulePublishingService {

    private static final Logger logger = LoggerFactory.getLogger(RulePublishingService.class);
    private static final String RULE_NOT_FOUND_MESSAGE = "Rule not found: ";
    private static final String NULL_RESPONSE = "null response";

    private final ValidationEngineClient validationEngineClient;
    private final RulePersistencePort rulePersistencePort;
    private final vn.viettel.vds.promotion.validation.application.port.out.RuleVersionPersistencePort ruleVersionPersistencePort;
    private final vn.viettel.vds.promotion.validation.config.TenantProperties tenantProperties;

    public RulePublishingService(ValidationEngineClient validationEngineClient,
                                 RulePersistencePort rulePersistencePort,
                                 vn.viettel.vds.promotion.validation.application.port.out.RuleVersionPersistencePort ruleVersionPersistencePort,
                                 vn.viettel.vds.promotion.validation.config.TenantProperties tenantProperties) {
        this.validationEngineClient = validationEngineClient;
        this.rulePersistencePort = rulePersistencePort;
        this.ruleVersionPersistencePort = ruleVersionPersistencePort;
        this.tenantProperties = tenantProperties;
    }

    public RulePublishResult publishRule(String ruleId) {
        logger.info("Publishing rule: ruleId={}", ruleId);

        try {
            Rule rule = loadRuleForPublishing(ruleId);
            validateRuleForPublishing(rule);

            CompileResponse compileResponse = compileRuleInEngine(rule);
            if (!compileResponse.isOk()) {
                return handleCompilationFailure(ruleId, compileResponse);
            }

            performRulePublishingSteps(rule, compileResponse);

            return createSuccessfulPublishResult(ruleId, compileResponse);

        } catch (Exception e) {
            logger.error("Failed to publish rule: ruleId={}", ruleId, e);
            return RulePublishResult.failed(ruleId, "Publishing failed: " + e.getMessage());
        }
    }

    private Rule loadRuleForPublishing(String ruleId) {
        return rulePersistencePort.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException(RULE_NOT_FOUND_MESSAGE + ruleId));
    }

    private RulePublishResult handleCompilationFailure(String ruleId, CompileResponse compileResponse) {
        List<String> errors = compileResponse.getErrors();
        String errorMessage = (errors != null && !errors.isEmpty())
                ? String.join(", ", errors)
                : "Unknown compilation error";
        logger.error("Rule compilation failed: ruleId={}, errors={}", ruleId, errorMessage);
        return RulePublishResult.failed(ruleId, "Compilation failed: " + errorMessage);
    }

    private void performRulePublishingSteps(Rule rule, CompileResponse compileResponse) {
        warmup(compileResponse);

        boolean verificationResult = verifyRuleExecution(rule, compileResponse.getBundleHash());
        if (!verificationResult) {
            throw new IllegalStateException("Rule verification failed: " + rule.getId());
        }

        updateRuleToPublishedState(rule, compileResponse);
    }

    private void updateRuleToPublishedState(Rule rule, CompileResponse compileResponse) {
        Instant now = Instant.now();

        // Update Rule state
        rule.setState(Rule.RuleState.PUBLISHED);
        rule.setPublishedAt(now);
        rule.setPublishedBy("rule-publishing-service");
        rule.setUpdatedAt(now);
        rulePersistencePort.save(rule);

        // Save bundleHash to RuleVersion
        saveRuleVersionWithBundleHash(rule, compileResponse, now);

        logger.info("Rule published successfully: ruleId={}, bundleHash={}, artifactSize={}",
                rule.getId(), compileResponse.getBundleHash(), compileResponse.getArtifactSize());
    }

    private void saveRuleVersionWithBundleHash(Rule rule, CompileResponse compileResponse, Instant now) {
        try {
            // Determine version number
            Integer versionNumber = determineRuleVersion(rule);

            // Query existing RuleVersion or create new one
            RuleVersion ruleVersion = ruleVersionPersistencePort
                    .findByRuleIdAndVersion(rule.getId(), versionNumber)
                    .orElseGet(() -> createNewRuleVersion(rule, versionNumber, now));

            // Update compile info with bundleHash
            RuleVersion.CompileInfo compileInfo = RuleVersion.CompileInfo.builder()
                    .status(RuleVersion.CompileInfo.CompileStatus.SUCCESS)
                    .compilerId("validation-engine")
                    .bundleHash(compileResponse.getBundleHash())
                    .logs(compileResponse.getErrors() != null ? compileResponse.getErrors() : List.of())
                    .build();

            // Update RuleVersion with compile info and published metadata
            RuleVersion updatedRuleVersion = ruleVersion.toBuilder()
                    .compile(compileInfo)
                    .publishedAt(now)
                    .publishedBy("rule-publishing-service")
                    .updatedAt(now)
                    .build();

            ruleVersionPersistencePort.save(updatedRuleVersion);

            logger.info("Saved bundleHash to RuleVersion: ruleId={}, version={}, bundleHash={}",
                    rule.getId(), versionNumber, compileResponse.getBundleHash());

        } catch (Exception e) {
            logger.error("Failed to save bundleHash to RuleVersion: ruleId={}, error={}",
                    rule.getId(), e.getMessage(), e);
            // Don't fail the entire publish operation if version save fails
        }
    }

    private RuleVersion createNewRuleVersion(Rule rule, Integer versionNumber, Instant now) {
        return RuleVersion.builder()
                .id(com.promix.platform.core.util.IdGenerator.generateId())
                .ruleId(rule.getId())
                .code(rule.getCode())
                .version(versionNumber)
                .logic(rule.getLogic() != null ? RuleVersion.LogicType.valueOf(rule.getLogic().name()) : null)
                .nodes(convertNodesToMaps(rule.getNodes()))
                .operatorsFingerprint(generateOperatorFingerprint(rule.getNodes()))
                .dsl(rule.getDsl())
                .createdAt(now)
                .createdBy("rule-publishing-service")
                .updatedAt(now)
                .build();
    }

    private List<Map<String, Object>> convertNodesToMaps(List<RuleNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return List.of();
        }

        return nodes.stream()
                .map(this::convertNodeToMap)
                .toList();
    }

    private Map<String, Object> convertNodeToMap(RuleNode node) {
        Map<String, Object> nodeMap = new java.util.HashMap<>();
        nodeMap.put("id", node.getId());
        nodeMap.put("type", node.getType() != null ? node.getType().name() : null);
        nodeMap.put("operatorName", node.getOperatorName());
        nodeMap.put("params", node.getParams());
        nodeMap.put("reasonCode", node.getReasonCode());

        if (node.getGroupLogic() != null) {
            nodeMap.put("groupLogic", node.getGroupLogic().name());
        }

        if (node.getChildren() != null && !node.getChildren().isEmpty()) {
            List<String> childIds = node.getChildren().stream()
                    .map(RuleNode::getId)
                    .toList();
            nodeMap.put("children", childIds);
        }

        return nodeMap;
    }

    private RulePublishResult createSuccessfulPublishResult(String ruleId, CompileResponse compileResponse) {
        return RulePublishResult.success(ruleId, compileResponse.getBundleHash(), compileResponse.getArtifactSize());
    }

    public List<RulePublishResult> publishRuleBatch(List<String> ruleIds) {
        logger.info("Publishing rule batch: count={}", ruleIds.size());

        List<RulePublishResult> results = new ArrayList<>();

        for (String ruleId : ruleIds) {
            try {
                RulePublishResult result = publishRule(ruleId);
                results.add(result);

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
                    .orElseThrow(() -> new IllegalArgumentException(RULE_NOT_FOUND_MESSAGE + ruleId));

            if (Rule.RuleState.PUBLISHED != rule.getState()) {
                return RulePublishResult.failed(ruleId, "Rule is not published, cannot unpublish");
            }

            // Update rule status
            rule.setState(Rule.RuleState.DRAFT);
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
                    .orElseThrow(() -> new IllegalArgumentException(RULE_NOT_FOUND_MESSAGE + ruleId));

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
        Map<String, RuleNode> nodeMap = buildNodeMap(nodes);
        validateNodeReferences(nodes, nodeMap);
    }

    private Map<String, RuleNode> buildNodeMap(List<RuleNode> nodes) {
        Map<String, RuleNode> nodeMap = new HashMap<>();
        for (RuleNode node : nodes) {
            validateNode(node);
            nodeMap.put(node.getId(), node);
        }
        return nodeMap;
    }

    private void validateNode(RuleNode node) {
        if (node.getId() == null || node.getId().isBlank()) {
            throw new IllegalArgumentException("Node must have an ID");
        }
        if (node.getType() == null) {
            throw new IllegalArgumentException("Node must have a type");
        }
    }

    private void validateNodeReferences(List<RuleNode> nodes, Map<String, RuleNode> nodeMap) {
        for (RuleNode node : nodes) {
            if (RuleNode.NodeType.GROUP.equals(node.getType()) && node.getChildren() != null) {
                validateChildren(node, nodeMap);
            }
        }
    }

    private void validateChildren(RuleNode node, Map<String, RuleNode> nodeMap) {
        for (RuleNode childNode : node.getChildren()) {
            if (!nodeMap.containsKey(childNode.getId())) {
                throw new IllegalArgumentException("Node references non-existent child: " + childNode.getId());
            }
        }
    }

    private CompileResponse compileRuleInEngine(Rule rule) {
        // Convert nodes to DTOs using new API
        List<RuleNodeDto> nodeDtos = convertToNodeDtos(rule.getNodes());

        // Determine version and logic
        Integer version = determineRuleVersion(rule);
        String logic = determineRootLogic(rule);

        CompileRequest compileRequest = buildCompileRequest(rule, version, logic, nodeDtos);

        // Note: Removed compilerId, Source, Limits, timeLinks - not part of new simplified API
        // These are business logic concerns, not compilation concerns

        com.promix.platform.web.template.ResponseTemplate<CompileResponse> responseTemplate = validationEngineClient.compile(compileRequest);
        if (responseTemplate == null || !responseTemplate.isSuccess() || responseTemplate.getData() == null) {
            throw new RuleCompilationException("Failed to compile rule: " + (responseTemplate != null ? responseTemplate.getMessage() : NULL_RESPONSE));
        }
        return responseTemplate.getData();
    }

    private Integer determineRuleVersion(Rule rule) {
        // Determine version - use latestVersion if available, otherwise use ruleVersion or default to 1
        Integer version = rule.getLatestVersion();
        if (version == null) {
            version = rule.getRuleVersion() != null ? rule.getRuleVersion().intValue() : 1;
        }
        return version;
    }

    private String determineRootLogic(Rule rule) {
        // Determine root logic - use rule logic if available, default to ALL
        return rule.getLogic() != null ? rule.getLogic().name() : "ALL";
    }

    private CompileRequest buildCompileRequest(Rule rule, Integer version, String logic, List<RuleNodeDto> nodeDtos) {
        CompileRequest compileRequest = new CompileRequest();
        compileRequest.setTenantId(tenantProperties.getDefaultTenantId());
        compileRequest.setRuleId(rule.getId());
        compileRequest.setVersion(version);
        compileRequest.setLogic(logic);
        compileRequest.setNodes(nodeDtos);
        compileRequest.setOperatorsFingerprint(generateOperatorFingerprint(rule.getNodes()));
        return compileRequest;
    }

    private List<RuleNodeDto> convertToNodeDtos(List<RuleNode> nodes) {
        List<RuleNodeDto> dtos = new ArrayList<>();

        for (RuleNode node : nodes) {
            // Convert children nodes to IDs
            List<String> childIds = convertChildrenToIds(node);

            // Create RuleNodeDto using setters (it's a class, not a record)
            RuleNodeDto dto = createRuleNodeDto(node, childIds);

            dtos.add(dto);
        }

        return dtos;
    }

    private List<String> convertChildrenToIds(RuleNode node) {
        List<String> childIds = null;
        if (node.getChildren() != null && !node.getChildren().isEmpty()) {
            childIds = new ArrayList<>();
            for (RuleNode child : node.getChildren()) {
                childIds.add(child.getId());
            }
        }
        return childIds;
    }

    private RuleNodeDto createRuleNodeDto(RuleNode node, List<String> childIds) {
        RuleNodeDto dto = new RuleNodeDto();
        dto.setId(node.getId());
        dto.setType(node.getType() != null ? node.getType().name() : null);
        dto.setGroupLogic(node.getGroupLogic() != null ? node.getGroupLogic().name() : null);
        dto.setOperatorName(node.getOperatorName());
        dto.setOperatorVersion(null); // Not available in current model
        dto.setParams(node.getParams());
        dto.setReasonCode(node.getReasonCode());
        dto.setChildren(childIds);
        dto.setOrder(null); // Not available in current model
        return dto;
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
        String result = fingerprint.toString();
        return result.isEmpty() ? "default-fingerprint" : result;
    }

    private void warmup(CompileResponse compileResponse) {
        // Validate artifact bytes are present
        if (compileResponse.getArtifactBytes() == null) {
            String errorMsg = String.format(
                    "Cannot warm up bundle: artifact bytes are missing. BundleHash=%s. " +
                            "This indicates a storage retrieval failure in the validation engine.",
                    compileResponse.getBundleHash()
            );
            logger.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        try {
            WarmupRequest warmupRequest = new WarmupRequest(
                    compileResponse.getBundleHash(),
                    compileResponse.getArtifactBytes()
            );

            com.promix.platform.web.template.ResponseTemplate<WarmupResponse> warmupResponseTemplate = validationEngineClient.warmup(warmupRequest);
            if (warmupResponseTemplate == null || !warmupResponseTemplate.isSuccess() || warmupResponseTemplate.getData() == null) {
                throw new BundleWarmupException("Failed to warmup bundle: " + (warmupResponseTemplate != null ? warmupResponseTemplate.getMessage() : NULL_RESPONSE));
            }
            WarmupResponse warmupResponse = warmupResponseTemplate.getData();

            if (warmupResponse == null || !warmupResponse.isOk()) {
                String errorMsg = String.format(
                        "Bundle warmup failed: bundleHash=%s, response=%s",
                        compileResponse.getBundleHash(),
                        warmupResponse != null ? warmupResponse.toString() : "null"
                );
                logger.error(errorMsg);
                throw new IllegalStateException(errorMsg);
            }

            logger.info("Bundle warmed up successfully: bundleHash={}, artifactSize={}",
                    compileResponse.getBundleHash(),
                    compileResponse.getArtifactBytes().length);

        } catch (Exception e) {
            throw new IllegalStateException("Failed to warm up bundle: bundleHash=" + compileResponse.getBundleHash() + ", error=" + e.getMessage(), e);
        }
    }

    private boolean verifyRuleExecution(Rule rule, String bundleHash) {
        try {
            // Create a simple test execution to verify the rule works
            ExecuteRequest testRequest = createTestExecuteRequest(bundleHash);
            com.promix.platform.web.template.ResponseTemplate<ExecuteResponse> responseTemplate = validationEngineClient.execute(testRequest);
            if (responseTemplate == null || !responseTemplate.isSuccess() || responseTemplate.getData() == null) {
                throw new RuleExecutionException("Failed to execute test: " + (responseTemplate != null ? responseTemplate.getMessage() : NULL_RESPONSE));
            }
            ExecuteResponse response = responseTemplate.getData();

            boolean isValid = isValidResponse(response);

            logger.debug("Rule verification completed: ruleId={}, valid={}, decision={}",
                    rule.getId(), isValid, response != null ? response.getDecision() : null);

            return isValid;

        } catch (Exception e) {
            logger.error("Rule verification failed: ruleId={}", rule.getId(), e);
            return false;
        }
    }

    private boolean isValidResponse(ExecuteResponse response) {
        // The 'ok' field represents the business decision (ALLOW=true, DENY=false), not execution success
        // Verification passes if we get a valid decision (ALLOW or DENY) and engine info is present
        return response != null &&
                response.getDecision() != null &&
                (response.getDecision().equals("ALLOW") || response.getDecision().equals("DENY")) &&
                response.getEngine() != null;
    }

    private ExecuteRequest createTestExecuteRequest(String bundleHash) {
        ExecuteRequest request = new ExecuteRequest();
        request.setBundleHash(bundleHash);

        // Create minimal test data using record constructors
        CustomerDto customer = new CustomerDto(
                "test-customer",
                null,  // segments
                null,  // region
                1,     // tier
                null   // metadata
        );

        OrderDto order = new OrderDto(
                "test-order",
                java.math.BigDecimal.valueOf(100000.0),
                "VND",
                null,  // items
                null   // metadata
        );

        CandidateDto candidate = new CandidateDto(
                "test-candidate",
                "voucher",
                null  // metadata
        );

        ExecutionContextDto context = new ExecutionContextDto();
        context.setNow(Instant.now());
        context.setTimezone("Asia/Bangkok");

        request.setCustomer(customer);
        request.setOrder(order);
        request.setCandidate(candidate);
        request.setExecutionContext(context);

        return request;
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