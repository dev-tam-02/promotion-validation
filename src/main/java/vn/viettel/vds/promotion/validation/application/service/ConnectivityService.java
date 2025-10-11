package vn.viettel.vds.promotion.validation.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Service for testing connectivity to external services
 */
@Service
public class ConnectivityService {

    private static final Logger logger = LoggerFactory.getLogger(ConnectivityService.class);

    private final HttpClient httpClient;
    private final String artifactServiceUrl;
    private final String redemptionServiceUrl;
    private final String eventBusUrl;

    public ConnectivityService(
            @Value("${integration.artifact-service.url:http://artifact-service:8080}") String artifactServiceUrl,
            @Value("${integration.redemption-service.url:http://redemption-service:8080}") String redemptionServiceUrl,
            @Value("${integration.event-bus.url:http://event-bus:8080}") String eventBusUrl) {

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.artifactServiceUrl = artifactServiceUrl;
        this.redemptionServiceUrl = redemptionServiceUrl;
        this.eventBusUrl = eventBusUrl;
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

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.debug("{} health check interrupted: {}", serviceName, e.getMessage());
            return false;
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

        public boolean isArtifactServiceReachable() {
            return artifactServiceReachable;
        }

        public boolean isRedemptionServiceReachable() {
            return redemptionServiceReachable;
        }

        public boolean isEventBusReachable() {
            return eventBusReachable;
        }

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
