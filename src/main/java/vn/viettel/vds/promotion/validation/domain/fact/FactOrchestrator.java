package vn.viettel.vds.promotion.validation.domain.fact;

import java.util.concurrent.CompletableFuture;

public interface FactOrchestrator {

    CompletableFuture<FactPack> resolve(FactRequest request);

    CompletableFuture<FactPack> resolveWithCache(FactRequest request);

    void evictCache(String cacheKey);

    String generateCacheKey(FactRequest request);
}