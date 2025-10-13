package vn.viettel.vds.promotion.validation.adapter.out.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.domain.exception.EventPublishingException;
import vn.viettel.vds.promotion.validation.domain.model.OutboxEvent;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * HTTP-based event publisher for validation events.
 * Publishes events to external services via HTTP endpoints.
 */
@Component
public class ValidationHttpEventPublisher implements EventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(ValidationHttpEventPublisher.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String artifactServiceUrl;
    private final String redemptionServiceUrl;
    private final String eventBusUrl;

    public ValidationHttpEventPublisher(
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

    @Override
    public boolean supports(String destination) {
        // Support HTTP-based destinations
        return destination != null &&
                (destination.startsWith("http:") || destination.startsWith("https:"));
    }

    @Override
    public void publish(OutboxEvent event) {
        logger.debug("Publishing HTTP event: id={}, type={}", event.getId(), event.getEventType());

        try {
            // Create event payload for external services
            Map<String, Object> eventPayload = Map.of(
                    "eventType", event.getEventType(),
                    "eventId", event.getId(),
                    "aggregateType", event.getAggregateType(),
                    "aggregateId", event.getAggregateId(),
                    "timestamp", event.getCreatedAt().toString(),
                    "data", event.getPayload()
            );

            boolean success = false;

            // Route based on event type
            if ("rule.published".equals(event.getEventType())) {
                success = publishRulePublishedEvent(eventPayload);
            } else if ("assignment.changed".equals(event.getEventType())) {
                success = publishAssignmentChangedEvent(eventPayload);
            } else {
                logger.warn("Unknown event type for HTTP publishing: {}", event.getEventType());
            }

            if (!success) {
                throw new EventPublishingException("Failed to publish event to any destination", event.getId(), event.getEventType());
            }

        } catch (EventPublishingException e) {
            throw e;
        } catch (Exception e) {
            throw new EventPublishingException("HTTP event publishing failed for event id=" + event.getId(), e, event.getId(), event.getEventType());
        }
    }

    private boolean publishRulePublishedEvent(Map<String, Object> eventPayload) {
        logger.debug("Publishing RulePublished event via HTTP");

        // Publish to multiple destinations
        boolean artifactSuccess = publishToArtifactService(eventPayload);
        boolean redemptionSuccess = publishToRedemptionService(eventPayload);
        boolean eventBusSuccess = publishToEventBus(eventPayload);

        // Consider success if at least one destination succeeds
        boolean overallSuccess = artifactSuccess || redemptionSuccess || eventBusSuccess;

        if (!overallSuccess) {
            logger.warn("Failed to publish RulePublished event to any destination");
        }

        return overallSuccess;
    }

    private boolean publishAssignmentChangedEvent(Map<String, Object> eventPayload) {
        logger.debug("Publishing AssignmentChanged event via HTTP");

        // Publish primarily to redemption service and event bus
        boolean redemptionSuccess = publishToRedemptionService(eventPayload);
        boolean eventBusSuccess = publishToEventBus(eventPayload);

        boolean overallSuccess = redemptionSuccess || eventBusSuccess;

        if (!overallSuccess) {
            logger.warn("Failed to publish AssignmentChanged event to any destination");
        }

        return overallSuccess;
    }

    private boolean publishToArtifactService(Map<String, Object> eventPayload) {
        try {
            String endpoint = artifactServiceUrl + "/api/v1/events/rule-published";
            return sendHttpEvent(endpoint, eventPayload, "Artifact Service");
        } catch (Exception e) {
            logger.warn("Failed to publish to Artifact Service", e);
            return false;
        }
    }

    private boolean publishToRedemptionService(Map<String, Object> eventPayload) {
        try {
            String eventType = (String) eventPayload.get("eventType");
            String endpoint = redemptionServiceUrl + "/api/v1/events/" +
                    (eventType.equals("rule.published") ? "rule-published" : "assignment-changed");
            return sendHttpEvent(endpoint, eventPayload, "Redemption Service");
        } catch (Exception e) {
            logger.warn("Failed to publish to Redemption Service", e);
            return false;
        }
    }

    private boolean publishToEventBus(Map<String, Object> eventPayload) {
        try {
            String endpoint = eventBusUrl + "/api/v1/events/publish";
            return sendHttpEvent(endpoint, eventPayload, "Event Bus");
        } catch (Exception e) {
            logger.warn("Failed to publish to Event Bus", e);
            return false;
        }
    }

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
                if (logger.isWarnEnabled()) {
                    logger.warn("Failed to publish event to {}: status={}, body={}",
                            serviceName, response.statusCode(), response.body());
                }
                return false;
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted while sending HTTP event to {}: {}", serviceName, endpoint, e);
            return false;
        } catch (Exception e) {
            logger.error("Error sending HTTP event to {}: {}", serviceName, endpoint, e);
            return false;
        }
    }
}
