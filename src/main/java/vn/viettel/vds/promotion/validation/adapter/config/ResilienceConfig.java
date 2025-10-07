package vn.viettel.vds.promotion.validation.adapter.config;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.viettel.vds.promotion.validation.infrastructure.resilience.ResilienceConfiguration;

@Configuration
public class ResilienceConfig {

    private final ResilienceConfiguration config;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;

    public ResilienceConfig(
            ResilienceConfiguration config,
            CircuitBreakerRegistry circuitBreakerRegistry,
            RetryRegistry retryRegistry) {
        this.config = config;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.retryRegistry = retryRegistry;
    }

    @Bean
    public CircuitBreaker factResolverCircuitBreaker() {
        return CircuitBreaker.ofDefaults("factResolver");
    }

    @Bean
    public CircuitBreaker customerServiceCircuitBreaker() {
        return CircuitBreaker.ofDefaults("customerService");
    }

    @Bean
    public CircuitBreaker orderServiceCircuitBreaker() {
        return CircuitBreaker.ofDefaults("orderService");
    }

    @Bean
    public CircuitBreaker catalogServiceCircuitBreaker() {
        return CircuitBreaker.ofDefaults("catalogService");
    }

    @Bean
    public CircuitBreaker redemptionServiceCircuitBreaker() {
        return CircuitBreaker.ofDefaults("redemptionService");
    }

    @Bean
    public Retry factResolverRetry() {
        return Retry.ofDefaults("factResolver");
    }

    @Bean
    public Retry customerServiceRetry() {
        return Retry.ofDefaults("customerService");
    }

    @Bean
    public Retry orderServiceRetry() {
        return Retry.ofDefaults("orderService");
    }

    @Bean
    public Retry catalogServiceRetry() {
        return Retry.ofDefaults("catalogService");
    }

    @Bean
    public Retry redemptionServiceRetry() {
        return Retry.ofDefaults("redemptionService");
    }

    @Bean
    public Bulkhead factResolverBulkhead() {
        return Bulkhead.ofDefaults("factResolver");
    }

    @Bean
    public TimeLimiter factResolverTimeLimiter() {
        return TimeLimiter.ofDefaults("factResolver");
    }

    /**
     * Circuit breaker for segments fact resolver.
     * Configuration is loaded from application.yml under resilience4j.circuitbreaker.instances.segmentsCircuitBreaker
     */
    @Bean
    public CircuitBreaker segmentsCircuitBreaker() {
        return circuitBreakerRegistry.circuitBreaker("segmentsCircuitBreaker");
    }

    /**
     * Retry configuration for segments fact resolver.
     * Configuration is loaded from application.yml under resilience4j.retry.instances.segmentsRetry
     */
    @Bean
    public Retry segmentsRetry() {
        return retryRegistry.retry("segmentsRetry");
    }
}