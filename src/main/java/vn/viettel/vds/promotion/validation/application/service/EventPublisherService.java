package vn.viettel.vds.promotion.validation.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import vn.viettel.vds.promotion.validation.domain.entity.OutboxEvent;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@Service
public class EventPublisherService {

    private static final Logger logger = LoggerFactory.getLogger(EventPublisherService.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String artifactServiceUrl;
    private final String redemptionServiceUrl;
    private final String eventBusUrl;

    public EventPublisherService(
            @Value("${integration.artifact-service.url:http://artifact-service:8080}") String artifactServiceUrl,
            @Value("${integration.redemption-service.url:http://redemption-service:8080}") String redemptionServiceUrl,
            @Value("${integration.event-bus.url:http://event-bus:8080}") String eventBusUrl) {

        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        this.objectMapper = new ObjectMapper();
        this.artifactServiceUrl = artifactServiceUrl;
        this.redemptionServiceUrl = redemptionServiceUrl;
        this.eventBusUrl = eventBusUrl;
    }

    /**
     * Publish rule published event
     */
    public boolean publishRulePublishedEvent(OutboxEvent event) {
        logger.debug("Publishing RulePublished event: id={}", event.getId());

        try {
            Map<String, Object> payload = event.getPayload();

            // Create event payload for external services
            Map<String, Object> eventPayload = Map.of(
                "eventType", "RulePublished",
                "eventId", event.getId(),
                "tenantId", event.getTenantId(),
                "timestamp", event.getCreatedAt().toString(),
                "data", payload
            );

            // Publish to multiple destinations
            boolean artifactSuccess = publishToArtifactService(eventPayload);
            boolean redemptionSuccess = publishToRedemptionService(eventPayload);
            boolean eventBusSuccess = publishToEventBus(eventPayload);

            // Consider success if at least one destination succeeds
            boolean overallSuccess = artifactSuccess || redemptionSuccess || eventBusSuccess;

            if (!overallSuccess) {
                logger.warn("Failed to publish RulePublished event to any destination: id={}", event.getId());
            }

            return overallSuccess;

        } catch (Exception e) {
            logger.error("Error publishing RulePublished event: id={}", event.getId(), e);
            return false;
        }
    }

    /**
     * Publish assignment changed event
     */
    public boolean publishAssignmentChangedEvent(OutboxEvent event) {
        logger.debug("Publishing AssignmentChanged event: id={}", event.getId());

        try {
            Map<String, Object> payload = event.getPayload();

            // Create event payload for external services
            Map<String, Object> eventPayload = Map.of(
                "eventType", "AssignmentChanged",
                "eventId", event.getId(),
                "tenantId", event.getTenantId(),
                "timestamp", event.getCreatedAt().toString(),
                "data", payload
            );

            // Publish primarily to redemption service and event bus
            boolean redemptionSuccess = publishToRedemptionService(eventPayload);
            boolean eventBusSuccess = publishToEventBus(eventPayload);

            boolean overallSuccess = redemptionSuccess || eventBusSuccess;

            if (!overallSuccess) {
                logger.warn("Failed to publish AssignmentChanged event to any destination: id={}", event.getId());
            }

            return overallSuccess;

        } catch (Exception e) {
            logger.error("Error publishing AssignmentChanged event: id={}", event.getId(), e);
            return false;
        }
    }

    /**
     * Publish to Artifact Service
     */
    private boolean publishToArtifactService(Map<String, Object> eventPayload) {
        try {
            String endpoint = artifactServiceUrl + "/api/v1/events/rule-published";
            return sendHttpEvent(endpoint, eventPayload, "Artifact Service");
        } catch (Exception e) {
            logger.warn("Failed to publish to Artifact Service", e);
            return false;
        }
    }

    /**
     * Publish to Redemption Service
     */
    private boolean publishToRedemptionService(Map<String, Object> eventPayload) {
        try {
            String eventType = (String) eventPayload.get("eventType");
            String endpoint = redemptionServiceUrl + "/api/v1/events/" +
                (eventType.equals("RulePublished") ? "rule-published" : "assignment-changed");
            return sendHttpEvent(endpoint, eventPayload, "Redemption Service");
        } catch (Exception e) {
            logger.warn("Failed to publish to Redemption Service", e);
            return false;
        }
    }

    /**
     * Publish to Event Bus (Kafka/RabbitMQ)
     */
    private boolean publishToEventBus(Map<String, Object> eventPayload) {
        try {
            String endpoint = eventBusUrl + "/api/v1/events/publish";
            return sendHttpEvent(endpoint, eventPayload, "Event Bus");
        } catch (Exception e) {
            logger.warn("Failed to publish to Event Bus", e);
            return false;
        }
    }

    /**
     * Send HTTP event to a specific endpoint
     */
    private boolean sendHttpEvent(String endpoint, Map<String, Object> payload, String serviceName) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .header("User-Agent", "ValidationService/1.0")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                logger.debug("Successfully published event to {}: status={}", serviceName, response.statusCode());
                return true;
            } else {
                logger.warn("Failed to publish event to {}: status={}, body={}",
                    serviceName, response.statusCode(), response.body());
                return false;
            }

        } catch (Exception e) {
            logger.error("Error sending HTTP event to {}: {}", serviceName, endpoint, e);
            return false;
        }
    }

    /**
     * Test connectivity to external services
     */
    public ServiceConnectivityStatus testConnectivity() {
        logger.info("Testing connectivity to external services");

        boolean artifactReachable = testServiceHealth(artifactServiceUrl + "/actuator/health", "Artifact Service");
        boolean redemptionReachable = testServiceHealth(redemptionServiceUrl + "/actuator/health", "Redemption Service");
        boolean eventBusReachable = testServiceHealth(eventBusUrl + "/health", "Event Bus");

        return new ServiceConnectivityStatus(artifactReachable, redemptionReachable, eventBusReachable);
    }

    /**
     * Test individual service health
     */
    private boolean testServiceHealth(String healthEndpoint, String serviceName) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(healthEndpoint))
                .header("User-Agent", "ValidationService/1.0")
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            boolean healthy = response.statusCode() == 200;

            logger.debug("{} health check: {}", serviceName, healthy ? "OK" : "FAIL");
            return healthy;

        } catch (Exception e) {
            logger.debug("{} health check failed: {}", serviceName, e.getMessage());
            return false;
        }
    }

    /**
     * Service connectivity status
     */
    public static class ServiceConnectivityStatus {
        private final boolean artifactServiceReachable;
        private final boolean redemptionServiceReachable;
        private final boolean eventBusReachable;

        public ServiceConnectivityStatus(boolean artifactServiceReachable,
                                       boolean redemptionServiceReachable,
                                       boolean eventBusReachable) {
            this.artifactServiceReachable = artifactServiceReachable;
            this.redemptionServiceReachable = redemptionServiceReachable;
            this.eventBusReachable = eventBusReachable;
        }

        public boolean isArtifactServiceReachable() { return artifactServiceReachable; }
        public boolean isRedemptionServiceReachable() { return redemptionServiceReachable; }
        public boolean isEventBusReachable() { return eventBusReachable; }

        public boolean isAllServicesReachable() {
            return artifactServiceReachable && redemptionServiceReachable && eventBusReachable;
        }

        public int getReachableCount() {
            int count = 0;
            if (artifactServiceReachable) count++;
            if (redemptionServiceReachable) count++;
            if (eventBusReachable) count++;
            return count;
        }
    }
}