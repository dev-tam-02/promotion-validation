package vn.viettel.vds.promotion.validation.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Configuration properties for the Validation module.
 *
 * Binds configuration from application.yml with validation.
 *
 * Usage in application.yml:
 * <pre>
 * validation:
 *   module:
 *     enabled: true
 *     database:
 *       strategy: JPA
 *     cache:
 *       enabled: true
 *       ttl: 30m
 * </pre>
 */
@ConfigurationProperties(prefix = "validation.module")
@Validated
public class ValidationModuleProperties {

    /**
     * Enable/disable the validation module
     */
    private boolean enabled = true;

    /**
     * Database configuration
     */
    @Valid
    @NotNull
    private DatabaseConfig database = new DatabaseConfig();

    /**
     * Outbox pattern configuration
     */
    @Valid
    @NotNull
    private OutboxConfig outbox = new OutboxConfig();

    /**
     * Cache configuration
     */
    @Valid
    @NotNull
    private CacheConfig cache = new CacheConfig();

    /**
     * Integration configuration
     */
    @Valid
    @NotNull
    private IntegrationConfig integration = new IntegrationConfig();

    /**
     * Rule processing configuration
     */
    @Valid
    @NotNull
    private RuleConfig rule = new RuleConfig();

    // Getters and Setters

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public DatabaseConfig getDatabase() {
        return database;
    }

    public void setDatabase(DatabaseConfig database) {
        this.database = database;
    }

    public OutboxConfig getOutbox() {
        return outbox;
    }

    public void setOutbox(OutboxConfig outbox) {
        this.outbox = outbox;
    }

    public CacheConfig getCache() {
        return cache;
    }

    public void setCache(CacheConfig cache) {
        this.cache = cache;
    }

    public IntegrationConfig getIntegration() {
        return integration;
    }

    public void setIntegration(IntegrationConfig integration) {
        this.integration = integration;
    }

    public RuleConfig getRule() {
        return rule;
    }

    public void setRule(RuleConfig rule) {
        this.rule = rule;
    }

    /**
     * Database configuration
     */
    public static class DatabaseConfig {
        /**
         * Database strategy: JPA or MONGODB
         */
        @NotBlank
        private String strategy = "JPA";

        /**
         * Enable database operations
         */
        private boolean enabled = true;

        public String getStrategy() {
            return strategy;
        }

        public void setStrategy(String strategy) {
            this.strategy = strategy;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    /**
     * Outbox pattern configuration
     */
    public static class OutboxConfig {
        /**
         * Enable outbox pattern
         */
        private boolean enabled = true;

        /**
         * Batch processing chunk size
         */
        @Positive
        private int chunkSize = 100;

        /**
         * Processing interval
         */
        @NotNull
        private Duration interval = Duration.ofMinutes(1);

        /**
         * Retention period for processed events
         */
        @NotNull
        private Duration retentionPeriod = Duration.ofDays(7);

        /**
         * Maximum retry attempts
         */
        @Min(1)
        @Max(10)
        private int maxRetryAttempts = 3;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getChunkSize() {
            return chunkSize;
        }

        public void setChunkSize(int chunkSize) {
            this.chunkSize = chunkSize;
        }

        public Duration getInterval() {
            return interval;
        }

        public void setInterval(Duration interval) {
            this.interval = interval;
        }

        public Duration getRetentionPeriod() {
            return retentionPeriod;
        }

        public void setRetentionPeriod(Duration retentionPeriod) {
            this.retentionPeriod = retentionPeriod;
        }

        public int getMaxRetryAttempts() {
            return maxRetryAttempts;
        }

        public void setMaxRetryAttempts(int maxRetryAttempts) {
            this.maxRetryAttempts = maxRetryAttempts;
        }
    }

    /**
     * Cache configuration
     */
    public static class CacheConfig {
        /**
         * Enable caching
         */
        private boolean enabled = true;

        /**
         * Cache prefix
         */
        @NotBlank
        private String prefix = "validation:";

        /**
         * Default TTL for cache entries
         */
        @NotNull
        private Duration defaultTtl = Duration.ofMinutes(30);

        /**
         * Rule cache TTL
         */
        @NotNull
        private Duration ruleTtl = Duration.ofHours(1);

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getPrefix() {
            return prefix;
        }

        public void setPrefix(String prefix) {
            this.prefix = prefix;
        }

        public Duration getDefaultTtl() {
            return defaultTtl;
        }

        public void setDefaultTtl(Duration defaultTtl) {
            this.defaultTtl = defaultTtl;
        }

        public Duration getRuleTtl() {
            return ruleTtl;
        }

        public void setRuleTtl(Duration ruleTtl) {
            this.ruleTtl = ruleTtl;
        }
    }

    /**
     * Integration configuration for external services
     */
    public static class IntegrationConfig {
        /**
         * Validation engine deployment configuration
         */
        @Valid
        @NotNull
        private ValidationEngineConfig validationEngine = new ValidationEngineConfig();

        /**
         * Segment service configuration
         */
        @Valid
        @NotNull
        private ServiceConfig segmentService = new ServiceConfig();

        public ValidationEngineConfig getValidationEngine() {
            return validationEngine;
        }

        public void setValidationEngine(ValidationEngineConfig validationEngine) {
            this.validationEngine = validationEngine;
        }

        public ServiceConfig getSegmentService() {
            return segmentService;
        }

        public void setSegmentService(ServiceConfig segmentService) {
            this.segmentService = segmentService;
        }
    }

    /**
     * Validation engine specific configuration
     */
    public static class ValidationEngineConfig {
        /**
         * Enable deployment to validation engine
         */
        private boolean enabled = true;

        /**
         * Validation engine base URL
         */
        @NotBlank
        private String baseUrl = "http://validation-engine:16015";

        /**
         * Connection timeout
         */
        @NotNull
        private Duration connectTimeout = Duration.ofSeconds(10);

        /**
         * Read timeout
         */
        @NotNull
        private Duration readTimeout = Duration.ofSeconds(30);

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public Duration getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        public Duration getReadTimeout() {
            return readTimeout;
        }

        public void setReadTimeout(Duration readTimeout) {
            this.readTimeout = readTimeout;
        }
    }

    /**
     * Generic service configuration
     */
    public static class ServiceConfig {
        /**
         * Enable this service integration
         */
        private boolean enabled = true;

        /**
         * Service base URL
         */
        @NotBlank
        private String baseUrl = "http://localhost:8080";

        /**
         * Connection timeout
         */
        @NotNull
        private Duration connectTimeout = Duration.ofSeconds(5);

        /**
         * Read timeout
         */
        @NotNull
        private Duration readTimeout = Duration.ofSeconds(30);

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public Duration getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        public Duration getReadTimeout() {
            return readTimeout;
        }

        public void setReadTimeout(Duration readTimeout) {
            this.readTimeout = readTimeout;
        }
    }

    /**
     * Rule processing configuration
     */
    public static class RuleConfig {
        /**
         * Maximum rule tree depth
         */
        @Min(1)
        @Max(20)
        private int maxDepth = 10;

        /**
         * Maximum number of nodes in a rule
         */
        @Min(1)
        @Max(1000)
        private int maxNodes = 500;

        /**
         * Rule evaluation timeout
         */
        @NotNull
        private Duration evaluationTimeout = Duration.ofSeconds(5);

        /**
         * Enable rule validation on save
         */
        private boolean validateOnSave = true;

        public int getMaxDepth() {
            return maxDepth;
        }

        public void setMaxDepth(int maxDepth) {
            this.maxDepth = maxDepth;
        }

        public int getMaxNodes() {
            return maxNodes;
        }

        public void setMaxNodes(int maxNodes) {
            this.maxNodes = maxNodes;
        }

        public Duration getEvaluationTimeout() {
            return evaluationTimeout;
        }

        public void setEvaluationTimeout(Duration evaluationTimeout) {
            this.evaluationTimeout = evaluationTimeout;
        }

        public boolean isValidateOnSave() {
            return validateOnSave;
        }

        public void setValidateOnSave(boolean validateOnSave) {
            this.validateOnSave = validateOnSave;
        }
    }
}
