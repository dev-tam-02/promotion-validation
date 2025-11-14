package vn.viettel.vds.promotion.validation.domain.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.viettel.vds.promotion.validation.adapter.out.integration.ValidationEngineClient;
import vn.viettel.vds.promotion.validation.adapter.out.integration.dto.*;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.RuleTemporalLinkEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.TemporalPolicyEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.TemporalPolicyWindowEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.RuleTemporalLinkJpaRepository;
import vn.viettel.vds.promotion.validation.application.port.out.RulePersistencePort;
import vn.viettel.vds.promotion.validation.domain.exception.BundleWarmupException;
import vn.viettel.vds.promotion.validation.domain.exception.RuleCompilationException;
import vn.viettel.vds.promotion.validation.domain.exception.RuleExecutionException;
import vn.viettel.vds.promotion.validation.domain.model.Rule;
import vn.viettel.vds.promotion.validation.domain.model.RuleNode;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional
public class RulePublishingService {

    private static final Logger logger = LoggerFactory.getLogger(RulePublishingService.class);
    private static final String RULE_NOT_FOUND_MESSAGE = "Rule not found: ";
    private static final String NULL_RESPONSE = "null response";

    private final ValidationEngineClient validationEngineClient;
    private final RulePersistencePort rulePersistencePort;
    private final vn.viettel.vds.promotion.validation.config.TenantProperties tenantProperties;
    private final RuleTemporalLinkJpaRepository ruleTemporalLinkRepository;

    public RulePublishingService(ValidationEngineClient validationEngineClient,
                                 RulePersistencePort rulePersistencePort,
                                 vn.viettel.vds.promotion.validation.config.TenantProperties tenantProperties,
                                 RuleTemporalLinkJpaRepository ruleTemporalLinkRepository) {
        this.validationEngineClient = validationEngineClient;
        this.rulePersistencePort = rulePersistencePort;
        this.tenantProperties = tenantProperties;
        this.ruleTemporalLinkRepository = ruleTemporalLinkRepository;
    }

    public RulePublishResult publishRule(String ruleId) {
        return publishRule(ruleId, null, null);
    }

    public RulePublishResult publishRule(String ruleId, String assignmentId) {
        return publishRule(ruleId, assignmentId, null);
    }

    public RulePublishResult publishRule(String ruleId, String assignmentId,
                                        vn.viettel.vds.promotion.validation.command.SettingValidationRuleCommand.ApplicabilityScope applicableToData) {
        logger.info("Publishing rule: ruleId={}, assignmentId={}, hasApplicability={}",
                ruleId, assignmentId, applicableToData != null);

        try {
            Rule rule = loadRuleForPublishing(ruleId);
            validateRuleForPublishing(rule);

            CompileResponse compileResponse = compileRuleInEngine(rule, assignmentId, applicableToData);
            if (!compileResponse.isOk()) {
                return handleCompilationFailure(ruleId, compileResponse);
            }

            performRulePublishingSteps(rule, compileResponse);

            return createSuccessfulPublishResult(ruleId, compileResponse);

        } catch (Exception e) {
            logger.error("Failed to publish rule: ruleId={}, assignmentId={}", ruleId, assignmentId, e);
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
        rule.setState(Rule.RuleState.PUBLISHED);
        rule.setPublishedAt(Instant.now());
        rule.setPublishedBy("rule-publishing-service");
        rule.setUpdatedAt(Instant.now());
        // Save bundleHash directly to rule
        rule.setBundleHash(compileResponse.getBundleHash());
        rulePersistencePort.save(rule);

        logger.info("Rule published successfully: ruleId={}, bundleHash={}, artifactSize={}",
                rule.getId(), compileResponse.getBundleHash(), compileResponse.getArtifactSize());
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
            collectAllNodes(node, nodeMap);
        }
        return nodeMap;
    }

    private void collectAllNodes(RuleNode node, Map<String, RuleNode> nodeMap) {
        validateNode(node);
        nodeMap.put(node.getId(), node);

        // Recursively collect children
        if (node.getChildren() != null) {
            for (RuleNode child : node.getChildren()) {
                collectAllNodes(child, nodeMap);
            }
        }
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

    private CompileResponse compileRuleInEngine(Rule rule, String assignmentId,
                                                vn.viettel.vds.promotion.validation.command.SettingValidationRuleCommand.ApplicabilityScope applicableToData) {
        // Build full nodes list: existing nodes + dynamic product applicability node
        List<RuleNodeDto> fullNodeDtos = buildFullNodeList(rule, applicableToData);

        // Determine version and logic
        Integer version = determineRuleVersion(rule);
        String logic = determineRootLogic(rule);

        CompileRequest compileRequest = buildCompileRequest(rule, version, logic, fullNodeDtos, assignmentId);

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

    private CompileRequest buildCompileRequest(Rule rule, Integer version, String logic, List<RuleNodeDto> nodeDtos, String assignmentId) {
        CompileRequest compileRequest = new CompileRequest();
        compileRequest.setTenantId(tenantProperties.getDefaultTenantId());
        compileRequest.setRuleId(rule.getId());
        compileRequest.setVersion(version);
        compileRequest.setLogic(logic);
        compileRequest.setNodes(nodeDtos);
        compileRequest.setOperatorsFingerprint(generateOperatorFingerprint(rule.getNodes()));

        // Add temporal policy data if assignmentId is provided
        if (assignmentId != null) {
            List<RuleTemporalLinkEntity> temporalLinks = ruleTemporalLinkRepository.findByAssignmentId(assignmentId);

            if (!temporalLinks.isEmpty()) {
                logger.info("Found {} temporal links for assignmentId={}", temporalLinks.size(), assignmentId);

                RuleTemporalLinkEntity link = temporalLinks.get(0); // Assumption: 1 policy per assignment
                TemporalPolicyEntity policy = link.getTemporalPolicy();

                // Build TemporalPolicyData
                CompileRequest.TemporalPolicyData temporalData = buildTemporalPolicyData(policy);

                // Create TimeLink and add to compile request
                CompileRequest.TimeLink timeLink = new CompileRequest.TimeLink(
                        policy.getId(),
                        link.getMode(),
                        temporalData
                );

                compileRequest.setTimeLinks(List.of(timeLink));

                logger.debug("Added temporal policy to compile request: policyId={}, mode={}, timezone={}",
                        policy.getId(), link.getMode(), policy.getTz());
            } else {
                logger.debug("No temporal links found for assignmentId={}", assignmentId);
            }
        }

        return compileRequest;
    }

    private CompileRequest.TemporalPolicyData buildTemporalPolicyData(TemporalPolicyEntity policy) {
        CompileRequest.TemporalPolicyData data = new CompileRequest.TemporalPolicyData();
        data.setTimezone(policy.getTz());
        data.setRrule(policy.getRrule());
        data.setStartTs(policy.getStartTs() != null ? policy.getStartTs().toString() : null);
        data.setEndTs(policy.getEndTs() != null ? policy.getEndTs().toString() : null);

        // Convert time-of-day windows
        List<CompileRequest.TimeWindow> windowDtos = policy.getTimeOfDayWindows().stream()
                .map(w -> new CompileRequest.TimeWindow(w.getStart(), w.getEnd()))
                .toList();
        data.setWindows(windowDtos);

        logger.debug("Built temporal policy data: timezone={}, rrule={}, windowsCount={}",
                policy.getTz(), policy.getRrule(), windowDtos.size());

        return data;
    }

    /**
     * Build full node list by merging existing nodes + dynamic product applicability node
     *
     * @param rule Rule entity with existing nodes
     * @param applicableToData Applicability scope data (can be null)
     * @return Full list of RuleNodeDto including dynamic nodes
     */
    private List<RuleNodeDto> buildFullNodeList(Rule rule,
                                                 vn.viettel.vds.promotion.validation.command.SettingValidationRuleCommand.ApplicabilityScope applicableToData) {
        // Convert existing nodes to DTOs
        List<RuleNodeDto> nodeDtos = convertToNodeDtos(rule.getNodes());

        // If no applicability data, return existing nodes as-is
        if (applicableToData == null) {
            logger.debug("No applicability data provided, using existing nodes only");
            return nodeDtos;
        }

        logger.info("Creating dynamic product applicability node for rule: ruleId={}", rule.getId());

        // Create product applicability node DTO
        RuleNodeDto productNode = createProductApplicabilityNodeDto(applicableToData);

        // Add product node to the list
        nodeDtos.add(productNode);

        // Find root node and add product node to its children
        addProductNodeToRootChildren(nodeDtos, productNode.getId());

        logger.info("Built full node list: ruleId={}, totalNodes={}, productNodeId={}",
                rule.getId(), nodeDtos.size(), productNode.getId());

        return nodeDtos;
    }

    /**
     * Create product applicability node DTO from ApplicabilityScope
     *
     * @param applicableToData Applicability scope with included/excluded products
     * @return RuleNodeDto for product.applicability.in operator
     */
    private RuleNodeDto createProductApplicabilityNodeDto(
            vn.viettel.vds.promotion.validation.command.SettingValidationRuleCommand.ApplicabilityScope applicableToData) {

        RuleNodeDto dto = new RuleNodeDto();
        dto.setId(com.promix.platform.core.util.IdGenerator.generateId());
        dto.setType("COND");
        dto.setOperatorName("order.item.product.applicable");
        dto.setOperatorVersion(1);
        dto.setReasonCode("PRODUCT_NOT_APPLICABLE");

        // Build params map
        Map<String, Object> params = new HashMap<>();

        if (Boolean.TRUE.equals(applicableToData.getIncludedAll())) {
            params.put("includeAll", true);
        } else {
            // Extract included product IDs
            if (applicableToData.getIncluded() != null && !applicableToData.getIncluded().isEmpty()) {
                List<String> includedIds = applicableToData.getIncluded().stream()
                        .map(vn.viettel.vds.promotion.validation.command.SettingValidationRuleCommand.ApplicabilityRule::getId)
                        .toList();
                params.put("include", includedIds);
            }

            // Extract excluded product IDs
            if (applicableToData.getExcluded() != null && !applicableToData.getExcluded().isEmpty()) {
                List<String> excludedIds = applicableToData.getExcluded().stream()
                        .map(vn.viettel.vds.promotion.validation.command.SettingValidationRuleCommand.ApplicabilityRule::getId)
                        .toList();
                params.put("exclude", excludedIds);
            }

            params.put("includeAll", false);
        }

        dto.setParams(params);

        logger.debug("Created product applicability node: nodeId={}, params={}", dto.getId(), params);

        return dto;
    }

    /**
     * Add product node ID to root node's children list
     *
     * @param nodeDtos List of all nodes
     * @param productNodeId ID of the product applicability node to add
     */
    private void addProductNodeToRootChildren(List<RuleNodeDto> nodeDtos, String productNodeId) {
        // Find root node (node that is not a child of any other node)
        Set<String> childIds = nodeDtos.stream()
                .filter(n -> n.getChildren() != null)
                .flatMap(n -> n.getChildren().stream())
                .collect(java.util.stream.Collectors.toSet());

        RuleNodeDto rootNode = nodeDtos.stream()
                .filter(n -> !childIds.contains(n.getId()))
                .findFirst()
                .orElse(null);

        if (rootNode != null && "GROUP".equals(rootNode.getType())) {
            // Add product node to root's children
            if (rootNode.getChildren() == null) {
                rootNode.setChildren(new ArrayList<>());
            }
            rootNode.getChildren().add(productNodeId);
            logger.debug("Added product node to root children: rootId={}, productNodeId={}",
                    rootNode.getId(), productNodeId);
        } else {
            logger.warn("Root node not found or not a GROUP node, product node added to list but not to tree");
        }
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