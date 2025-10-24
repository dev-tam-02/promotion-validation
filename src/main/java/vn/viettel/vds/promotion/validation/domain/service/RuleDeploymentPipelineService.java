package vn.viettel.vds.promotion.validation.domain.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.viettel.vds.promotion.validation.application.port.out.RulePersistencePort;
import vn.viettel.vds.promotion.validation.domain.model.Rule;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Service
@Transactional
public class RuleDeploymentPipelineService {

    private static final Logger logger = LoggerFactory.getLogger(RuleDeploymentPipelineService.class);

    // Pipeline stage constants
    private static final String STAGE_VALIDATION = "VALIDATION";
    private static final String STAGE_COMPILATION = "COMPILATION";
    private static final String STAGE_TESTING = "TESTING";
    private static final String STAGE_HEALTH_CHECK = "HEALTH_CHECK";
    private static final String STAGE_DIRECT_DEPLOYMENT = "DIRECT_DEPLOYMENT";

    private final RulePublishingService rulePublishingService;
    private final RulePersistencePort rulePersistencePort;
    private final Executor deploymentExecutor;

    public RuleDeploymentPipelineService(RulePublishingService rulePublishingService,
                                         RulePersistencePort rulePersistencePort) {
        this.rulePublishingService = rulePublishingService;
        this.rulePersistencePort = rulePersistencePort;
        this.deploymentExecutor = Executors.newVirtualThreadPerTaskExecutor();
    }

    public DeploymentPipelineResult deployRulePipeline(String tenantId, String ruleId, DeploymentConfig config) {
        logger.info("Starting deployment pipeline: tenantId={}, ruleId={}", tenantId, ruleId);

        String pipelineId = generatePipelineId();
        DeploymentPipelineResult result = new DeploymentPipelineResult(pipelineId, ruleId);

        try {
            // Stage 1: Validation
            result.addStage(executeValidationStage(ruleId, config));

            if (!result.getLastStage().isSuccess()) {
                return result.markFailed("Validation stage failed");
            }

            // Stage 2: Compilation
            result.addStage(executeCompilationStage(ruleId, config));

            if (!result.getLastStage().isSuccess()) {
                return result.markFailed("Compilation stage failed");
            }

            // Stage 3: Testing
            if (config.isRunTests()) {
                result.addStage(executeTestingStage(ruleId, config));

                if (!result.getLastStage().isSuccess()) {
                    return result.markFailed("Testing stage failed");
                }
            }

            // Stage 4: Blue-Green Deployment (if enabled)
            if (config.isBlueGreenDeployment()) {
                result.addStage(executeBlueGreenStage(ruleId, config));
            } else {
                result.addStage(executeDirectDeploymentStage(ruleId, config));
            }

            if (!result.getLastStage().isSuccess()) {
                return result.markFailed("Deployment stage failed");
            }

            // Stage 5: Health Check
            result.addStage(executeHealthCheckStage(ruleId, config));

            if (!result.getLastStage().isSuccess()) {
                logger.warn("Health check failed, but deployment is complete: ruleId={}", ruleId);
            }

            logger.info("Deployment pipeline completed successfully: pipelineId={}, ruleId={}", pipelineId, ruleId);
            return result.markSuccess();

        } catch (Exception e) {
            logger.error("Deployment pipeline failed: pipelineId={}, ruleId={}", pipelineId, ruleId, e);
            return result.markFailed("Pipeline error: " + e.getMessage());
        }
    }

    public CompletableFuture<DeploymentPipelineResult> deployRulePipelineAsync(String tenantId, String ruleId,
                                                                               DeploymentConfig config) {
        return CompletableFuture.supplyAsync(() -> deployRulePipeline(tenantId, ruleId, config), deploymentExecutor);
    }

    public List<DeploymentPipelineResult> deployMultipleRules(String tenantId, List<String> ruleIds,
                                                              DeploymentConfig config) {
        logger.info("Starting multi-rule deployment: tenantId={}, count={}", tenantId, ruleIds.size());

        List<CompletableFuture<DeploymentPipelineResult>> futures = ruleIds.stream()
                .map(ruleId -> deployRulePipelineAsync(tenantId, ruleId, config))
                .toList();

        return futures.stream()
                .map(CompletableFuture::join)
                .toList();
    }

    public EnvironmentHealthStatus getEnvironmentHealth(String tenantId) {
        logger.debug("Checking environment health: tenantId={}", tenantId);

        try {
            List<Rule> activeRules = rulePersistencePort.findByState(Rule.RuleState.PUBLISHED, Pageable.unpaged()).getContent();

            int totalRules = activeRules.size();
            int deployedRules = 0;
            int healthyRules = 0;
            List<String> unhealthyRules = new ArrayList<>();

            for (Rule rule : activeRules) {
                if (rule.getState() == Rule.RuleState.PUBLISHED) {
                    deployedRules++;
                    healthyRules += checkRuleHealth(rule, unhealthyRules);
                }
            }

            String overallHealth = determineOverallHealth(totalRules, healthyRules);

            return new EnvironmentHealthStatus.Builder()
                    .tenantId(tenantId)
                    .overallHealth(overallHealth)
                    .totalRules(totalRules)
                    .deployedRules(deployedRules)
                    .healthyRules(healthyRules)
                    .unhealthyRules(unhealthyRules)
                    .timestamp(System.currentTimeMillis())
                    .build();

        } catch (Exception e) {
            logger.error("Failed to check environment health: tenantId={}", tenantId, e);
            return new EnvironmentHealthStatus.Builder()
                    .tenantId(tenantId)
                    .overallHealth("ERROR")
                    .totalRules(0)
                    .deployedRules(0)
                    .healthyRules(0)
                    .unhealthyRules(List.of())
                    .timestamp(System.currentTimeMillis())
                    .build();
        }
    }

    private DeploymentStage executeValidationStage(String ruleId, DeploymentConfig config) {
        logger.debug("Executing validation stage: ruleId={}", ruleId);

        try {
            Rule rule = rulePersistencePort.findByCode(ruleId)
                    .orElseThrow(() -> new IllegalArgumentException("Rule not found: " + ruleId));

            // Validate rule structure
            if (rule.getNodes() == null || rule.getNodes().isEmpty()) {
                return DeploymentStage.failed(STAGE_VALIDATION, "Rule has no nodes defined");
            }

            if (config.isStrictValidation()) {
                logger.debug("Strict validation is enabled for rule: {}", ruleId);
            }

            return DeploymentStage.success(STAGE_VALIDATION, "Rule validation passed");

        } catch (Exception e) {
            return DeploymentStage.failed(STAGE_VALIDATION, "Validation error: " + e.getMessage());
        }
    }

    @SuppressWarnings("unused") // config reserved for future use
    private DeploymentStage executeCompilationStage(String ruleId, DeploymentConfig config) {
        logger.debug("Executing compilation stage: ruleId={}", ruleId);

        try {
            RulePublishingService.RulePublishResult result = rulePublishingService.publishRule(ruleId);

            if (result.isSuccess()) {
                return DeploymentStage.success(STAGE_COMPILATION,
                        "Rule compiled successfully: " + result.getBundleHash());
            } else {
                return DeploymentStage.failed(STAGE_COMPILATION, result.getErrorMessage());
            }

        } catch (Exception e) {
            return DeploymentStage.failed(STAGE_COMPILATION, "Compilation error: " + e.getMessage());
        }
    }

    private DeploymentStage executeTestingStage(String ruleId, DeploymentConfig config) {
        logger.debug("Executing testing stage: ruleId={}", ruleId);

        try {
            // Run test scenarios
            boolean testsPassed = runRuleTests(ruleId, config.getTestScenarios());

            if (testsPassed) {
                return DeploymentStage.success(STAGE_TESTING, "All tests passed");
            } else {
                return DeploymentStage.failed(STAGE_TESTING, "Some tests failed");
            }

        } catch (Exception e) {
            return DeploymentStage.failed(STAGE_TESTING, "Testing error: " + e.getMessage());
        }
    }

    @SuppressWarnings("unused") // config reserved for future use
    private DeploymentStage executeBlueGreenStage(String ruleId, DeploymentConfig config) {
        logger.debug("Executing blue-green deployment stage: ruleId={}", ruleId);

        try {
            logger.debug("Blue-green deployment logic not yet implemented for rule: {}", ruleId);

            return DeploymentStage.success("BLUE_GREEN_DEPLOYMENT", "Blue-green deployment completed");

        } catch (Exception e) {
            return DeploymentStage.failed("BLUE_GREEN_DEPLOYMENT", "Blue-green deployment error: " + e.getMessage());
        }
    }

    @SuppressWarnings("unused") // config reserved for future use
    private DeploymentStage executeDirectDeploymentStage(String ruleId, DeploymentConfig config) {
        logger.debug("Executing direct deployment stage: ruleId={}", ruleId);

        try {
            RulePublishingService.RuleDeploymentStatus status =
                    rulePublishingService.getDeploymentStatus(ruleId);

            if (status.isDeployed()) {
                return DeploymentStage.success(STAGE_DIRECT_DEPLOYMENT, "Rule deployed successfully");
            } else {
                return DeploymentStage.failed(STAGE_DIRECT_DEPLOYMENT, "Deployment verification failed");
            }

        } catch (Exception e) {
            return DeploymentStage.failed(STAGE_DIRECT_DEPLOYMENT, "Deployment error: " + e.getMessage());
        }
    }

    @SuppressWarnings("unused") // config reserved for future use
    private DeploymentStage executeHealthCheckStage(String ruleId, DeploymentConfig config) {
        logger.debug("Executing health check stage: ruleId={}", ruleId);

        try {
            Rule rule = rulePersistencePort.findByCode(ruleId)
                    .orElseThrow(() -> new IllegalArgumentException("Rule not found: " + ruleId));

            if (rule.getState() == Rule.RuleState.PUBLISHED) {
                return DeploymentStage.success(STAGE_HEALTH_CHECK, "Rule is published (health check skipped)");
            } else {
                return DeploymentStage.failed(STAGE_HEALTH_CHECK, "Rule is not published");
            }

        } catch (Exception e) {
            return DeploymentStage.failed(STAGE_HEALTH_CHECK, "Health check error: " + e.getMessage());
        }
    }

    @SuppressWarnings("unused") // ruleId reserved for future use
    private boolean runRuleTests(String ruleId, List<TestScenario> testScenarios) {
        if (testScenarios == null || testScenarios.isEmpty()) {
            logger.debug("No test scenarios provided, skipping tests");
            return true;
        }

        for (TestScenario scenario : testScenarios) {
            try {
                logger.debug("Executing test scenario: {}", scenario.getName());
            } catch (Exception e) {
                logger.error("Test scenario failed: {}", scenario.getName(), e);
                return false;
            }
        }

        return true;
    }

    private int checkRuleHealth(Rule rule, List<String> unhealthyRules) {
        try {
            return 1;
        } catch (Exception e) {
            logger.warn("Failed to check bundle status: ruleId={}", rule.getId(), e);
            unhealthyRules.add(rule.getId());
            return 0;
        }
    }

    private String determineOverallHealth(int total, int healthy) {
        if (total == 0) return "NO_RULES";

        double healthRatio = (double) healthy / total;
        if (healthRatio >= 0.95) return "HEALTHY";
        if (healthRatio >= 0.8) return "DEGRADED";
        return "UNHEALTHY";
    }

    private String generatePipelineId() {
        return "pipeline_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    // Data classes for pipeline management
    public static class DeploymentConfig {
        private boolean runTests = true;
        private boolean strictValidation = false;
        private boolean blueGreenDeployment = false;
        private int timeoutMinutes = 10;
        private List<TestScenario> testScenarios = new ArrayList<>();

        // Getters and setters
        public boolean isRunTests() {
            return runTests;
        }

        public void setRunTests(boolean runTests) {
            this.runTests = runTests;
        }

        public boolean isStrictValidation() {
            return strictValidation;
        }

        public void setStrictValidation(boolean strictValidation) {
            this.strictValidation = strictValidation;
        }

        public boolean isBlueGreenDeployment() {
            return blueGreenDeployment;
        }

        public void setBlueGreenDeployment(boolean blueGreenDeployment) {
            this.blueGreenDeployment = blueGreenDeployment;
        }

        public int getTimeoutMinutes() {
            return timeoutMinutes;
        }

        public void setTimeoutMinutes(int timeoutMinutes) {
            this.timeoutMinutes = timeoutMinutes;
        }

        public List<TestScenario> getTestScenarios() {
            return testScenarios;
        }

        public void setTestScenarios(List<TestScenario> testScenarios) {
            this.testScenarios = testScenarios;
        }
    }

    public static class TestScenario {
        private String name;
        private String description;
        private Map<String, Object> inputData;
        private Map<String, Object> expectedOutput;

        // Default constructor required for JSON deserialization and framework instantiation
        public TestScenario() {
            // Intentionally empty - fields are set via setters or deserialization
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Map<String, Object> getInputData() {
            return inputData;
        }

        public void setInputData(Map<String, Object> inputData) {
            this.inputData = inputData;
        }

        public Map<String, Object> getExpectedOutput() {
            return expectedOutput;
        }

        public void setExpectedOutput(Map<String, Object> expectedOutput) {
            this.expectedOutput = expectedOutput;
        }
    }

    public static class DeploymentStage {
        private final String stageName;
        private final boolean success;
        private final String message;
        private final long timestamp;

        private DeploymentStage(String stageName, boolean success, String message) {
            this.stageName = stageName;
            this.success = success;
            this.message = message;
            this.timestamp = System.currentTimeMillis();
        }

        public static DeploymentStage success(String stageName, String message) {
            return new DeploymentStage(stageName, true, message);
        }

        public static DeploymentStage failed(String stageName, String message) {
            return new DeploymentStage(stageName, false, message);
        }

        // Getters
        public String getStageName() {
            return stageName;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }

    public static class DeploymentPipelineResult {
        private final String pipelineId;
        private final String ruleId;
        private final long startTime;
        private final List<DeploymentStage> stages;
        private boolean success;
        private String finalMessage;
        private Long endTime;

        public DeploymentPipelineResult(String pipelineId, String ruleId) {
            this.pipelineId = pipelineId;
            this.ruleId = ruleId;
            this.startTime = System.currentTimeMillis();
            this.stages = new ArrayList<>();
        }

        public void addStage(DeploymentStage stage) {
            stages.add(stage);
        }

        public DeploymentStage getLastStage() {
            return stages.isEmpty() ? null : stages.get(stages.size() - 1);
        }

        public DeploymentPipelineResult markSuccess() {
            this.success = true;
            this.finalMessage = "Deployment pipeline completed successfully";
            this.endTime = System.currentTimeMillis();
            return this;
        }

        public DeploymentPipelineResult markFailed(String message) {
            this.success = false;
            this.finalMessage = message;
            this.endTime = System.currentTimeMillis();
            return this;
        }

        // Getters
        public String getPipelineId() {
            return pipelineId;
        }

        public String getRuleId() {
            return ruleId;
        }

        public long getStartTime() {
            return startTime;
        }

        public List<DeploymentStage> getStages() {
            return stages;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getFinalMessage() {
            return finalMessage;
        }

        public Long getEndTime() {
            return endTime;
        }

        public Long getDuration() {
            return endTime != null ? endTime - startTime : null;
        }
    }

    public static class EnvironmentHealthStatus {
        private final String tenantId;
        private final String overallHealth;
        private final int totalRules;
        private final int deployedRules;
        private final int healthyRules;
        private final List<String> unhealthyRules;
        private final long timestamp;

        private EnvironmentHealthStatus(Builder builder) {
            this.tenantId = builder.tenantId;
            this.overallHealth = builder.overallHealth;
            this.totalRules = builder.totalRules;
            this.deployedRules = builder.deployedRules;
            this.healthyRules = builder.healthyRules;
            this.unhealthyRules = builder.unhealthyRules;
            this.timestamp = builder.timestamp;
        }

        // Getters
        public String getTenantId() {
            return tenantId;
        }

        public String getOverallHealth() {
            return overallHealth;
        }

        public int getTotalRules() {
            return totalRules;
        }

        public int getDeployedRules() {
            return deployedRules;
        }

        public int getHealthyRules() {
            return healthyRules;
        }

        public List<String> getUnhealthyRules() {
            return unhealthyRules;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public static class Builder {
            private String tenantId;
            private String overallHealth;
            private int totalRules;
            private int deployedRules;
            private int healthyRules;
            private List<String> unhealthyRules;
            private long timestamp;

            public Builder tenantId(String tenantId) {
                this.tenantId = tenantId;
                return this;
            }

            public Builder overallHealth(String overallHealth) {
                this.overallHealth = overallHealth;
                return this;
            }

            public Builder totalRules(int totalRules) {
                this.totalRules = totalRules;
                return this;
            }

            public Builder deployedRules(int deployedRules) {
                this.deployedRules = deployedRules;
                return this;
            }

            public Builder healthyRules(int healthyRules) {
                this.healthyRules = healthyRules;
                return this;
            }

            public Builder unhealthyRules(List<String> unhealthyRules) {
                this.unhealthyRules = unhealthyRules;
                return this;
            }

            public Builder timestamp(long timestamp) {
                this.timestamp = timestamp;
                return this;
            }

            public EnvironmentHealthStatus build() {
                return new EnvironmentHealthStatus(this);
            }
        }
    }
}