package vn.viettel.vds.promotion.validation.infrastructure.resilience;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

@Service
public class BulkheadService {

    private static final Logger logger = LoggerFactory.getLogger(BulkheadService.class);

    private static final String BULKHEAD_EXECUTION_FAILED = "Bulkhead execution failed";
    private static final String FOR_SEPARATOR = " for: ";

    private final BulkheadRegistry bulkheadRegistry;
    private final ResilienceConfiguration config;

    public BulkheadService(ResilienceConfiguration config) {
        this.config = config;
        this.bulkheadRegistry = createBulkheadRegistry();
    }

    private BulkheadRegistry createBulkheadRegistry() {
        BulkheadConfig defaultConfig = BulkheadConfig.custom()
                .maxConcurrentCalls(config.getBulkhead().getMaxConcurrentCalls())
                .maxWaitDuration(Duration.ofMillis(config.getBulkhead().getMaxWaitDuration()))
                .build();

        BulkheadRegistry registry = BulkheadRegistry.of(defaultConfig);

        // Event listeners for monitoring are disabled to avoid API compatibility issues
        // TODO: Implement with correct Resilience4j event API

        return registry;
    }

    public Bulkhead getBulkhead(String name) {
        return bulkheadRegistry.bulkhead(name);
    }

    public <T> T executeWithBulkhead(String bulkheadName, Supplier<T> supplier) {
        Bulkhead bulkhead = getBulkhead(bulkheadName);
        Supplier<T> decoratedSupplier = Bulkhead.decorateSupplier(bulkhead, supplier);

        try {
            return decoratedSupplier.get();
        } catch (Exception e) {
            throw new BulkheadExecutionException(BULKHEAD_EXECUTION_FAILED + FOR_SEPARATOR + bulkheadName, e, bulkheadName);
        }
    }

    public <T> T executeWithBulkhead(String bulkheadName, Callable<T> callable) {
        Bulkhead bulkhead = getBulkhead(bulkheadName);
        Callable<T> decoratedCallable = Bulkhead.decorateCallable(bulkhead, callable);

        try {
            return decoratedCallable.call();
        } catch (Exception e) {
            throw new BulkheadExecutionException(BULKHEAD_EXECUTION_FAILED + FOR_SEPARATOR + bulkheadName, e, bulkheadName);
        }
    }

    public void executeWithBulkhead(String bulkheadName, Runnable runnable) {
        Bulkhead bulkhead = getBulkhead(bulkheadName);
        Runnable decoratedRunnable = Bulkhead.decorateRunnable(bulkhead, runnable);

        try {
            decoratedRunnable.run();
        } catch (Exception e) {
            throw new BulkheadExecutionException(BULKHEAD_EXECUTION_FAILED + FOR_SEPARATOR + bulkheadName, e, bulkheadName);
        }
    }

    public BulkheadStatus getBulkheadStatus(String name) {
        Bulkhead bulkhead = bulkheadRegistry.bulkhead(name);

        return new BulkheadStatus(
                name,
                bulkhead.getMetrics().getAvailableConcurrentCalls(),
                bulkhead.getMetrics().getMaxAllowedConcurrentCalls(),
                bulkhead.getBulkheadConfig().getMaxWaitDuration()
        );
    }

    public void resetBulkhead(String name) {
        // Bulkhead doesn't have reset functionality
        // The state is automatically managed by concurrent call counting
        logger.info("Bulkhead state is automatically managed for: {}", name);
    }

    // Exception class
    public static class BulkheadExecutionException extends RuntimeException {
        private final String bulkheadName;

        public BulkheadExecutionException(String message, Throwable cause, String bulkheadName) {
            super(message, cause);
            this.bulkheadName = bulkheadName;
        }

        public String getBulkheadName() {
            return bulkheadName;
        }
    }

    // Status class
    public static class BulkheadStatus {
        private final String name;
        private final int availableConcurrentCalls;
        private final int maxAllowedConcurrentCalls;
        private final Duration maxWaitDuration;

        public BulkheadStatus(String name, int availableConcurrentCalls,
                              int maxAllowedConcurrentCalls, Duration maxWaitDuration) {
            this.name = name;
            this.availableConcurrentCalls = availableConcurrentCalls;
            this.maxAllowedConcurrentCalls = maxAllowedConcurrentCalls;
            this.maxWaitDuration = maxWaitDuration;
        }

        // Getters
        public String getName() {
            return name;
        }

        public int getAvailableConcurrentCalls() {
            return availableConcurrentCalls;
        }

        public int getMaxAllowedConcurrentCalls() {
            return maxAllowedConcurrentCalls;
        }

        public Duration getMaxWaitDuration() {
            return maxWaitDuration;
        }

        public int getCurrentConcurrentCalls() {
            return maxAllowedConcurrentCalls - availableConcurrentCalls;
        }

        public double getUtilizationRate() {
            return maxAllowedConcurrentCalls > 0 ?
                    (double) getCurrentConcurrentCalls() / maxAllowedConcurrentCalls * 100 : 0.0;
        }

        public boolean isAtCapacity() {
            return availableConcurrentCalls == 0;
        }
    }
}