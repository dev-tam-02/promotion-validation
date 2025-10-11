package vn.viettel.vds.promotion.validation.application.fact.resolver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.domain.fact.FactRequest;
import vn.viettel.vds.promotion.validation.domain.fact.ProvenanceInfo;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
@Component
public class FactResolverOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(FactResolverOrchestrator.class);

    private final List<FactResolver<?>> resolvers;

    public FactResolverOrchestrator(List<FactResolver<?>> resolvers) {
        this.resolvers = resolvers.stream()
                .filter(FactResolver::isEnabled)
                .sorted((a, b) -> Integer.compare(a.getPriority(), b.getPriority()))
                .toList();

        log.info("Initialized FactResolverOrchestrator with {} enabled resolvers: {}",
                this.resolvers.size(),
                this.resolvers.stream().map(FactResolver::getContextName).toList());
    }

    public CompletableFuture<Map<String, Object>> resolveAll(FactRequest request, ProvenanceInfo.FetchPolicy fetchPolicy) {
        Map<String, Object> results = new ConcurrentHashMap<>();
        Map<String, ProvenanceInfo.SourceInfo> sources = new ConcurrentHashMap<>();

        // Create all resolution tasks using virtual threads
        List<CompletableFuture<Void>> resolutionTasks = resolvers.stream()
                .map(resolver -> resolveWithTimeout(resolver, request, fetchPolicy, results, sources))
                .toList();

        // Wait for all tasks to complete (or timeout)
        CompletableFuture<Void> allTasks = CompletableFuture.allOf(
                resolutionTasks.toArray(new CompletableFuture[0])
        );

        return allTasks
                .orTimeout(10, TimeUnit.SECONDS) // Global timeout for all resolvers
                .thenApply(ignored -> {
                    // Add sources to results
                    results.put("_sources", sources.values().stream().toList());
                    return results;
                })
                .exceptionally(throwable -> {
                    log.warn("Some resolvers timed out or failed, proceeding with partial results: {}",
                            throwable.getMessage());
                    results.put("_sources", sources.values().stream().toList());
                    return results;
                });
    }

    private CompletableFuture<Void> resolveWithTimeout(
            FactResolver<?> resolver,
            FactRequest request,
            ProvenanceInfo.FetchPolicy fetchPolicy,
            Map<String, Object> results,
            Map<String, ProvenanceInfo.SourceInfo> sources
    ) {
        Instant startTime = Instant.now();

        return resolver.resolve(request, fetchPolicy)
                .orTimeout(resolver.getTimeoutMs(), TimeUnit.MILLISECONDS)
                .thenAccept(result -> {
                    if (result != null) {
                        results.put(resolver.getContextName(), result);

                        // Record source information
                        long responseTime = Duration.between(startTime, Instant.now()).toMillis();
                        ProvenanceInfo.SourceInfo sourceInfo = new ProvenanceInfo.SourceInfo(
                                resolver.getContextName(),
                                "RESOLVER",
                                Instant.now(),
                                responseTime,
                                "SUCCESS",
                                "1.0",
                                false, // Not cached at resolver level
                                null
                        );
                        sources.put(resolver.getContextName(), sourceInfo);

                        log.debug("Resolved {} successfully in {}ms", resolver.getContextName(), responseTime);
                    }
                })
                .exceptionally(throwable -> {
                    long responseTime = Duration.between(startTime, Instant.now()).toMillis();
                    log.warn("Failed to resolve {} after {}ms: {}",
                            resolver.getContextName(), responseTime, throwable.getMessage());

                    // Record failed source information
                    ProvenanceInfo.SourceInfo sourceInfo = new ProvenanceInfo.SourceInfo(
                            resolver.getContextName(),
                            "RESOLVER",
                            Instant.now(),
                            responseTime,
                            "FAILED",
                            "1.0",
                            false,
                            null
                    );
                    sources.put(resolver.getContextName(), sourceInfo);

                    return null; // Continue with other resolvers
                });
    }
}