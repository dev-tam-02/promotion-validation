package vn.viettel.vds.promotion.validation.adapter.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;
import vn.viettel.vds.promotion.validation.domain.factory.RuleFactory;
import vn.viettel.vds.promotion.validation.domain.service.RuleEvaluationService;
import vn.viettel.vds.promotion.validation.domain.service.RuleVersioningDomainService;

import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Configuration for hexagonal architecture components
 */
@Configuration
@EnableAsync
public class ValidationHexagonalConfig {

    /**
     * Domain Services Configuration
     */
    @Bean
    public RuleFactory ruleFactory() {
        return new RuleFactory();
    }

    @Bean
    public RuleEvaluationService ruleEvaluationService() {
        ExecutorService executorService = ruleEvaluationExecutor().getThreadPoolExecutor();
        return new RuleEvaluationService(executorService);
    }

    @Bean
    public RuleVersioningDomainService ruleVersioningDomainService() {
        return new RuleVersioningDomainService();
    }

    /**
     * External Service Configuration
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * JSON Configuration
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules(); // Register JavaTimeModule, etc.
        return mapper;
    }

    /**
     * Async Executors Configuration
     */
    @Bean("eventPublisherExecutor")
    public Executor eventPublisherExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("event-publisher-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    @Bean("ruleEvaluationExecutor")
    public ThreadPoolTaskExecutor ruleEvaluationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("rule-eval-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}