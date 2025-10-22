package vn.viettel.vds.promotion.validation.adapter.in.web;

import com.promix.platform.web.annotation.ResponseWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import vn.viettel.vds.promotion.validation.infrastructure.resilience.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@ResponseWrapper
@RequestMapping("${spring.application.context-path}/api/v1/resilience")
public class ResilienceMonitoringController {

    private static final Logger logger = LoggerFactory.getLogger(ResilienceMonitoringController.class);

    private final ResilienceOrchestrator resilienceOrchestrator;
    private final CircuitBreakerService circuitBreakerService;
    private final RetryService retryService;
    private final TimeoutService timeoutService;
    private final BulkheadService bulkheadService;
    private final FallbackService fallbackService;

    public ResilienceMonitoringController(ResilienceOrchestrator resilienceOrchestrator,
                                          CircuitBreakerService circuitBreakerService,
                                          RetryService retryService,
                                          TimeoutService timeoutService,
                                          BulkheadService bulkheadService,
                                          FallbackService fallbackService) {
        this.resilienceOrchestrator = resilienceOrchestrator;
        this.circuitBreakerService = circuitBreakerService;
        this.retryService = retryService;
        this.timeoutService = timeoutService;
        this.bulkheadService = bulkheadService;
        this.fallbackService = fallbackService;
    }

    @GetMapping("/status")
    public ResilienceOrchestrator.ResilienceStatus getResilienceStatus() {
        return resilienceOrchestrator.getResilienceStatus();
    }

    @GetMapping("/circuit-breaker/{name}")
    public CircuitBreakerService.CircuitBreakerStatus getCircuitBreakerStatus(@PathVariable String name) {
        return circuitBreakerService.getCircuitBreakerStatus(name);
    }

    @PostMapping("/circuit-breaker/{name}/reset")
    public void resetCircuitBreaker(@PathVariable String name) {
        circuitBreakerService.resetCircuitBreaker(name);
        logger.info("Circuit breaker reset: {}", name);
    }

    @PostMapping("/circuit-breaker/{name}/open")
    public void openCircuitBreaker(@PathVariable String name) {
        circuitBreakerService.transitionToOpenState(name);
        logger.info("Circuit breaker manually opened: {}", name);
    }

    @PostMapping("/circuit-breaker/{name}/close")
    public void closeCircuitBreaker(@PathVariable String name) {
        circuitBreakerService.transitionToClosedState(name);
        logger.info("Circuit breaker manually closed: {}", name);
    }

    @GetMapping("/retry/{name}")
    public RetryService.RetryStatus getRetryStatus(@PathVariable String name) {
        return retryService.getRetryStatus(name);
    }

    @GetMapping("/timeout/{name}")
    public TimeoutService.TimeoutStatus getTimeoutStatus(@PathVariable String name) {
        return timeoutService.getTimeoutStatus(name);
    }

    @GetMapping("/bulkhead/{name}")
    public BulkheadService.BulkheadStatus getBulkheadStatus(@PathVariable String name) {
        return bulkheadService.getBulkheadStatus(name);
    }

    public FallbackService.FallbackStats getFallbackStats() {
        return fallbackService.getFallbackStats();
    }

    @PostMapping("/fallback/reset")
    public void resetFallbackStats() {
        fallbackService.resetFallbackStats();
        logger.info("Fallback stats reset");
    }

    @PostMapping("/reset-all")
    public void resetAllResilienceComponents() {
        resilienceOrchestrator.resetResilienceComponents();
        logger.info("All resilience components reset");
    }

    @GetMapping("/health")
    public Map<String, Object> getResilienceHealth() {
        ResilienceOrchestrator.ResilienceStatus status = resilienceOrchestrator.getResilienceStatus();

        Map<String, Object> health = new HashMap<>();
        health.put("status", status.getOverallStatus());
        health.put("enabled", status.isEnabled());
        health.put("healthy", status.isHealthy());
        health.put("timestamp", status.getTimestamp());

        Map<String, Object> components = new HashMap<>();
        components.put("circuitBreaker", Map.of(
                "state", status.getCircuitBreakerStatus().getState(),
                "failureRate", status.getCircuitBreakerStatus().getFailureRate(),
                "healthy", status.getCircuitBreakerStatus().isHealthy()
        ));
        components.put("retry", Map.of(
                "successRate", status.getRetryStatus().getSuccessRate(),
                "totalCalls", status.getRetryStatus().getTotalCalls()
        ));
        components.put("timeout", Map.of(
                "timeoutRate", status.getTimeoutStatus().getTimeoutRate(),
                "successRate", status.getTimeoutStatus().getSuccessRate()
        ));
        components.put("bulkhead", Map.of(
                "utilization", status.getBulkheadStatus().getUtilizationRate(),
                "atCapacity", status.getBulkheadStatus().isAtCapacity()
        ));
        components.put("fallback", Map.of(
                "totalExecutions", status.getFallbackStats().getTotalFallbackExecutions(),
                "mostUsedStrategy", status.getFallbackStats().getMostUsedStrategy()
        ));

        health.put("components", components);

        return health;
    }
}