package vn.viettel.vds.promotion.validation.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ValidationEngineProperties.class)
public class ValidationEngineClientConfig {

    @Bean
    public CircuitBreaker validationEngineCircuitBreaker(ValidationEngineProperties properties,
                                                        CircuitBreakerRegistry registry) {
        ValidationEngineProperties.CircuitBreakerConfig cbConfig = properties.getCircuitBreaker();

        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(cbConfig.getFailureRateThreshold())
                .waitDurationInOpenState(cbConfig.getWaitDurationInOpenState())
                .slidingWindowSize(cbConfig.getSlidingWindowSize())
                .minimumNumberOfCalls(cbConfig.getMinimumNumberOfCalls())
                .build();

        return registry.circuitBreaker("validationEngine", config);
    }

    @Bean
    public Retry validationEngineRetry(ValidationEngineProperties properties,
                                      RetryRegistry registry) {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(properties.getRetryAttempts())
                .waitDuration(java.time.Duration.ofMillis(1000))
                .retryExceptions(
                    feign.FeignException.class,
                    java.net.ConnectException.class,
                    java.net.SocketTimeoutException.class
                )
                .build();

        return registry.retry("validationEngine", config);
    }
}