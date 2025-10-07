package vn.viettel.vds.promotion.validation.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * Redis-based implementation of IdempotencyService.
 * Stores processed command IDs with their results to prevent duplicate processing.
 *
 * <p>Key pattern: {@code validation:idempotency:{commandId}}
 * <p>TTL: 24 hours (86400 seconds) by default
 *
 * @see IdempotencyService
 */
@Service
public class RedisIdempotencyService implements IdempotencyService {

    private static final Logger logger = LoggerFactory.getLogger(RedisIdempotencyService.class);

    private static final String KEY_PREFIX = "validation:idempotency:";

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration ttl;

    public RedisIdempotencyService(
            RedisTemplate<String, String> redisTemplate,
            ObjectMapper objectMapper,
            @Value("${promix.idempotency.ttl-seconds:86400}") long ttlSeconds) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.ttl = Duration.ofSeconds(ttlSeconds);

        logger.info("Initialized RedisIdempotencyService with TTL: {} seconds", ttlSeconds);
    }

    @Override
    public boolean isProcessed(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isEmpty()) {
            logger.warn("Idempotency key is null or empty");
            return false;
        }

        try {
            String redisKey = buildRedisKey(idempotencyKey);
            Boolean exists = redisTemplate.hasKey(redisKey);

            boolean processed = Boolean.TRUE.equals(exists);
            logger.debug("Idempotency check: key={}, processed={}", idempotencyKey, processed);

            return processed;

        } catch (Exception e) {
            logger.error("Error checking idempotency key: {}", idempotencyKey, e);
            // In case of Redis failure, allow processing (fail-open strategy)
            return false;
        }
    }

    @Override
    public void markAsProcessed(String idempotencyKey, Object result) {
        if (idempotencyKey == null || idempotencyKey.isEmpty()) {
            logger.warn("Cannot mark null or empty idempotency key as processed");
            return;
        }

        try {
            String redisKey = buildRedisKey(idempotencyKey);
            String resultJson = serializeResult(result);

            redisTemplate.opsForValue().set(redisKey, resultJson, ttl);

            logger.info("Marked command as processed: key={}, ttl={}", idempotencyKey, ttl);
            logger.debug("Stored result: key={}, result={}", idempotencyKey, resultJson);

        } catch (Exception e) {
            logger.error("Error marking idempotency key as processed: {}", idempotencyKey, e);
            // Don't fail the operation if Redis fails
        }
    }

    @Override
    public Optional<Object> getProcessedResult(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isEmpty()) {
            logger.warn("Cannot get result for null or empty idempotency key");
            return Optional.empty();
        }

        try {
            String redisKey = buildRedisKey(idempotencyKey);
            String resultJson = redisTemplate.opsForValue().get(redisKey);

            if (resultJson == null) {
                logger.debug("No cached result found for idempotency key: {}", idempotencyKey);
                return Optional.empty();
            }

            Object result = deserializeResult(resultJson);
            logger.debug("Retrieved cached result: key={}", idempotencyKey);

            return Optional.of(result);

        } catch (Exception e) {
            logger.error("Error retrieving cached result: {}", idempotencyKey, e);
            return Optional.empty();
        }
    }

    @Override
    public void remove(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isEmpty()) {
            logger.warn("Cannot remove null or empty idempotency key");
            return;
        }

        try {
            String redisKey = buildRedisKey(idempotencyKey);
            Boolean deleted = redisTemplate.delete(redisKey);

            if (Boolean.TRUE.equals(deleted)) {
                logger.info("Removed idempotency key: {}", idempotencyKey);
            } else {
                logger.debug("Idempotency key not found for removal: {}", idempotencyKey);
            }

        } catch (Exception e) {
            logger.error("Error removing idempotency key: {}", idempotencyKey, e);
        }
    }

    /**
     * Build Redis key with prefix
     */
    private String buildRedisKey(String idempotencyKey) {
        return KEY_PREFIX + idempotencyKey;
    }

    /**
     * Serialize result object to JSON
     */
    private String serializeResult(Object result) throws JsonProcessingException {
        if (result == null) {
            return "null";
        }

        // Wrap result in a container to preserve type information
        IdempotencyResult container = new IdempotencyResult(
            result.getClass().getName(),
            objectMapper.writeValueAsString(result)
        );

        return objectMapper.writeValueAsString(container);
    }

    /**
     * Deserialize result from JSON
     */
    private Object deserializeResult(String json) throws JsonProcessingException {
        if ("null".equals(json)) {
            return null;
        }

        IdempotencyResult container = objectMapper.readValue(json, IdempotencyResult.class);

        // For now, return the raw JSON payload
        // In a real implementation, you might want to deserialize to the actual type
        return container.payload;
    }

    /**
     * Container for storing idempotency result with type information
     */
    private record IdempotencyResult(String type, String payload) {}
}
