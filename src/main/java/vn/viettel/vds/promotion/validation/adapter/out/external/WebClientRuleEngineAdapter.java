package vn.viettel.vds.promotion.validation.adapter.out.external;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import vn.viettel.vds.promotion.validation.application.port.out.RuleEngineClient;

import java.time.Duration;
import java.util.Map;

/**
 * WebClient-based adapter that calls pp-rule-engine's DRL registration endpoints.
 *
 * <p>Expected pp-rule-engine API (to be implemented — see Task 07 notes):
 * <ul>
 *   <li>POST /v1/rules {id, drl} → {bundleHash}</li>
 *   <li>PUT  /v1/rules/{id} {drl} → {bundleHash}</li>
 *   <li>DELETE /v1/rules/{id}</li>
 * </ul>
 *
 * <p>Resilience4j circuit breaker name: {@code ruleEngine}.
 * Configuration in application.yml under {@code resilience4j.circuitbreaker.instances.ruleEngine}.
 */
@Component
public class WebClientRuleEngineAdapter implements RuleEngineClient {

    private static final Logger log = LoggerFactory.getLogger(WebClientRuleEngineAdapter.class);

    private static final String BUNDLE_HASH_FIELD = "bundleHash";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final WebClient webClient;

    public WebClientRuleEngineAdapter(
            WebClient.Builder webClientBuilder,
            @Value("${integration.rule-engine.url:http://localhost:8080}") String ruleEngineUrl,
            @Value("${integration.rule-engine.service-path:/promotion/promotion-rule-engine}") String servicePath) {
        this.webClient = webClientBuilder
                .baseUrl(ruleEngineUrl + servicePath)
                .build();
    }

    @Override
    @CircuitBreaker(name = "ruleEngine", fallbackMethod = "registerFallback")
    public String register(String ruleId, String drl) {
        log.info("Registering rule with pp-rule-engine: ruleId={}", ruleId);

        try {
            Map<?, ?> response = webClient.post()
                    .uri("/v1/rules")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("id", ruleId, "drl", drl))
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError(), clientResponse ->
                            clientResponse.bodyToMono(String.class)
                                    .map(body -> new RuleEngineException(
                                            "Rule engine rejected DRL for ruleId=" + ruleId + ": " + body,
                                            clientResponse.statusCode().value()))
                    )
                    .bodyToMono(Map.class)
                    .timeout(TIMEOUT)
                    .block();

            if (response == null || !response.containsKey(BUNDLE_HASH_FIELD)) {
                throw new RuleEngineException("Rule engine returned no bundleHash for ruleId=" + ruleId, -1);
            }

            String bundleHash = (String) response.get(BUNDLE_HASH_FIELD);
            log.info("Rule registered successfully: ruleId={}, bundleHash={}", ruleId, bundleHash);
            return bundleHash;

        } catch (RuleEngineException ex) {
            throw ex;
        } catch (WebClientResponseException ex) {
            throw new RuleEngineException(
                    "Rule engine HTTP error for ruleId=" + ruleId + ": " + ex.getMessage(),
                    ex.getStatusCode().value());
        } catch (Exception ex) {
            throw new RuleEngineException("Rule engine call failed for ruleId=" + ruleId, ex);
        }
    }

    @Override
    @CircuitBreaker(name = "ruleEngine", fallbackMethod = "updateFallback")
    public String update(String ruleId, String drl) {
        log.info("Updating rule in pp-rule-engine: ruleId={}", ruleId);

        try {
            Map<?, ?> response = webClient.put()
                    .uri("/v1/rules/{id}", ruleId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("drl", drl))
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError(), clientResponse ->
                            clientResponse.bodyToMono(String.class)
                                    .map(body -> new RuleEngineException(
                                            "Rule engine rejected DRL update for ruleId=" + ruleId + ": " + body,
                                            clientResponse.statusCode().value()))
                    )
                    .bodyToMono(Map.class)
                    .timeout(TIMEOUT)
                    .block();

            if (response == null || !response.containsKey(BUNDLE_HASH_FIELD)) {
                throw new RuleEngineException("Rule engine returned no bundleHash on update for ruleId=" + ruleId, -1);
            }

            String bundleHash = (String) response.get(BUNDLE_HASH_FIELD);
            log.info("Rule updated successfully: ruleId={}, bundleHash={}", ruleId, bundleHash);
            return bundleHash;

        } catch (RuleEngineException ex) {
            throw ex;
        } catch (WebClientResponseException ex) {
            throw new RuleEngineException(
                    "Rule engine HTTP error on update for ruleId=" + ruleId + ": " + ex.getMessage(),
                    ex.getStatusCode().value());
        } catch (Exception ex) {
            throw new RuleEngineException("Rule engine update call failed for ruleId=" + ruleId, ex);
        }
    }

    @Override
    @CircuitBreaker(name = "ruleEngine", fallbackMethod = "deleteFallback")
    public void delete(String ruleId) {
        log.info("Deleting rule from pp-rule-engine: ruleId={}", ruleId);

        try {
            webClient.delete()
                    .uri("/v1/rules/{id}", ruleId)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .timeout(TIMEOUT)
                    .block();

            log.info("Rule deleted from engine: ruleId={}", ruleId);

        } catch (WebClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                log.warn("Rule not found in engine on delete (idempotent): ruleId={}", ruleId);
                return;
            }
            throw new RuleEngineException(
                    "Rule engine HTTP error on delete for ruleId=" + ruleId + ": " + ex.getMessage(),
                    ex.getStatusCode().value());
        } catch (Exception ex) {
            throw new RuleEngineException("Rule engine delete call failed for ruleId=" + ruleId, ex);
        }
    }

    // ---------- Resilience4j fallback methods ----------

    @SuppressWarnings("unused")
    private String registerFallback(String ruleId, String drl, Throwable ex) {
        log.warn("Circuit breaker OPEN — rule engine unavailable on register: ruleId={}, cause={}",
                ruleId, ex.getMessage());
        throw new RuleEngineException(
                "Rule engine circuit breaker open; rule " + ruleId + " stays DRAFT", ex);
    }

    @SuppressWarnings("unused")
    private String updateFallback(String ruleId, String drl, Throwable ex) {
        log.warn("Circuit breaker OPEN — rule engine unavailable on update: ruleId={}, cause={}",
                ruleId, ex.getMessage());
        throw new RuleEngineException(
                "Rule engine circuit breaker open; rule " + ruleId + " stays DRAFT", ex);
    }

    @SuppressWarnings("unused")
    private void deleteFallback(String ruleId, Throwable ex) {
        log.warn("Circuit breaker OPEN — rule engine unavailable on delete: ruleId={}, cause={}",
                ruleId, ex.getMessage());
        // Deletion failure is non-fatal; log and continue
    }
}
