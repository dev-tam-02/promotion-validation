package vn.viettel.vds.promotion.validation.infrastructure.migration;

import com.promix.platform.core.util.IdGenerator;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ChangeUnit(id = "001-create-operators", order = "001", author = "system")
public class ChangeLogs001Operators {

    @Execution
    public void createOperators(MongoTemplate mongoTemplate) {
        List<Map<String, Object>> operators = new ArrayList<>();

        // Order Amount Operators
        Map<String, Object> orderAmountGte = new HashMap<>();
        orderAmountGte.put("_id", IdGenerator.generateId());
        orderAmountGte.put("tenantId", "DEFAULT");
        orderAmountGte.put("name", "order.amount.gte");
        orderAmountGte.put("version", 1);
        orderAmountGte.put("context", "order");
        orderAmountGte.put("compilerId", "drools-8.5");
        orderAmountGte.put("status", "ACTIVE");

        Map<String, Object> jsonSchema1 = new HashMap<>();
        jsonSchema1.put("type", "object");
        jsonSchema1.put("properties", Map.of(
            "amount", Map.of("type", "number", "minimum", 0),
            "currency", Map.of("type", "string", "enum", List.of("VND", "USD"))
        ));
        jsonSchema1.put("required", List.of("amount"));
        orderAmountGte.put("jsonSchema", jsonSchema1);
        orderAmountGte.put("createdAt", LocalDateTime.now().toInstant(ZoneOffset.UTC));
        orderAmountGte.put("updatedAt", LocalDateTime.now().toInstant(ZoneOffset.UTC));
        operators.add(orderAmountGte);

        // Customer Segment Operator
        Map<String, Object> customerSegment = new HashMap<>();
        customerSegment.put("_id", IdGenerator.generateId());
        customerSegment.put("tenantId", "DEFAULT");
        customerSegment.put("name", "customer.segment.in");
        customerSegment.put("version", 1);
        customerSegment.put("context", "customer");
        customerSegment.put("compilerId", "drools-8.5");
        customerSegment.put("status", "ACTIVE");

        Map<String, Object> jsonSchema2 = new HashMap<>();
        jsonSchema2.put("type", "object");
        jsonSchema2.put("properties", Map.of(
            "segments", Map.of("type", "array", "items", Map.of("type", "string"))
        ));
        jsonSchema2.put("required", List.of("segments"));
        customerSegment.put("jsonSchema", jsonSchema2);
        customerSegment.put("createdAt", LocalDateTime.now().toInstant(ZoneOffset.UTC));
        customerSegment.put("updatedAt", LocalDateTime.now().toInstant(ZoneOffset.UTC));
        operators.add(customerSegment);

        // Time Window Operator
        Map<String, Object> timeWindow = new HashMap<>();
        timeWindow.put("_id", IdGenerator.generateId());
        timeWindow.put("tenantId", "DEFAULT");
        timeWindow.put("name", "time.window.active");
        timeWindow.put("version", 1);
        timeWindow.put("context", "time");
        timeWindow.put("compilerId", "drools-8.5");
        timeWindow.put("status", "ACTIVE");

        Map<String, Object> jsonSchema3 = new HashMap<>();
        jsonSchema3.put("type", "object");
        jsonSchema3.put("properties", Map.of(
            "policyId", Map.of("type", "string"),
            "tz", Map.of("type", "string")
        ));
        jsonSchema3.put("required", List.of("policyId"));
        timeWindow.put("jsonSchema", jsonSchema3);
        timeWindow.put("createdAt", LocalDateTime.now().toInstant(ZoneOffset.UTC));
        timeWindow.put("updatedAt", LocalDateTime.now().toInstant(ZoneOffset.UTC));
        operators.add(timeWindow);

        // Item Count Operator
        Map<String, Object> itemCount = new HashMap<>();
        itemCount.put("_id", IdGenerator.generateId());
        itemCount.put("tenantId", "DEFAULT");
        itemCount.put("name", "order.items.count.gte");
        itemCount.put("version", 1);
        itemCount.put("context", "order");
        itemCount.put("compilerId", "drools-8.5");
        itemCount.put("status", "ACTIVE");

        Map<String, Object> jsonSchema4 = new HashMap<>();
        jsonSchema4.put("type", "object");
        jsonSchema4.put("properties", Map.of(
            "count", Map.of("type", "integer", "minimum", 1)
        ));
        jsonSchema4.put("required", List.of("count"));
        itemCount.put("jsonSchema", jsonSchema4);
        itemCount.put("createdAt", LocalDateTime.now().toInstant(ZoneOffset.UTC));
        itemCount.put("updatedAt", LocalDateTime.now().toInstant(ZoneOffset.UTC));
        operators.add(itemCount);

        // Customer Loyalty Points Operator
        Map<String, Object> loyaltyPoints = new HashMap<>();
        loyaltyPoints.put("_id", IdGenerator.generateId());
        loyaltyPoints.put("tenantId", "DEFAULT");
        loyaltyPoints.put("name", "customer.loyalty.points.gte");
        loyaltyPoints.put("version", 1);
        loyaltyPoints.put("context", "customer");
        loyaltyPoints.put("compilerId", "drools-8.5");
        loyaltyPoints.put("status", "ACTIVE");

        Map<String, Object> jsonSchema5 = new HashMap<>();
        jsonSchema5.put("type", "object");
        jsonSchema5.put("properties", Map.of(
            "points", Map.of("type", "integer", "minimum", 0)
        ));
        jsonSchema5.put("required", List.of("points"));
        loyaltyPoints.put("jsonSchema", jsonSchema5);
        loyaltyPoints.put("createdAt", LocalDateTime.now().toInstant(ZoneOffset.UTC));
        loyaltyPoints.put("updatedAt", LocalDateTime.now().toInstant(ZoneOffset.UTC));
        operators.add(loyaltyPoints);

        // Channel Operator
        Map<String, Object> channelIn = new HashMap<>();
        channelIn.put("_id", IdGenerator.generateId());
        channelIn.put("tenantId", "DEFAULT");
        channelIn.put("name", "order.channel.in");
        channelIn.put("version", 1);
        channelIn.put("context", "order");
        channelIn.put("compilerId", "drools-8.5");
        channelIn.put("status", "ACTIVE");

        Map<String, Object> jsonSchema6 = new HashMap<>();
        jsonSchema6.put("type", "object");
        jsonSchema6.put("properties", Map.of(
            "channels", Map.of("type", "array", "items", Map.of("type", "string"))
        ));
        jsonSchema6.put("required", List.of("channels"));
        channelIn.put("jsonSchema", jsonSchema6);
        channelIn.put("createdAt", LocalDateTime.now().toInstant(ZoneOffset.UTC));
        channelIn.put("updatedAt", LocalDateTime.now().toInstant(ZoneOffset.UTC));
        operators.add(channelIn);

        // Usage Limit Operator
        Map<String, Object> usageLimit = new HashMap<>();
        usageLimit.put("_id", IdGenerator.generateId());
        usageLimit.put("tenantId", "DEFAULT");
        usageLimit.put("name", "customer.usage.count.lt");
        usageLimit.put("version", 1);
        usageLimit.put("context", "customer");
        usageLimit.put("compilerId", "drools-8.5");
        usageLimit.put("status", "ACTIVE");

        Map<String, Object> jsonSchema7 = new HashMap<>();
        jsonSchema7.put("type", "object");
        jsonSchema7.put("properties", Map.of(
            "maxUsage", Map.of("type", "integer", "minimum", 1),
            "voucherCode", Map.of("type", "string")
        ));
        jsonSchema7.put("required", List.of("maxUsage", "voucherCode"));
        usageLimit.put("jsonSchema", jsonSchema7);
        usageLimit.put("createdAt", LocalDateTime.now().toInstant(ZoneOffset.UTC));
        usageLimit.put("updatedAt", LocalDateTime.now().toInstant(ZoneOffset.UTC));
        operators.add(usageLimit);

        for (Map<String, Object> operator : operators) {
            mongoTemplate.save(operator, "operators");
        }
    }

    @RollbackExecution
    public void rollback(MongoTemplate mongoTemplate) {
        mongoTemplate.remove(Query.query(Criteria.where("tenantId").is("DEFAULT")), "operators");
        mongoTemplate.remove(Query.query(Criteria.where("name").in(
            "order.items.count.gte", "customer.loyalty.points.gte",
            "order.channel.in", "customer.usage.count.lt"
        )), "operators");
    }
}