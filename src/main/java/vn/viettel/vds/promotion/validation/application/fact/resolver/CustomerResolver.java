package vn.viettel.vds.promotion.validation.application.fact.resolver;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.domain.exception.FactResolutionException;
import vn.viettel.vds.promotion.validation.domain.fact.CustomerFact;
import vn.viettel.vds.promotion.validation.domain.fact.FactRequest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
public class CustomerResolver extends AbstractFactResolver<CustomerFact> {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;

    public CustomerResolver(
            @Qualifier("customerServiceCircuitBreaker") CircuitBreaker circuitBreaker,
            @Qualifier("customerServiceRetry") Retry retry,
            ObjectMapper objectMapper
    ) {
        super(circuitBreaker, retry);
        this.objectMapper = objectMapper;
        this.baseUrl = "http://customer-service"; // Can be made configurable

        // Use Java 21 HTTP Client with virtual threads
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .executor(java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor())
                .build();
    }

    @Override
    public String getContextName() {
        return "customer";
    }

    @Override
    public CustomerFact resolveFromIds(FactRequest request) {
        if (request.customerId() == null) {
            return null;
        }

        if (log.isDebugEnabled()) {
            log.debug("Resolving customer facts for customerId: {}", request.customerId());
        }

        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/customers/" + request.customerId() + "/profile"))
                    .timeout(Duration.ofMillis(getTimeoutMs()))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Map<String, Object> customerData = objectMapper.readValue(response.body(), new TypeReference<>() {
                });
                CustomerFact result = mapToCustomerFact(customerData);
                if (log.isDebugEnabled()) {
                    log.debug("Successfully resolved customer facts for: {}", request.customerId());
                }
                return result;
            } else {
                if (log.isWarnEnabled()) {
                    log.warn("Customer service returned status {} for customerId: {}", response.statusCode(), request.customerId());
                }
                return getPartialResult(request);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore interrupted state
            throw new FactResolutionException("Failed to resolve customer facts for customerId: " + request.customerId(), e);
        } catch (Exception e) {
            throw new FactResolutionException("Failed to resolve customer facts for customerId: " + request.customerId(), e);
        }
    }

    @Override
    public CustomerFact resolveFromEmbeddedPayload(FactRequest request, Map<String, Object> embeddedData) {
        Map<String, Object> customerData = (Map<String, Object>) embeddedData.get("customer");
        if (customerData == null) {
            return null;
        }

        log.debug("Resolving customer facts from embedded payload");
        return mapToCustomerFact(customerData);
    }

    @Override
    public boolean supportsEmbeddedPayload() {
        return true;
    }

    @Override
    public int getTimeoutMs() {
        return 3000; // 3 seconds for customer service
    }

    @Override
    public int getPriority() {
        return 10; // High priority
    }

    @Override
    protected CustomerFact getPartialResult(FactRequest request) {
        return CustomerFact.builder()
                .customerId(request.customerId())
                .isActive(true) // Default assumption
                .build();
    }

    private CustomerFact mapToCustomerFact(Map<String, Object> data) {
        CustomerFact.Builder builder = CustomerFact.builder();

        // Map identification fields
        mapIdentificationFields(data, builder);

        // Map contact fields
        mapContactFields(data, builder);

        // Map preference fields
        mapPreferenceFields(data, builder);

        return builder.build();
    }

    private void mapIdentificationFields(Map<String, Object> data, CustomerFact.Builder builder) {
        if (data.get("customerId") != null) {
            builder.customerId((String) data.get("customerId"));
        }
        if (data.get("email") != null) {
            builder.email((String) data.get("email"));
        }
        if (data.get("phone") != null) {
            builder.phone((String) data.get("phone"));
        }
    }

    private void mapContactFields(Map<String, Object> data, CustomerFact.Builder builder) {
        if (data.get("tier") != null) {
            builder.tier((String) data.get("tier"));
        }
        if (data.get("registrationDate") != null) {
            builder.registrationDate(Instant.parse((String) data.get("registrationDate")));
        }
        if (data.get("lastActivityDate") != null) {
            builder.lastActivityDate(Instant.parse((String) data.get("lastActivityDate")));
        }
        if (data.get("isActive") != null) {
            builder.isActive((Boolean) data.get("isActive"));
        }
    }

    private void mapPreferenceFields(Map<String, Object> data, CustomerFact.Builder builder) {
        if (data.get("region") != null) {
            builder.region((String) data.get("region"));
        }
        if (data.get("language") != null) {
            builder.language((String) data.get("language"));
        }
        if (data.get("currency") != null) {
            builder.currency((String) data.get("currency"));
        }
        if (data.get("tags") != null) {
            builder.tags((List<String>) data.get("tags"));
        }
        if (data.get("attributes") != null) {
            builder.attributes((Map<String, Object>) data.get("attributes"));
        }
        if (data.get("preferences") != null) {
            builder.preferences((Map<String, Object>) data.get("preferences"));
        }
    }
}