package vn.viettel.vds.promotion.validation.application.fact.resolver;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.domain.fact.FactRequest;
import vn.viettel.vds.promotion.validation.domain.fact.LimitsFact;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class LimitsResolver extends AbstractFactResolver<LimitsFact> {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;

    public LimitsResolver(
            @Qualifier("redemptionServiceCircuitBreaker") CircuitBreaker circuitBreaker,
            @Qualifier("redemptionServiceRetry") Retry retry,
            ObjectMapper objectMapper
    ) {
        super(circuitBreaker, retry);
        this.objectMapper = objectMapper;
        this.baseUrl = "http://redemption-service"; // Can be made configurable

        // Use Java 21 HTTP Client with virtual threads
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .executor(java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor())
                .build();
    }

    @Override
    public String getContextName() {
        return "limits";
    }

    @Override
    public LimitsFact resolveFromIds(FactRequest request) {
        if (request.customerId() == null) {
            return null;
        }

        log.debug("Resolving limits facts for customerId: {}", request.customerId());

        try {
            String campaignId = request.candidate() != null ? request.candidate().campaignId() : "";
            String url = baseUrl + "/api/limits/snapshot?customerId=" + request.customerId() + "&campaignId=" + campaignId;

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMillis(getTimeoutMs()))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Map<String, Object> limitsData = objectMapper.readValue(response.body(), new TypeReference<>() {
                });
                LimitsFact result = mapToLimitsFact(limitsData);
                log.debug("Successfully resolved limits facts for: {}", request.customerId());
                return result;
            } else {
                log.warn("Redemption service returned status {} for customerId: {}", response.statusCode(), request.customerId());
                return getPartialResult(request);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while resolving limits facts for {}: {}", request.customerId(), e.getMessage());
            throw new RuntimeException("Failed to resolve limits facts", e);
        } catch (Exception e) {
            log.warn("Failed to resolve limits facts for {}: {}", request.customerId(), e.getMessage());
            throw new RuntimeException("Failed to resolve limits facts", e);
        }
    }

    @Override
    public boolean supportsEmbeddedPayload() {
        return false; // Limits must be fetched real-time
    }

    @Override
    public int getTimeoutMs() {
        return 2000; // 2 seconds for limits service
    }

    @Override
    public int getPriority() {
        return 30; // Medium priority
    }

    @Override
    protected LimitsFact getPartialResult(FactRequest request) {
        return LimitsFact.builder()
                .snapshotAt(Instant.now())
                .build();
    }

    private LimitsFact mapToLimitsFact(Map<String, Object> data) {
        LimitsFact.Builder builder = LimitsFact.builder();

        if (data.get("globalLimits") != null) {
            List<Map<String, Object>> globalLimitsData = (List<Map<String, Object>>) data.get("globalLimits");
            List<LimitsFact.LimitInfo> globalLimits = globalLimitsData.stream()
                    .map(this::mapToLimitInfo)
                    .toList();
            builder.globalLimits(globalLimits);
        }

        if (data.get("customerLimits") != null) {
            List<Map<String, Object>> customerLimitsData = (List<Map<String, Object>>) data.get("customerLimits");
            List<LimitsFact.LimitInfo> customerLimits = customerLimitsData.stream()
                    .map(this::mapToLimitInfo)
                    .toList();
            builder.customerLimits(customerLimits);
        }

        if (data.get("campaignLimits") != null) {
            List<Map<String, Object>> campaignLimitsData = (List<Map<String, Object>>) data.get("campaignLimits");
            List<LimitsFact.LimitInfo> campaignLimits = campaignLimitsData.stream()
                    .map(this::mapToLimitInfo)
                    .toList();
            builder.campaignLimits(campaignLimits);
        }

        if (data.get("counters") != null) {
            Map<String, Map<String, Object>> countersData = (Map<String, Map<String, Object>>) data.get("counters");
            Map<String, LimitsFact.UsageCounter> counters = countersData.entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> mapToUsageCounter(entry.getValue())
                    ));
            builder.counters(counters);
        }

        if (data.get("snapshotAt") != null) {
            builder.snapshotAt(Instant.parse((String) data.get("snapshotAt")));
        }

        return builder.build();
    }

    private LimitsFact.LimitInfo mapToLimitInfo(Map<String, Object> data) {
        return new LimitsFact.LimitInfo(
                (String) data.get("type"),
                (String) data.get("scope"),
                (String) data.get("period"),
                data.get("limit") != null ? new BigDecimal(data.get("limit").toString()) : null,
                data.get("used") != null ? new BigDecimal(data.get("used").toString()) : null,
                data.get("remaining") != null ? new BigDecimal(data.get("remaining").toString()) : null
        );
    }

    private LimitsFact.UsageCounter mapToUsageCounter(Map<String, Object> data) {
        return new LimitsFact.UsageCounter(
                (String) data.get("key"),
                data.get("count") != null ? new BigDecimal(data.get("count").toString()) : null,
                (String) data.get("period"),
                data.get("lastUpdated") != null ? Instant.parse((String) data.get("lastUpdated")) : null,
                data.get("resetAt") != null ? Instant.parse((String) data.get("resetAt")) : null
        );
    }
}