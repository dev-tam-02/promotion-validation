package vn.viettel.vds.promotion.validation.application.fact.resolver;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.viettel.vds.promotion.validation.domain.fact.FactRequest;
import vn.viettel.vds.promotion.validation.domain.fact.ProvenanceInfo;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

public abstract class AbstractFactResolver<T> implements FactResolver<T> {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final Executor virtualThreadExecutor;

    protected AbstractFactResolver(CircuitBreaker circuitBreaker, Retry retry) {
        this.circuitBreaker = circuitBreaker;
        this.retry = retry;
        this.virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
    }

    @Override
    public CompletableFuture<T> resolve(FactRequest request, ProvenanceInfo.FetchPolicy fetchPolicy) {
        return switch (fetchPolicy) {
            case FROM_IDS -> resolveFromIdsWithResilience(request);
            case EMBEDDED_PAYLOAD -> {
                if (supportsEmbeddedPayload() && request.embeddedPayload() != null) {
                    yield resolveFromEmbeddedPayloadWithResilience(request, request.embeddedPayload());
                } else {
                    yield resolveFromIdsWithResilience(request);
                }
            }
            case HYBRID -> {
                if (supportsEmbeddedPayload() && request.embeddedPayload() != null) {
                    yield resolveFromEmbeddedPayloadWithResilience(request, request.embeddedPayload())
                            .handle((result, throwable) -> {
                                if (throwable != null) {
                                    log.warn("Embedded payload resolution failed for {}, falling back to IDs: {}",
                                            getContextName(), throwable.getMessage());
                                    return resolveFromIdsWithResilience(request).join();
                                }
                                return result;
                            });
                } else {
                    yield resolveFromIdsWithResilience(request);
                }
            }
            case PARTIAL -> resolveFromIdsWithResilience(request)
                    .handle((result, throwable) -> {
                        if (throwable != null) {
                            log.warn("Full resolution failed for {}, returning partial result: {}",
                                    getContextName(), throwable.getMessage());
                            return getPartialResult(request);
                        }
                        return result;
                    });
        };
    }

    private CompletableFuture<T> resolveFromIdsWithResilience(FactRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            Supplier<T> supplier = () -> resolveFromIds(request);

            // Apply circuit breaker
            Supplier<T> decoratedSupplier = CircuitBreaker.decorateSupplier(circuitBreaker, supplier);

            // Apply retry
            decoratedSupplier = Retry.decorateSupplier(retry, decoratedSupplier);

            try {
                return decoratedSupplier.get();
            } catch (Exception e) {
                throw new RuntimeException("Failed to resolve " + getContextName() + " from IDs: " + e.getMessage(), e);
            }
        }, virtualThreadExecutor);
    }

    private CompletableFuture<T> resolveFromEmbeddedPayloadWithResilience(FactRequest request, Map<String, Object> embeddedData) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return resolveFromEmbeddedPayload(request, embeddedData);
            } catch (Exception e) {
                throw new RuntimeException("Failed to resolve " + getContextName() + " from embedded payload: " + e.getMessage(), e);
            }
        }, virtualThreadExecutor);
    }

    @Override
    public boolean supportsEmbeddedPayload() {
        return false;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public int getTimeoutMs() {
        return 5000; // 5 seconds default
    }

    @Override
    public int getPriority() {
        return 100; // Default priority
    }

    protected abstract T getPartialResult(FactRequest request);

    @Override
    public T resolveFromEmbeddedPayload(FactRequest request, Map<String, Object> embeddedData) {
        throw new UnsupportedOperationException("Embedded payload not supported for " + getContextName());
    }
}