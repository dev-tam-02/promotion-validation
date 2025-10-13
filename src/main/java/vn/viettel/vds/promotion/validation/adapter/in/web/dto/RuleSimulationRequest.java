package vn.viettel.vds.promotion.validation.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

@Schema(description = "Request to simulate rule execution")
public class RuleSimulationRequest {

    @Schema(description = "Customer context for simulation", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Customer context is required")
    @JsonProperty("customerContext")
    private CustomerContext customerContext;

    @Schema(description = "Order context for simulation", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Order context is required")
    @JsonProperty("orderContext")
    private OrderContext orderContext;

    @Schema(description = "Additional facts for simulation")
    @JsonProperty("additionalFacts")
    private Map<String, Object> additionalFacts;

    // Getters and setters
    public CustomerContext getCustomerContext() {
        return customerContext;
    }

    public void setCustomerContext(CustomerContext customerContext) {
        this.customerContext = customerContext;
    }

    public OrderContext getOrderContext() {
        return orderContext;
    }

    public void setOrderContext(OrderContext orderContext) {
        this.orderContext = orderContext;
    }

    public Map<String, Object> getAdditionalFacts() {
        return additionalFacts;
    }

    public void setAdditionalFacts(Map<String, Object> additionalFacts) {
        this.additionalFacts = additionalFacts;
    }

    @Schema(description = "Customer context for rule simulation")
    public static class CustomerContext {
        @JsonProperty("customerId")
        private String customerId;

        @JsonProperty("segment")
        private String segment;

        @JsonProperty("loyaltyPoints")
        private Integer loyaltyPoints;

        @JsonProperty("registrationDate")
        private String registrationDate;

        @JsonProperty("attributes")
        private Map<String, Object> attributes;

        // Getters and setters
        public String getCustomerId() {
            return customerId;
        }

        public void setCustomerId(String customerId) {
            this.customerId = customerId;
        }

        public String getSegment() {
            return segment;
        }

        public void setSegment(String segment) {
            this.segment = segment;
        }

        public Integer getLoyaltyPoints() {
            return loyaltyPoints;
        }

        public void setLoyaltyPoints(Integer loyaltyPoints) {
            this.loyaltyPoints = loyaltyPoints;
        }

        public String getRegistrationDate() {
            return registrationDate;
        }

        public void setRegistrationDate(String registrationDate) {
            this.registrationDate = registrationDate;
        }

        public Map<String, Object> getAttributes() {
            return attributes;
        }

        public void setAttributes(Map<String, Object> attributes) {
            this.attributes = attributes;
        }
    }

    @Schema(description = "Order context for rule simulation")
    public static class OrderContext {
        @JsonProperty("orderId")
        private String orderId;

        @JsonProperty("totalAmount")
        private Double totalAmount;

        @JsonProperty("itemCount")
        private Integer itemCount;

        @JsonProperty("currency")
        private String currency;

        @JsonProperty("channel")
        private String channel;

        @JsonProperty("attributes")
        private Map<String, Object> attributes;

        // Getters and setters
        public String getOrderId() {
            return orderId;
        }

        public void setOrderId(String orderId) {
            this.orderId = orderId;
        }

        public Double getTotalAmount() {
            return totalAmount;
        }

        public void setTotalAmount(Double totalAmount) {
            this.totalAmount = totalAmount;
        }

        public Integer getItemCount() {
            return itemCount;
        }

        public void setItemCount(Integer itemCount) {
            this.itemCount = itemCount;
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }

        public String getChannel() {
            return channel;
        }

        public void setChannel(String channel) {
            this.channel = channel;
        }

        public Map<String, Object> getAttributes() {
            return attributes;
        }

        public void setAttributes(Map<String, Object> attributes) {
            this.attributes = attributes;
        }
    }
}