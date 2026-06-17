package vn.viettel.vds.promotion.validation.adapter.in.web;

import com.promix.platform.web.annotation.ResponseWrapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.promix.platform.outbox.spi.OutboxService;
import vn.viettel.vds.promotion.validation.application.service.ConnectivityService;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@ResponseWrapper
@RequestMapping("${spring.application.context-path}/v1/admin")
public class AdminController {

    // String literal constants
    private static final String TIMESTAMP_KEY = "timestamp";
    private static final String ARTIFACT_SERVICE_KEY = "artifactService";
    private static final String REDEMPTION_SERVICE_KEY = "redemptionService";
    private static final String EVENT_BUS_KEY = "eventBus";
    private static final String REACHABLE_KEY = "reachable";
    private static final String STATUS_KEY = "status";

    private final OutboxService outboxService;
    private final ConnectivityService connectivityService;

    public AdminController(OutboxService outboxService,
                           ConnectivityService connectivityService) {
        this.outboxService = outboxService;
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
        OutboxService.OutboxStatistics stats = outboxService.getStatistics();
        Map<String, Object> outboxMetrics = Map.of(
                "pendingEvents", stats.pendingCount(),
                "processingEvents", stats.processingCount(),
                "publishedEvents", stats.publishedCount(),
                "failedEvents", stats.failedCount(),
                "deadLetterEvents", stats.deadLetterCount(),
                "totalEvents", stats.totalCount()
        );

        // External services status
        ConnectivityService.ServiceConnectivityStatus serviceStatus =
                connectivityService.testConnectivity();

        Map<String, Object> externalServices = Map.of(
                ARTIFACT_SERVICE_KEY, serviceStatus.isArtifactServiceReachable(),
                REDEMPTION_SERVICE_KEY, serviceStatus.isRedemptionServiceReachable(),
                EVENT_BUS_KEY, serviceStatus.isEventBusReachable(),
                "reachableCount", serviceStatus.getReachableCount(),
                "totalCount", 3
        );

        metrics.put(TIMESTAMP_KEY, Instant.now());
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
        OutboxService.OutboxStatistics stats = outboxService.getStatistics();
        return Map.of(
                "pendingEvents", stats.pendingCount(),
                "processingEvents", stats.processingCount(),
                "publishedEvents", stats.publishedCount(),
                "failedEvents", stats.failedCount(),
                "deadLetterEvents", stats.deadLetterCount(),
                "totalEvents", stats.totalCount(),
                TIMESTAMP_KEY, Instant.now()
        );
    }

    /**
     * Manually trigger outbox event processing.
     * The promix outbox scheduler also polls automatically; this forces a pass.
     */
    @PostMapping("/outbox/process")
    public Map<String, Object> triggerOutboxProcessing() {
        int processed = outboxService.processPendingEvents(100);
        return Map.of(
                "message", "Triggered promix outbox processing",
                "processed", processed,
                TIMESTAMP_KEY, Instant.now()
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
                TIMESTAMP_KEY, Instant.now(),
                "services", Map.of(
                        ARTIFACT_SERVICE_KEY, Map.of(
                                REACHABLE_KEY, status.isArtifactServiceReachable(),
                                STATUS_KEY, status.isArtifactServiceReachable() ? "UP" : "DOWN"
                        ),
                        REDEMPTION_SERVICE_KEY, Map.of(
                                REACHABLE_KEY, status.isRedemptionServiceReachable(),
                                STATUS_KEY, status.isRedemptionServiceReachable() ? "UP" : "DOWN"
                        ),
                        EVENT_BUS_KEY, Map.of(
                                REACHABLE_KEY, status.isEventBusReachable(),
                                STATUS_KEY, status.isEventBusReachable() ? "UP" : "DOWN"
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
                        "multiTenant", false
                ),
                "dependencies", Map.of(
                        "mongodb", "Required for data persistence",
                        ARTIFACT_SERVICE_KEY, "Optional for rule compilation",
                        REDEMPTION_SERVICE_KEY, "Optional for event publishing",
                        EVENT_BUS_KEY, "Optional for event streaming"
                ),
                TIMESTAMP_KEY, Instant.now()
        );
    }

    /**
     * Get current memory statistics
     * Note: Explicit garbage collection removed - JVM manages memory automatically
     */
    @PostMapping("/gc")
    public Map<String, Object> getMemoryStatistics() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();

        return Map.of(
                "message", "Memory statistics retrieved",
                "currentUsedMemory_MB", usedMemory / (1024 * 1024),
                "totalMemory_MB", runtime.totalMemory() / (1024 * 1024),
                "freeMemory_MB", runtime.freeMemory() / (1024 * 1024),
                TIMESTAMP_KEY, Instant.now(),
                "note", "JVM automatically manages garbage collection for optimal performance"
        );
    }

    /**
     * Get thread information using modern ThreadMXBean
     */
    @GetMapping("/threads")
    public Map<String, Object> getThreadInfo() {
        java.lang.management.ThreadMXBean threadMXBean =
                java.lang.management.ManagementFactory.getThreadMXBean();

        long[] threadIds = threadMXBean.getAllThreadIds();
        Map<String, Integer> threadStates = new HashMap<>();

        for (long threadId : threadIds) {
            java.lang.management.ThreadInfo threadInfo =
                    threadMXBean.getThreadInfo(threadId);
            if (threadInfo != null) {
                String state = threadInfo.getThreadState().name();
                threadStates.merge(state, 1, Integer::sum);
            }
        }

        return Map.of(
                "totalThreads", threadIds.length,
                "threadStates", threadStates,
                TIMESTAMP_KEY, Instant.now()
        );
    }
}