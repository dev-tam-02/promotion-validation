package vn.viettel.vds.promotion.validation.adapter.in.web;

import com.promix.platform.web.annotation.ResponseWrapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.viettel.vds.promotion.validation.application.service.ConnectivityService;
import vn.viettel.vds.promotion.validation.application.service.OutboxEventService;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@ResponseWrapper
@RequestMapping("/v1/admin")
public class AdminController {

    private final OutboxEventService outboxEventService;
    private final ConnectivityService connectivityService;

    public AdminController(OutboxEventService outboxEventService,
                           ConnectivityService connectivityService) {
        this.outboxEventService = outboxEventService;
        this.connectivityService = connectivityService;
    }

    /**
     * Get system metrics and status
     */
    @GetMapping("/metrics")
    public Map<String, Object> getSystemMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        // JVM metrics
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;

        Map<String, Object> jvmMetrics = Map.of(
                "totalMemoryMB", totalMemory / (1024 * 1024),
                "usedMemoryMB", usedMemory / (1024 * 1024),
                "freeMemoryMB", freeMemory / (1024 * 1024),
                "memoryUsagePercent", (double) usedMemory / totalMemory * 100,
                "processors", runtime.availableProcessors()
        );

        // Outbox metrics
        OutboxEventService.OutboxStatistics stats = outboxEventService.getStatistics();
        Map<String, Object> outboxMetrics = Map.of(
                "pendingEvents", stats.getPendingCount(),
                "processingEvents", stats.getProcessingCount(),
                "publishedEvents", stats.getPublishedCount(),
                "failedEvents", stats.getFailedCount(),
                "deadLetterEvents", stats.getDeadLetterCount(),
                "totalEvents", stats.getTotalCount()
        );

        // External services status
        ConnectivityService.ServiceConnectivityStatus serviceStatus =
                connectivityService.testConnectivity();

        Map<String, Object> externalServices = Map.of(
                "artifactService", serviceStatus.isArtifactServiceReachable(),
                "redemptionService", serviceStatus.isRedemptionServiceReachable(),
                "eventBus", serviceStatus.isEventBusReachable(),
                "reachableCount", serviceStatus.getReachableCount(),
                "totalCount", 3
        );

        metrics.put("timestamp", Instant.now());
        metrics.put("service", "validation-service");
        metrics.put("version", "1.0.0");
        metrics.put("jvm", jvmMetrics);
        metrics.put("outbox", outboxMetrics);
        metrics.put("externalServices", externalServices);

        return metrics;
    }

    /**
     * Get outbox event statistics
     */
    @GetMapping("/outbox/stats")
    public Map<String, Object> getOutboxStats() {
        OutboxEventService.OutboxStatistics stats = outboxEventService.getStatistics();
        return Map.of(
                "pendingEvents", stats.getPendingCount(),
                "processingEvents", stats.getProcessingCount(),
                "publishedEvents", stats.getPublishedCount(),
                "failedEvents", stats.getFailedCount(),
                "deadLetterEvents", stats.getDeadLetterCount(),
                "totalEvents", stats.getTotalCount(),
                "timestamp", Instant.now()
        );
    }

    /**
     * Manually trigger outbox event processing
     * Note: With Spring Batch, processing is automatic and scheduled
     */
    @PostMapping("/outbox/process")
    public Map<String, Object> triggerOutboxProcessing() {
        return Map.of(
                "message", "Outbox processing is handled automatically by Spring Batch",
                "note", "Events are processed in batches based on configuration at regular intervals",
                "timestamp", Instant.now()
        );
    }

    /**
     * Test external service connectivity
     */
    @GetMapping("/connectivity/test")
    public Map<String, Object> testConnectivity() {
        ConnectivityService.ServiceConnectivityStatus status =
                connectivityService.testConnectivity();

        return Map.of(
                "timestamp", Instant.now(),
                "services", Map.of(
                        "artifactService", Map.of(
                                "reachable", status.isArtifactServiceReachable(),
                                "status", status.isArtifactServiceReachable() ? "UP" : "DOWN"
                        ),
                        "redemptionService", Map.of(
                                "reachable", status.isRedemptionServiceReachable(),
                                "status", status.isRedemptionServiceReachable() ? "UP" : "DOWN"
                        ),
                        "eventBus", Map.of(
                                "reachable", status.isEventBusReachable(),
                                "status", status.isEventBusReachable() ? "UP" : "DOWN"
                        )
                ),
                "summary", Map.of(
                        "reachableCount", status.getReachableCount(),
                        "totalCount", 3,
                        "allReachable", status.isAllServicesReachable()
                )
        );
    }

    /**
     * Get service information
     */
    @GetMapping("/info")
    public Map<String, Object> getServiceInfo() {
        return Map.of(
                "service", Map.of(
                        "name", "validation-service",
                        "version", "1.0.0",
                        "description", "Validation service for promotion rules",
                        "buildTime", "2024-01-15T10:30:00Z" // This would come from build info
                ),
                "features", Map.of(
                        "ruleValidation", true,
                        "ruleSimulation", true,
                        "operatorRegistry", true,
                        "temporalPolicies", true,
                        "outboxEvents", true,
                        "multiTenant", true
                ),
                "dependencies", Map.of(
                        "mongodb", "Required for data persistence",
                        "artifactService", "Optional for rule compilation",
                        "redemptionService", "Optional for event publishing",
                        "eventBus", "Optional for event streaming"
                ),
                "timestamp", Instant.now()
        );
    }

    /**
     * Perform garbage collection (use with caution)
     */
    @PostMapping("/gc")
    public Map<String, Object> triggerGarbageCollection() {
        Runtime runtime = Runtime.getRuntime();
        long beforeGC = runtime.totalMemory() - runtime.freeMemory();

        System.gc(); // Suggest GC

        long afterGC = runtime.totalMemory() - runtime.freeMemory();
        long freedMemory = beforeGC - afterGC;

        return Map.of(
                "message", "Garbage collection suggested",
                "memoryBeforeGC_MB", beforeGC / (1024 * 1024),
                "memoryAfterGC_MB", afterGC / (1024 * 1024),
                "freedMemory_MB", freedMemory / (1024 * 1024),
                "timestamp", Instant.now(),
                "note", "GC is only suggested, actual collection is JVM-dependent"
        );
    }

    /**
     * Get thread dump (basic thread information)
     */
    @GetMapping("/threads")
    public Map<String, Object> getThreadInfo() {
        ThreadGroup rootGroup = Thread.currentThread().getThreadGroup();
        while (rootGroup.getParent() != null) {
            rootGroup = rootGroup.getParent();
        }

        int threadCount = rootGroup.activeCount();
        Thread[] threads = new Thread[threadCount * 2]; // Buffer for safety
        int actualCount = rootGroup.enumerate(threads, true);

        Map<String, Integer> threadStates = new HashMap<>();
        for (int i = 0; i < actualCount; i++) {
            if (threads[i] != null) {
                String state = threads[i].getState().name();
                threadStates.merge(state, 1, Integer::sum);
            }
        }

        return Map.of(
                "totalThreads", actualCount,
                "threadStates", threadStates,
                "timestamp", Instant.now()
        );
    }
}