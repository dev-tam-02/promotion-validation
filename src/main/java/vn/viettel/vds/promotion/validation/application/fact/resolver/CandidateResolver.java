package vn.viettel.vds.promotion.validation.application.fact.resolver;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.domain.fact.CandidateFact;
import vn.viettel.vds.promotion.validation.domain.fact.FactRequest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@Component
public class CandidateResolver extends AbstractFactResolver<CandidateFact> {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;

    public CandidateResolver(
            @Qualifier("catalogServiceCircuitBreaker") CircuitBreaker circuitBreaker,
            @Qualifier("catalogServiceRetry") Retry retry,
            ObjectMapper objectMapper
    ) {
        super(circuitBreaker, retry);
        this.objectMapper = objectMapper;
        this.baseUrl = "http://catalog-service"; // Can be made configurable

        // Use Java 21 HTTP Client with virtual threads
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .executor(java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor())
                .build();
    }

    @Override
    public String getContextName() {
        return "candidate";
    }

    @Override
    public CandidateFact resolveFromIds(FactRequest request) {
        if (request.candidate() == null || request.candidate().key() == null) {
            return null;
        }

        log.debug("Resolving candidate facts for key: {}", request.candidate().key());

        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/promotions/" + request.candidate().key() + "/details"))
                    .timeout(Duration.ofMillis(getTimeoutMs()))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Map<String, Object> candidateData = objectMapper.readValue(response.body(), new TypeReference<>() {
                });
                CandidateFact result = mapToCandidateFact(candidateData);
                log.debug("Successfully resolved candidate facts for: {}", request.candidate().key());
                return result;
            } else {
                log.warn("Catalog service returned status {} for candidateKey: {}", response.statusCode(), request.candidate().key());
                return getPartialResult(request);
            }
        } catch (Exception e) {
            log.warn("Failed to resolve candidate facts for {}: {}", request.candidate().key(), e.getMessage());
            throw new RuntimeException("Failed to resolve candidate facts", e);
        }
    }

    @Override
    public CandidateFact resolveFromEmbeddedPayload(FactRequest request, Map<String, Object> embeddedData) {
        Map<String, Object> candidateData = (Map<String, Object>) embeddedData.get("candidate");
        if (candidateData == null) {
            return null;
        }

        log.debug("Resolving candidate facts from embedded payload");
        return mapToCandidateFact(candidateData);
    }

    @Override
    public boolean supportsEmbeddedPayload() {
        return true;
    }

    @Override
    public int getTimeoutMs() {
        return 3000; // 3 seconds for catalog service
    }

    @Override
    public int getPriority() {
        return 15; // High priority
    }

    @Override
    protected CandidateFact getPartialResult(FactRequest request) {
        if (request.candidate() == null) {
            return null;
        }

        return CandidateFact.builder()
                .type(request.candidate().type())
                .key(request.candidate().key())
                .campaignId(request.candidate().campaignId())
                .status("ACTIVE") // Default assumption
                .build();
    }

    private CandidateFact mapToCandidateFact(Map<String, Object> data) {
        CandidateFact.Builder builder = CandidateFact.builder();

        if (data.get("type") != null) {
            builder.type((String) data.get("type"));
        }
        if (data.get("key") != null) {
            builder.key((String) data.get("key"));
        }
        if (data.get("campaignId") != null) {
            builder.campaignId((String) data.get("campaignId"));
        }
        if (data.get("promotionId") != null) {
            builder.promotionId((String) data.get("promotionId"));
        }
        if (data.get("name") != null) {
            builder.name((String) data.get("name"));
        }
        if (data.get("description") != null) {
            builder.description((String) data.get("description"));
        }
        if (data.get("status") != null) {
            builder.status((String) data.get("status"));
        }
        if (data.get("startDate") != null) {
            builder.startDate(Instant.parse((String) data.get("startDate")));
        }
        if (data.get("endDate") != null) {
            builder.endDate(Instant.parse((String) data.get("endDate")));
        }
        if (data.get("configuration") != null) {
            builder.configuration((Map<String, Object>) data.get("configuration"));
        }
        if (data.get("constraints") != null) {
            builder.constraints((Map<String, Object>) data.get("constraints"));
        }

        return builder.build();
    }
}