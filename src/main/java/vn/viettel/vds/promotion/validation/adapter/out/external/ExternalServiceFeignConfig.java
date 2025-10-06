package vn.viettel.vds.promotion.validation.adapter.out.external;

import feign.Logger;
import feign.Request;
import feign.Retryer;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Feign configuration for external services
 */
@Configuration
public class ExternalServiceFeignConfig {

    @Bean
    public Logger.Level externalServiceFeignLoggerLevel() {
        return Logger.Level.BASIC;
    }

    @Bean
    public Request.Options externalServiceFeignRequestOptions() {
        return new Request.Options(
                5000,  // connect timeout (5 seconds)
                TimeUnit.MILLISECONDS,
                10000, // read timeout (10 seconds)
                TimeUnit.MILLISECONDS,
                true   // follow redirects
        );
    }

    @Bean
    public Retryer externalServiceFeignRetryer() {
        return Retryer.NEVER_RETRY; // Use Resilience4j retry instead
    }

    @Bean
    public ErrorDecoder externalServiceFeignErrorDecoder() {
        return new ExternalServiceFeignErrorDecoder();
    }

}