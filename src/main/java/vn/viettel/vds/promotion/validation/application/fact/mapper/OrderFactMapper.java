package vn.viettel.vds.promotion.validation.application.fact.mapper;

import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.domain.fact.OrderFact;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@Component
public class OrderFactMapper {

    public OrderFact map(Object data) {
        if (data == null) {
            return null;
        }

        if (data instanceof OrderFact) {
            return (OrderFact) data;
        }

        if (data instanceof Map) {
            // Handle case where data comes as Map (from cache or other sources)
            Map<String, Object> mapData = (Map<String, Object>) data;
            return mapFromData(mapData);
        }

        return null;
    }

    private OrderFact mapFromData(Map<String, Object> data) {
        OrderFact.Builder builder = OrderFact.builder();

        if (data.get("orderId") != null) {
            builder.orderId((String) data.get("orderId"));
        }
        if (data.get("customerId") != null) {
            builder.customerId((String) data.get("customerId"));
        }
        if (data.get("status") != null) {
            builder.status((String) data.get("status"));
        }
        if (data.get("totalAmount") != null) {
            builder.totalAmount(new BigDecimal(data.get("totalAmount").toString()));
        }
        if (data.get("currency") != null) {
            builder.currency((String) data.get("currency"));
        }
        if (data.get("createdAt") != null) {
            builder.createdAt(Instant.parse(data.get("createdAt").toString()));
        }
        if (data.get("updatedAt") != null) {
            builder.updatedAt(Instant.parse(data.get("updatedAt").toString()));
        }
        if (data.get("items") != null) {
            builder.items((java.util.List<vn.viettel.vds.promotion.validation.domain.fact.OrderItemFact>) data.get("items"));
        }
        if (data.get("metadata") != null) {
            builder.metadata((Map<String, Object>) data.get("metadata"));
        }

        return builder.build();
    }
}