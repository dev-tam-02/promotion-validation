package vn.viettel.vds.promotion.validation.adapter.in.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * DTO for validation request from REST API
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationRequestDto {

    private String transactionId;
    private String promotionId;
    private String customerId;
    private String sessionId;
    private BigDecimal orderValue;
    private List<String> rules;
    private Map<String, Object> context;
    private Instant timestamp;

    // Nested DTOs for context
    private CustomerContextDto customerContext;
    private OrderContextDto orderContext;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerContextDto {
        private String customerId;
        private String segment;
        private String tier;
        private BigDecimal totalPurchaseAmount;
        private Integer transactionCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderContextDto {
        private String orderId;
        private BigDecimal orderValue;
        private Integer itemCount;
        private String channel;
        private Map<String, BigDecimal> productCategories;
    }
}