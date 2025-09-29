package vn.viettel.vds.promotion.validation.application.fact.cache;

import java.util.concurrent.CompletableFuture;

public interface FactCacheService {

    CompletableFuture<Object> get(String key);

    CompletableFuture<Void> put(String key, Object value, int ttlSeconds);

    CompletableFuture<Void> evict(String key);

    CompletableFuture<Void> clear();

    CompletableFuture<Boolean> exists(String key);

    String getCacheKeyWithVersion(String baseKey, String version);
}