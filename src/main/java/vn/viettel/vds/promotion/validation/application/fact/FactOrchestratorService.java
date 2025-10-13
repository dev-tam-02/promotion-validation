package vn.viettel.vds.promotion.validation.application.fact;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import vn.viettel.vds.promotion.validation.application.fact.cache.FactCacheService;
import vn.viettel.vds.promotion.validation.application.fact.mapper.FactMapper;
import vn.viettel.vds.promotion.validation.application.fact.policy.PolicyEngine;
import vn.viettel.vds.promotion.validation.application.fact.resolver.FactResolverOrchestrator;
import vn.viettel.vds.promotion.validation.domain.fact.FactOrchestrator;
import vn.viettel.vds.promotion.validation.domain.fact.FactPack;
import vn.viettel.vds.promotion.validation.domain.fact.FactRequest;
import vn.viettel.vds.promotion.validation.domain.fact.ProvenanceInfo;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class FactOrchestratorService implements FactOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(FactOrchestratorService.class);

    private static final String CACHE_KEY_PREFIX = "facts:v1:";
    private static final String CACHE_KEY_FALLBACK_PREFIX = "facts:v1:fallback:";
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final int HASH_TRUNCATE_LENGTH = 16;
    private static final int CACHE_TTL_SECONDS = 60;
    private static final String FACT_PACK_VERSION = "1.0";
    private static final String SERVICE_VERSION = "1.0.0";

    private final FactResolverOrchestrator resolverOrchestrator;
    private final FactMapper factMapper;
    private final FactCacheService cacheService;
    private final PolicyEngine policyEngine;
    private final ObjectMapper objectMapper;

    public FactOrchestratorService(
            FactResolverOrchestrator resolverOrchestrator,
            FactMapper factMapper,
            FactCacheService cacheService,
            PolicyEngine policyEngine,
            ObjectMapper objectMapper
    ) {
        this.resolverOrchestrator = resolverOrchestrator;
        this.factMapper = factMapper;
        this.cacheService = cacheService;
        this.policyEngine = policyEngine;
        this.objectMapper = objectMapper;
    }

    @Override
    public CompletableFuture<FactPack> resolve(FactRequest request) {
        return doResolve(request);
    }

    @Override
    public CompletableFuture<FactPack> resolveWithCache(FactRequest request) {
        String cacheKey = generateCacheKey(request);

        return cacheService.get(cacheKey)
                .thenCompose(cachedResult -> {
                    if (cachedResult != null) {
                        log.debug("Cache hit for key: {}", cacheKey);
                        return CompletableFuture.completedFuture((FactPack) cachedResult);
                    } else {
                        log.debug("Cache miss for key: {}, resolving...", cacheKey);
                        return doResolve(request)
                                .thenCompose(factPack ->
                                        cacheService.put(cacheKey, factPack, getCacheTtlSeconds())
                                                .thenApply(ignored -> factPack)
                                );
                    }
                });
    }

    @Override
    public void evictCache(String cacheKey) {
        cacheService.evict(cacheKey)
                .thenRun(() -> log.debug("Evicted cache key: {}", cacheKey))
                .exceptionally(throwable -> {
                    log.warn("Failed to evict cache key {}: {}", cacheKey, throwable.getMessage());
                    return null;
                });
    }

    @Override
    public String generateCacheKey(FactRequest request) {
        try {
            String requestJson = objectMapper.writeValueAsString(request);
            return CACHE_KEY_PREFIX + hashString(requestJson);
        } catch (JsonProcessingException e) {
            log.warn("Failed to generate cache key for request", e);
            return CACHE_KEY_FALLBACK_PREFIX + UUID.randomUUID();
        }
    }

    private CompletableFuture<FactPack> doResolve(FactRequest request) {
        long startTime = System.currentTimeMillis();
        String aggregationId = UUID.randomUUID().toString();

        log.info("Starting fact aggregation for request: customerId={}, orderId={}, aggregationId={}", request.customerId(), request.orderId(), aggregationId);

        return policyEngine.determineFetchPolicy(request)
                .thenCompose(policy -> resolverOrchestrator.resolveAll(request, policy))
                .thenApply(rawFacts -> {
                    FactPack factPack = factMapper.mapToFactPack(rawFacts, request);
                    return enrichWithProvenance(factPack, startTime, aggregationId);
                })
                .whenComplete((result, throwable) -> {
                    long processingTime = System.currentTimeMillis() - startTime;
                    if (throwable == null) {
                        log.info("Fact aggregation completed: aggregationId={}, processingTime={}ms", aggregationId, processingTime);
                    } else {
                        log.error("Fact aggregation failed: aggregationId={}, processingTime={}ms, error={}", aggregationId, processingTime, throwable != null ? throwable.getMessage() : "unknown error", throwable);
                    }
                });
    }

    private FactPack enrichWithProvenance(FactPack factPack, long startTime, String aggregationId) {
        long processingTime = System.currentTimeMillis() - startTime;

        ProvenanceInfo provenance = ProvenanceInfo.builder()
                .aggregatedAt(Instant.now())
                .aggregationId(aggregationId)
                .processingTimeMs(processingTime)
                .fetchPolicy(Optional.ofNullable(factPack.provenance()).map(ProvenanceInfo::fetchPolicy).orElse(null))
                .sources(Optional.ofNullable(factPack.provenance()).map(ProvenanceInfo::sources).orElse(null))
                .versions(Map.of("factPack", FACT_PACK_VERSION, "service", SERVICE_VERSION))
                .build();

        return FactPack.builder()
                .factPackVersion(factPack.factPackVersion())
                .timestamp(factPack.timestamp())
                .customer(factPack.customer())
                .order(factPack.order())
                .candidate(factPack.candidate())
                .segments(factPack.segments())
                .limits(factPack.limits())
                .metadata(factPack.metadata())
                .geo(factPack.geo())
                .derived(factPack.derived())
                .provenance(provenance)
                .build();
    }

    private int getCacheTtlSeconds() {
        return CACHE_TTL_SECONDS; // 60 seconds default TTL
    }

    private String hashString(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hashBytes = md.digest(input.getBytes());
            StringBuilder hexString = new StringBuilder();

            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString().substring(0, HASH_TRUNCATE_LENGTH); // Use first 16 characters
        } catch (NoSuchAlgorithmException e) {
            log.warn("SHA-256 not available, using fallback hash", e);
            return String.valueOf(input.hashCode());
        }
    }
}