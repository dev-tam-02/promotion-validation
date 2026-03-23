package vn.viettel.vds.promotion.validation.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import vn.viettel.vds.promotion.validation.TestFixtures;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ValidationContext Tests")
class ValidationContextTest {

    @Nested
    @DisplayName("of()")
    class OfTests {

        @Test
        @DisplayName("Should create context from null map")
        void shouldCreateFromNullMap() {
            var context = ValidationContext.of(null);

            assertThat(context).isNotNull();
            assertThat(context.getRequestTime()).isNotNull();
            assertThat(context.getCustomer()).isNull();
            assertThat(context.getOrder()).isNull();
        }

        @Test
        @DisplayName("Should create context from empty map")
        void shouldCreateFromEmptyMap() {
            var context = ValidationContext.of(new HashMap<>());

            assertThat(context).isNotNull();
            assertThat(context.getCustomer()).isNull();
        }

        @Test
        @DisplayName("Should extract customer context from map")
        void shouldExtractCustomerContext() {
            Map<String, Object> customerData = new HashMap<>();
            customerData.put("customerId", "cust-1");
            customerData.put("segment", "VIP");
            customerData.put("tier", "GOLD");
            customerData.put("totalPurchaseAmount", 500000);
            customerData.put("transactionCount", 10);

            Map<String, Object> data = new HashMap<>();
            data.put("customer", customerData);

            var context = ValidationContext.of(data);

            assertThat(context.getCustomer()).isNotNull();
            assertThat(context.getCustomer().getCustomerId()).isEqualTo("cust-1");
            assertThat(context.getCustomer().getSegment()).isEqualTo("VIP");
        }

        @Test
        @DisplayName("Should extract order context from map")
        void shouldExtractOrderContext() {
            Map<String, Object> orderData = new HashMap<>();
            orderData.put("orderId", "ord-1");
            orderData.put("orderValue", 100000);
            orderData.put("itemCount", 5);
            orderData.put("channel", "ONLINE");

            Map<String, Object> data = new HashMap<>();
            data.put("order", orderData);

            var context = ValidationContext.of(data);

            assertThat(context.getOrder()).isNotNull();
            assertThat(context.getOrder().getOrderId()).isEqualTo("ord-1");
            assertThat(context.getOrder().getChannel()).isEqualTo("ONLINE");
        }
    }

    @Nested
    @DisplayName("getValue()")
    class GetValueTests {

        @Test
        @DisplayName("Should return null for null field name")
        void shouldReturnNull_forNullFieldName() {
            var context = TestFixtures.validContext();
            assertThat(context.getValue(null)).isNull();
        }

        @Test
        @DisplayName("Should return customer fields")
        void shouldReturnCustomerFields() {
            var context = TestFixtures.validContext();

            assertThat(context.getValue("customerId")).isEqualTo("cust-1");
            assertThat(context.getValue("segment")).isEqualTo("VIP");
            assertThat(context.getValue("tier")).isEqualTo("GOLD");
            assertThat(context.getValue("transactionCount")).isEqualTo(10);
        }

        @Test
        @DisplayName("Should return order fields")
        void shouldReturnOrderFields() {
            var context = TestFixtures.validContext();

            assertThat(context.getValue("orderId")).isEqualTo("ord-1");
            assertThat(context.getValue("channel")).isEqualTo("ONLINE");
            assertThat(context.getValue("itemCount")).isEqualTo(5);
        }

        @Test
        @DisplayName("Should return null for unknown field")
        void shouldReturnNull_forUnknownField() {
            var context = TestFixtures.validContext();
            assertThat(context.getValue("nonexistent")).isNull();
        }
    }

    @Nested
    @DisplayName("isValid()")
    class IsValidTests {

        @Test
        @DisplayName("Should return true when customer has valid id")
        void shouldReturnTrue_whenCustomerValid() {
            var context = TestFixtures.validContext();
            assertThat(context.isValid()).isTrue();
        }

        @Test
        @DisplayName("Should return false when customer is null")
        void shouldReturnFalse_whenCustomerNull() {
            var context = ValidationContext.builder().requestTime(java.time.Instant.now()).build();
            assertThat(context.isValid()).isFalse();
        }
    }

    @Nested
    @DisplayName("hasOrder()")
    class HasOrderTests {

        @Test
        @DisplayName("Should return true when order is valid")
        void shouldReturnTrue_whenOrderValid() {
            var context = TestFixtures.validContext();
            assertThat(context.hasOrder()).isTrue();
        }

        @Test
        @DisplayName("Should return false when order is null")
        void shouldReturnFalse_whenOrderNull() {
            var context = TestFixtures.contextWithCustomerOnly("cust-1", "VIP");
            assertThat(context.hasOrder()).isFalse();
        }
    }

    @Nested
    @DisplayName("CustomerContext")
    class CustomerContextTests {

        @Test
        @DisplayName("Should detect high value customer")
        void shouldDetectHighValueCustomer() {
            var customer = ValidationContext.CustomerContext.builder()
                    .customerId("cust-1")
                    .totalPurchaseAmount(BigDecimal.valueOf(2000000))
                    .build();

            assertThat(customer.isHighValue()).isTrue();
        }

        @Test
        @DisplayName("Should return false for non-high-value customer")
        void shouldReturnFalse_forNonHighValue() {
            var customer = ValidationContext.CustomerContext.builder()
                    .customerId("cust-1")
                    .totalPurchaseAmount(BigDecimal.valueOf(500000))
                    .build();

            assertThat(customer.isHighValue()).isFalse();
        }

        @Test
        @DisplayName("Should return false when totalPurchaseAmount is null")
        void shouldReturnFalse_whenAmountNull() {
            var customer = ValidationContext.CustomerContext.builder()
                    .customerId("cust-1")
                    .build();

            assertThat(customer.isHighValue()).isFalse();
        }
    }

    @Nested
    @DisplayName("OrderContext")
    class OrderContextTests {

        @Test
        @DisplayName("Should detect large order")
        void shouldDetectLargeOrder() {
            var order = ValidationContext.OrderContext.builder()
                    .orderId("ord-1")
                    .orderValue(BigDecimal.valueOf(100))
                    .itemCount(15)
                    .build();

            assertThat(order.isLargeOrder()).isTrue();
        }

        @Test
        @DisplayName("Should detect online order")
        void shouldDetectOnlineOrder() {
            var order = ValidationContext.OrderContext.builder()
                    .orderId("ord-1")
                    .orderValue(BigDecimal.valueOf(100))
                    .channel("ONLINE")
                    .build();

            assertThat(order.isOnlineOrder()).isTrue();
        }

        @Test
        @DisplayName("Should return false for offline order")
        void shouldReturnFalse_forOfflineOrder() {
            var order = ValidationContext.OrderContext.builder()
                    .orderId("ord-1")
                    .orderValue(BigDecimal.valueOf(100))
                    .channel("STORE")
                    .build();

            assertThat(order.isOnlineOrder()).isFalse();
        }

        @Test
        @DisplayName("Should be valid when orderId and positive orderValue")
        void shouldBeValid() {
            var order = ValidationContext.OrderContext.builder()
                    .orderId("ord-1")
                    .orderValue(BigDecimal.valueOf(100))
                    .build();

            assertThat(order.isValid()).isTrue();
        }

        @Test
        @DisplayName("Should be invalid when orderValue is zero")
        void shouldBeInvalid_whenZeroOrderValue() {
            var order = ValidationContext.OrderContext.builder()
                    .orderId("ord-1")
                    .orderValue(BigDecimal.ZERO)
                    .build();

            assertThat(order.isValid()).isFalse();
        }
    }
}
