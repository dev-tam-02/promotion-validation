package vn.viettel.vds.promotion.validation.application.fact.resolver;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.domain.fact.DiscountFact;
import vn.viettel.vds.promotion.validation.domain.fact.FactRequest;
import vn.viettel.vds.promotion.validation.domain.fact.OrderFact;
import vn.viettel.vds.promotion.validation.domain.fact.OrderItemFact;

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
public class OrderResolver extends AbstractFactResolver<OrderFact> {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;

    public OrderResolver(
            @Qualifier("orderServiceCircuitBreaker") CircuitBreaker circuitBreaker,
            @Qualifier("orderServiceRetry") Retry retry,
            ObjectMapper objectMapper
    ) {
        super(circuitBreaker, retry);
        this.objectMapper = objectMapper;
        this.baseUrl = "http://order-service"; // Can be made configurable

        // Use Java 21 HTTP Client with virtual threads
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .executor(java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor())
                .build();
    }

    @Override
    public String getContextName() {
        return "order";
    }

    @Override
    public OrderFact resolveFromIds(FactRequest request) {
        if (request.orderId() == null) {
            return null;
        }

        log.debug("Resolving order facts for orderId: {}", request.orderId());

        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/orders/" + request.orderId() + "/details"))
                    .timeout(Duration.ofMillis(getTimeoutMs()))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Map<String, Object> orderData = objectMapper.readValue(response.body(), new TypeReference<>() {
                });
                OrderFact result = mapToOrderFact(orderData);
                log.debug("Successfully resolved order facts for: {}", request.orderId());
                return result;
            } else {
                log.warn("Order service returned status {} for orderId: {}", response.statusCode(), request.orderId());
                return getPartialResult(request);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while resolving order facts for {}: {}", request.orderId(), e.getMessage());
            throw new RuntimeException("Failed to resolve order facts", e);
        } catch (Exception e) {
            log.warn("Failed to resolve order facts for {}: {}", request.orderId(), e.getMessage());
            throw new RuntimeException("Failed to resolve order facts", e);
        }
    }

    @Override
    public OrderFact resolveFromEmbeddedPayload(FactRequest request, Map<String, Object> embeddedData) {
        Map<String, Object> orderData = (Map<String, Object>) embeddedData.get("order");
        if (orderData == null) {
            return null;
        }

        log.debug("Resolving order facts from embedded payload");
        return mapToOrderFact(orderData);
    }

    @Override
    public boolean supportsEmbeddedPayload() {
        return true;
    }

    @Override
    public int getTimeoutMs() {
        return 4000; // 4 seconds for order service
    }

    @Override
    public int getPriority() {
        return 20; // High priority
    }

    @Override
    protected OrderFact getPartialResult(FactRequest request) {
        return OrderFact.builder()
                .orderId(request.orderId())
                .customerId(request.customerId())
                .status("UNKNOWN")
                .totalAmount(BigDecimal.ZERO)
                .build();
    }

    private OrderFact mapToOrderFact(Map<String, Object> data) {
        OrderFact.Builder builder = OrderFact.builder();

        if (data.get("orderId") != null) {
            builder.orderId((String) data.get("orderId"));
        }
        if (data.get("customerId") != null) {
            builder.customerId((String) data.get("customerId"));
        }
        if (data.get("orderDate") != null) {
            builder.orderDate(Instant.parse((String) data.get("orderDate")));
        }
        if (data.get("status") != null) {
            builder.status((String) data.get("status"));
        }
        if (data.get("totalAmount") != null) {
            builder.totalAmount(new BigDecimal(data.get("totalAmount").toString()));
        }
        if (data.get("subtotal") != null) {
            builder.subtotal(new BigDecimal(data.get("subtotal").toString()));
        }
        if (data.get("tax") != null) {
            builder.tax(new BigDecimal(data.get("tax").toString()));
        }
        if (data.get("shipping") != null) {
            builder.shipping(new BigDecimal(data.get("shipping").toString()));
        }
        if (data.get("currency") != null) {
            builder.currency((String) data.get("currency"));
        }
        if (data.get("channel") != null) {
            builder.channel((String) data.get("channel"));
        }
        if (data.get("paymentMethod") != null) {
            builder.paymentMethod((String) data.get("paymentMethod"));
        }
        if (data.get("items") != null) {
            List<Map<String, Object>> itemsData = (List<Map<String, Object>>) data.get("items");
            List<OrderItemFact> items = itemsData.stream()
                    .map(this::mapToOrderItemFact)
                    .collect(Collectors.toList());
            builder.items(items);

            // Calculate derived fields
            if (!items.isEmpty()) {
                BigDecimal cheapest = items.stream()
                        .map(OrderItemFact::unitPrice)
                        .min(BigDecimal::compareTo)
                        .orElse(BigDecimal.ZERO);
                BigDecimal mostExpensive = items.stream()
                        .map(OrderItemFact::unitPrice)
                        .max(BigDecimal::compareTo)
                        .orElse(BigDecimal.ZERO);
                Integer totalQuantity = items.stream()
                        .mapToInt(OrderItemFact::quantity)
                        .sum();

                builder.cheapestItemPrice(cheapest);
                builder.mostExpensiveItemPrice(mostExpensive);
                builder.totalQuantity(totalQuantity);
            }
        }
        if (data.get("appliedDiscounts") != null) {
            List<Map<String, Object>> discountsData = (List<Map<String, Object>>) data.get("appliedDiscounts");
            List<DiscountFact> discounts = discountsData.stream()
                    .map(this::mapToDiscountFact)
                    .collect(Collectors.toList());
            builder.appliedDiscounts(discounts);
        }
        if (data.get("metadata") != null) {
            builder.metadata((Map<String, Object>) data.get("metadata"));
        }

        return builder.build();
    }

    private OrderItemFact mapToOrderItemFact(Map<String, Object> data) {
        OrderItemFact.Builder builder = OrderItemFact.builder();

        if (data.get("itemId") != null) {
            builder.itemId((String) data.get("itemId"));
        }
        if (data.get("sku") != null) {
            builder.sku((String) data.get("sku"));
        }
        if (data.get("canonicalSku") != null) {
            builder.canonicalSku((String) data.get("canonicalSku"));
        }
        if (data.get("productId") != null) {
            builder.productId((String) data.get("productId"));
        }
        if (data.get("name") != null) {
            builder.name((String) data.get("name"));
        }
        if (data.get("category") != null) {
            builder.category((String) data.get("category"));
        }
        if (data.get("brand") != null) {
            builder.brand((String) data.get("brand"));
        }
        if (data.get("tags") != null) {
            builder.tags((List<String>) data.get("tags"));
        }
        if (data.get("unitPrice") != null) {
            builder.unitPrice(new BigDecimal(data.get("unitPrice").toString()));
        }
        if (data.get("quantity") != null) {
            builder.quantity((Integer) data.get("quantity"));
        }
        if (data.get("totalPrice") != null) {
            builder.totalPrice(new BigDecimal(data.get("totalPrice").toString()));
        }
        if (data.get("discountAmount") != null) {
            builder.discountAmount(new BigDecimal(data.get("discountAmount").toString()));
        }
        if (data.get("unit") != null) {
            builder.unit((String) data.get("unit"));
        }
        if (data.get("attributes") != null) {
            builder.attributes((Map<String, Object>) data.get("attributes"));
        }

        return builder.build();
    }

    private DiscountFact mapToDiscountFact(Map<String, Object> data) {
        DiscountFact.Builder builder = DiscountFact.builder();

        if (data.get("discountId") != null) {
            builder.discountId((String) data.get("discountId"));
        }
        if (data.get("type") != null) {
            builder.type((String) data.get("type"));
        }
        if (data.get("code") != null) {
            builder.code((String) data.get("code"));
        }
        if (data.get("amount") != null) {
            builder.amount(new BigDecimal(data.get("amount").toString()));
        }
        if (data.get("percentage") != null) {
            builder.percentage(new BigDecimal(data.get("percentage").toString()));
        }
        if (data.get("scope") != null) {
            builder.scope((String) data.get("scope"));
        }
        if (data.get("status") != null) {
            builder.status((String) data.get("status"));
        }
        if (data.get("metadata") != null) {
            builder.metadata((Map<String, Object>) data.get("metadata"));
        }

        return builder.build();
    }
}