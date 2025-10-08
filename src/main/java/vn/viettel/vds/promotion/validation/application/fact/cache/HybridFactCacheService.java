package vn.viettel.vds.promotion.validation.application.fact.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.AsyncCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.redisson.api.RBucket;
import org.redisson.api.RKeys;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
public class HybridFactCacheService implements FactCacheService {

    private static final Logger log = LoggerFactory.getLogger(HybridFactCacheService.class);
    private static final String CACHE_VERSION = "v1";

    private final AsyncCache<String, Object> l1Cache; // Caffeine (in-process)
    private final RedissonClient redissonClient; // Redisson (distributed)
    private final ObjectMapper objectMapper;

    public HybridFactCacheService(
            RedissonClient redissonClient,
            ObjectMapper objectMapper
    ) {
        this.redissonClient = redissonClient;
        this.objectMapper = objectMapper;

        // Configure L1 Cache (Caffeine)
        this.l1Cache = Caffeine.newBuilder()
                .maximumSize(1000) // Max 1000 entries in memory
                .expireAfterWrite(Duration.ofMinutes(2)) // 2 minutes expiry
                .buildAsync();

        log.info("Initialized HybridFactCacheService with L1 (Caffeine) + L2 (Redis) caching");
    }

    @Override
    public CompletableFuture<Object> get(String key) {
        String versionedKey = getCacheKeyWithVersion(key, CACHE_VERSION);

        // Try L1 cache first
        return l1Cache.getIfPresent(versionedKey)
                .thenCompose(cachedValue -> {
                    if (cachedValue != null) {
                        log.debug("L1 cache hit for key: {}", versionedKey);
                        return CompletableFuture.completedFuture(cachedValue);
                    } else {
                        // L1 miss, try L2 (Redisson) using virtual threads
                        return CompletableFuture.supplyAsync(() -> {
                            try {
                                RBucket<String> bucket = redissonClient.getBucket(versionedKey);
                                String redisValue = bucket.get();
                                if (redisValue != null) {
                                    Object deserializedValue = deserializeValue(redisValue);
                                    log.debug("L2 cache hit for key: {}, promoting to L1", versionedKey);

                                    // Promote to L1 cache asynchronously
                                    l1Cache.put(versionedKey, CompletableFuture.completedFuture(deserializedValue));

                                    return deserializedValue;
                                } else {
                                    log.debug("Cache miss for key: {}", versionedKey);
                                    return null;
                                }
                            } catch (Exception e) {
                                log.warn("Error reading from Redisson cache for key {}: {}", versionedKey, e.getMessage());
                                return null;
                            }
                        }, Executors.newVirtualThreadPerTaskExecutor());
                    }
                });
    }

    @Override
    public CompletableFuture<Void> put(String key, Object value, int ttlSeconds) {
        String versionedKey = getCacheKeyWithVersion(key, CACHE_VERSION);

        // Store in L1 cache
        l1Cache.put(versionedKey, CompletableFuture.completedFuture(value));

        // Store in L2 cache (Redisson) with TTL using virtual threads
        return CompletableFuture.runAsync(() -> {
            try {
                String serializedValue = serializeValue(value);
                RBucket<String> bucket = redissonClient.getBucket(versionedKey);
                bucket.set(serializedValue, ttlSeconds, TimeUnit.SECONDS);
                log.debug("Stored in cache with TTL {}s: {}", ttlSeconds, versionedKey);
            } catch (Exception e) {
                log.warn("Failed to store in Redisson cache for key {}: {}", versionedKey, e.getMessage());
            }
        }, Executors.newVirtualThreadPerTaskExecutor());
    }

    @Override
    public CompletableFuture<Void> evict(String key) {
        String versionedKey = getCacheKeyWithVersion(key, CACHE_VERSION);

        // Remove from L1 cache
        l1Cache.synchronous().invalidate(versionedKey);

        // Remove from L2 cache using virtual threads
        return CompletableFuture.runAsync(() -> {
            try {
                RBucket<String> bucket = redissonClient.getBucket(versionedKey);
                bucket.delete();
                log.debug("Evicted from cache: {}", versionedKey);
            } catch (Exception e) {
                log.warn("Failed to evict from Redisson cache for key {}: {}", versionedKey, e.getMessage());
            }
        }, Executors.newVirtualThreadPerTaskExecutor());
    }

    @Override
    public CompletableFuture<Void> clear() {
        // Clear L1 cache
        l1Cache.synchronous().invalidateAll();

        // Clear L2 cache (Redisson) - only our versioned keys using virtual threads
        return CompletableFuture.runAsync(() -> {
            try {
                RKeys keys = redissonClient.getKeys();
                Iterable<String> matchingKeys = keys.getKeysByPattern(CACHE_VERSION + ":*");
                for (String key : matchingKeys) {
                    redissonClient.getBucket(key).delete();
                }
                log.info("Cleared all cache entries");
            } catch (Exception e) {
                log.warn("Failed to clear Redisson cache: {}", e.getMessage());
            }
        }, Executors.newVirtualThreadPerTaskExecutor());
    }

    @Override
    public CompletableFuture<Boolean> exists(String key) {
        String versionedKey = getCacheKeyWithVersion(key, CACHE_VERSION);

        // Check L1 first
        return l1Cache.getIfPresent(versionedKey)
                .thenCompose(cachedValue -> {
                    if (cachedValue != null) {
                        return CompletableFuture.completedFuture(true);
                    } else {
                        // Check L2 (Redisson) using virtual threads
                        return CompletableFuture.supplyAsync(() -> {
                            try {
                                RBucket<String> bucket = redissonClient.getBucket(versionedKey);
                                return bucket.isExists();
                            } catch (Exception e) {
                                log.warn("Failed to check existence in Redisson for key {}: {}", versionedKey, e.getMessage());
                                return false;
                            }
                        }, Executors.newVirtualThreadPerTaskExecutor());
                    }
                });
    }

    @Override
    public String getCacheKeyWithVersion(String baseKey, String version) {
        return version + ":" + baseKey;
    }

    private String serializeValue(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize cache value", e);
        }
    }

    private Object deserializeValue(String json) {
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize cache value", e);
        }
    }
}