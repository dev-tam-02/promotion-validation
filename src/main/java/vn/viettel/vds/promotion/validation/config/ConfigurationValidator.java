package vn.viettel.vds.promotion.validation.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Validates configuration on application startup.
 * <p>
 * Ensures that configuration values are sensible and compatible with each other.
 * Throws IllegalStateException if configuration is invalid, preventing application startup.
 */
@Component
@ConditionalOnProperty(name = "validation.module.enabled", havingValue = "true", matchIfMissing = true)
public class ConfigurationValidator {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationValidator.class);

    private final ValidationModuleProperties properties;

    public ConfigurationValidator(ValidationModuleProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void validate() {
        logger.info("Validating ValidationModule configuration...");

        validateDatabaseConfig();
        validateOutboxConfig();
        validateCacheConfig();
        validateIntegrationConfig();
        validateRuleConfig();

        logger.info("ValidationModule configuration is valid");
    }

    private void validateDatabaseConfig() {
        ValidationModuleProperties.DatabaseConfig db = properties.getDatabase();

        if (!db.isEnabled()) {
            logger.warn("Database is disabled - validation module will not persist data");
            return;
        }

        String strategy = db.getStrategy();
        if (!"JPA".equalsIgnoreCase(strategy) && !"MONGODB".equalsIgnoreCase(strategy)) {
            throw new IllegalStateException(
                    "Invalid database strategy: " + strategy + ". Must be JPA or MONGODB"
            );
        }

        logger.info("Database configuration valid - Strategy: {}", strategy);
    }

    private void validateOutboxConfig() {
        ValidationModuleProperties.OutboxConfig outbox = properties.getOutbox();

        if (!outbox.isEnabled()) {
            logger.info("Outbox pattern is disabled");
            return;
        }

        if (outbox.getChunkSize() <= 0) {
            throw new IllegalStateException(
                    "Outbox chunk size must be positive, got: " + outbox.getChunkSize()
            );
        }

        if (outbox.getMaxRetryAttempts() < 1 || outbox.getMaxRetryAttempts() > 10) {
            throw new IllegalStateException(
                    "Outbox max retry attempts must be between 1 and 10, got: " + outbox.getMaxRetryAttempts()
            );
        }

        Duration interval = outbox.getInterval();
        if (interval == null || interval.isNegative() || interval.isZero()) {
            throw new IllegalStateException(
                    "Outbox interval must be positive duration, got: " + interval
            );
        }

        Duration retention = outbox.getRetentionPeriod();
        if (retention == null || retention.isNegative()) {
            throw new IllegalStateException(
                    "Outbox retention period must be non-negative duration, got: " + retention
            );
        }

        if (retention.compareTo(Duration.ofDays(30)) > 0) {
            logger.warn("Outbox retention period is longer than 30 days: {}. This may cause storage issues.", retention);
        }

        logger.info("Outbox configuration valid - Chunk size: {}, Interval: {}, Retention: {}",
                outbox.getChunkSize(), interval, retention);
    }

    private void validateCacheConfig() {
        ValidationModuleProperties.CacheConfig cache = properties.getCache();

        if (!cache.isEnabled()) {
            logger.info("Cache is disabled");
            return;
        }

        if (cache.getPrefix() == null || cache.getPrefix().trim().isEmpty()) {
            throw new IllegalStateException("Cache prefix cannot be empty");
        }

        Duration defaultTtl = cache.getDefaultTtl();
        if (defaultTtl == null || defaultTtl.isNegative() || defaultTtl.isZero()) {
            throw new IllegalStateException(
                    "Cache default TTL must be positive duration, got: " + defaultTtl
            );
        }

        Duration ruleTtl = cache.getRuleTtl();
        if (ruleTtl == null || ruleTtl.isNegative() || ruleTtl.isZero()) {
            throw new IllegalStateException(
                    "Cache rule TTL must be positive duration, got: " + ruleTtl
            );
        }

        logger.info("Cache configuration valid - Prefix: {}, Default TTL: {}, Rule TTL: {}",
                cache.getPrefix(), defaultTtl, ruleTtl);
    }

    private void validateIntegrationConfig() {
        ValidationModuleProperties.IntegrationConfig integration = properties.getIntegration();

        // Validate validation engine config
        ValidationModuleProperties.ValidationEngineConfig engine = integration.getValidationEngine();
        if (engine.isEnabled()) {
            validateServiceEndpoint("ValidationEngine", engine.getBaseUrl());
            validateTimeout("ValidationEngine connect", engine.getConnectTimeout());
            validateTimeout("ValidationEngine read", engine.getReadTimeout());

            logger.info("ValidationEngine integration config valid - URL: {}", engine.getBaseUrl());
        } else {
            logger.info("ValidationEngine integration is disabled");
        }

        // Validate segment service config
        ValidationModuleProperties.ServiceConfig segment = integration.getSegmentService();
        if (segment.isEnabled()) {
            validateServiceEndpoint("SegmentService", segment.getBaseUrl());
            validateTimeout("SegmentService connect", segment.getConnectTimeout());
            validateTimeout("SegmentService read", segment.getReadTimeout());

            logger.info("SegmentService integration config valid - URL: {}", segment.getBaseUrl());
        } else {
            logger.info("SegmentService integration is disabled");
        }
    }

    private void validateRuleConfig() {
        ValidationModuleProperties.RuleConfig rule = properties.getRule();

        if (rule.getMaxDepth() < 1 || rule.getMaxDepth() > 20) {
            throw new IllegalStateException(
                    "Rule max depth must be between 1 and 20, got: " + rule.getMaxDepth()
            );
        }

        if (rule.getMaxNodes() < 1 || rule.getMaxNodes() > 1000) {
            throw new IllegalStateException(
                    "Rule max nodes must be between 1 and 1000, got: " + rule.getMaxNodes()
            );
        }

        Duration evalTimeout = rule.getEvaluationTimeout();
        if (evalTimeout == null || evalTimeout.isNegative() || evalTimeout.isZero()) {
            throw new IllegalStateException(
                    "Rule evaluation timeout must be positive duration, got: " + evalTimeout
            );
        }

        if (evalTimeout.compareTo(Duration.ofSeconds(30)) > 0) {
            logger.warn("Rule evaluation timeout is longer than 30 seconds: {}. This may cause performance issues.", evalTimeout);
        }

        logger.info("Rule configuration valid - Max depth: {}, Max nodes: {}, Eval timeout: {}",
                rule.getMaxDepth(), rule.getMaxNodes(), evalTimeout);
    }

    private void validateServiceEndpoint(String serviceName, String baseUrl) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            throw new IllegalStateException(
                    serviceName + " base URL cannot be empty"
            );
        }

        if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
            throw new IllegalStateException(
                    serviceName + " base URL must start with http:// or https://, got: " + baseUrl
            );
        }
    }

    private void validateTimeout(String name, Duration timeout) {
        if (timeout == null || timeout.isNegative() || timeout.isZero()) {
            throw new IllegalStateException(
                    name + " timeout must be positive duration, got: " + timeout
            );
        }

        if (timeout.compareTo(Duration.ofMinutes(5)) > 0) {
            logger.warn("{} timeout is longer than 5 minutes: {}. This may cause performance issues.", name, timeout);
        }
    }
}
