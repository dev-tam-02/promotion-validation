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

@ChangeUnit(id = "004-create-rules", order = "004", author = "system")
public class ChangeLogs004Rules {

    @Execution
    public void createRules(MongoTemplate mongoTemplate) {
        List<Map<String, Object>> rules = new ArrayList<>();

        // VIP Weekend Rule
        Map<String, Object> vipWeekendRule = new HashMap<>();
        vipWeekendRule.put("_id", IdGenerator.generateId());
        vipWeekendRule.put("tenantId", "DEFAULT");
        vipWeekendRule.put("code", "VIP_WEEKEND_500K");
        vipWeekendRule.put("name", "VIP Weekend 500K Rule");
        vipWeekendRule.put("state", "PUBLISHED");
        vipWeekendRule.put("latestVersion", 1);
        vipWeekendRule.put("logic", "ALL");

        Map<String, Object> limits1 = new HashMap<>();
        limits1.put("perCodeTotal", 1000);
        limits1.put("perCustomer", 1);
        limits1.put("perDay", 100);
        vipWeekendRule.put("limits", limits1);

        List<Map<String, Object>> nodes1 = new ArrayList<>();
        Map<String, Object> groupNode1 = new HashMap<>();
        groupNode1.put("id", "n1");
        groupNode1.put("type", "GROUP");
        groupNode1.put("groupLogic", "ALL");

        List<Map<String, Object>> children1 = new ArrayList<>();
        Map<String, Object> condNode1 = new HashMap<>();
        condNode1.put("id", "n2");
        condNode1.put("type", "COND");
        condNode1.put("operatorName", "customer.segment.in");
        condNode1.put("params", Map.of("segments", List.of("VIP")));
        condNode1.put("reasonCode", "CUSTOMER_SEGMENT_VIP");
        children1.add(condNode1);

        Map<String, Object> condNode2 = new HashMap<>();
        condNode2.put("id", "n3");
        condNode2.put("type", "COND");
        condNode2.put("operatorName", "order.amount.gte");
        condNode2.put("params", Map.of("amount", 500000, "currency", "VND"));
        condNode2.put("reasonCode", "ORDER_AMOUNT_MIN");
        children1.add(condNode2);

        Map<String, Object> condNode3 = new HashMap<>();
        condNode3.put("id", "n4");
        condNode3.put("type", "COND");
        condNode3.put("operatorName", "time.window.active");
        condNode3.put("params", Map.of("policyId", "weekend_policy", "tz", "Asia/Ho_Chi_Minh"));
        condNode3.put("reasonCode", "TIME_WINDOW_WEEKEND");
        children1.add(condNode3);

        groupNode1.put("children", children1);
        nodes1.add(groupNode1);
        vipWeekendRule.put("nodes", nodes1);

        vipWeekendRule.put("notes", "VIP customers weekend promotion with 500K minimum order");
        vipWeekendRule.put("createdAt", LocalDateTime.now().toInstant(ZoneOffset.UTC));
        vipWeekendRule.put("createdBy", "system");
        vipWeekendRule.put("updatedAt", LocalDateTime.now().toInstant(ZoneOffset.UTC));
        vipWeekendRule.put("updatedBy", "system");
        rules.add(vipWeekendRule);

        // New Customer Welcome Rule
        Map<String, Object> welcomeRule = new HashMap<>();
        welcomeRule.put("_id", IdGenerator.generateId());
        welcomeRule.put("tenantId", "DEFAULT");
        welcomeRule.put("code", "WELCOME_NEW_CUSTOMER");
        welcomeRule.put("name", "Welcome New Customer Rule");
        welcomeRule.put("state", "PUBLISHED");
        welcomeRule.put("latestVersion", 1);
        welcomeRule.put("logic", "ALL");

        Map<String, Object> limits2 = new HashMap<>();
        limits2.put("perCodeTotal", 5000);
        limits2.put("perCustomer", 1);
        limits2.put("perDay", 500);
        welcomeRule.put("limits", limits2);

        List<Map<String, Object>> nodes2 = new ArrayList<>();
        Map<String, Object> groupNode2 = new HashMap<>();
        groupNode2.put("id", "n1");
        groupNode2.put("type", "GROUP");
        groupNode2.put("groupLogic", "ALL");

        List<Map<String, Object>> children2 = new ArrayList<>();
        Map<String, Object> condNode4 = new HashMap<>();
        condNode4.put("id", "n2");
        condNode4.put("type", "COND");
        condNode4.put("operatorName", "customer.segment.in");
        condNode4.put("params", Map.of("segments", List.of("NEW")));
        condNode4.put("reasonCode", "CUSTOMER_SEGMENT_NEW");
        children2.add(condNode4);

        Map<String, Object> condNode5 = new HashMap<>();
        condNode5.put("id", "n3");
        condNode5.put("type", "COND");
        condNode5.put("operatorName", "order.amount.gte");
        condNode5.put("params", Map.of("amount", 100000, "currency", "VND"));
        condNode5.put("reasonCode", "ORDER_AMOUNT_MIN");
        children2.add(condNode5);

        groupNode2.put("children", children2);
        nodes2.add(groupNode2);
        welcomeRule.put("nodes", nodes2);

        welcomeRule.put("notes", "Welcome promotion for new customers with 100K minimum order");
        welcomeRule.put("createdAt", LocalDateTime.now().toInstant(ZoneOffset.UTC));
        welcomeRule.put("createdBy", "system");
        welcomeRule.put("updatedAt", LocalDateTime.now().toInstant(ZoneOffset.UTC));
        welcomeRule.put("updatedBy", "system");
        rules.add(welcomeRule);

        // Business Hours Flash Sale Rule
        Map<String, Object> flashRule = new HashMap<>();
        flashRule.put("_id", IdGenerator.generateId());
        flashRule.put("tenantId", "DEFAULT");
        flashRule.put("code", "FLASH_BUSINESS_HOURS");
        flashRule.put("name", "Flash Sale Business Hours Rule");
        flashRule.put("state", "DRAFT");
        flashRule.put("latestVersion", 0);
        flashRule.put("logic", "ALL");

        Map<String, Object> limits3 = new HashMap<>();
        limits3.put("perCodeTotal", 500);
        limits3.put("perCustomer", 2);
        limits3.put("perDay", 100);
        flashRule.put("limits", limits3);

        List<Map<String, Object>> nodes3 = new ArrayList<>();
        Map<String, Object> groupNode3 = new HashMap<>();
        groupNode3.put("id", "n1");
        groupNode3.put("type", "GROUP");
        groupNode3.put("groupLogic", "ALL");

        List<Map<String, Object>> children3 = new ArrayList<>();
        Map<String, Object> condNode6 = new HashMap<>();
        condNode6.put("id", "n2");
        condNode6.put("type", "COND");
        condNode6.put("operatorName", "customer.segment.in");
        condNode6.put("params", Map.of("segments", List.of("STANDARD", "PREMIUM", "VIP")));
        condNode6.put("reasonCode", "CUSTOMER_SEGMENT_ELIGIBLE");
        children3.add(condNode6);

        Map<String, Object> condNode7 = new HashMap<>();
        condNode7.put("id", "n3");
        condNode7.put("type", "COND");
        condNode7.put("operatorName", "order.amount.gte");
        condNode7.put("params", Map.of("amount", 200000, "currency", "VND"));
        condNode7.put("reasonCode", "ORDER_AMOUNT_MIN");
        children3.add(condNode7);

        Map<String, Object> condNode8 = new HashMap<>();
        condNode8.put("id", "n4");
        condNode8.put("type", "COND");
        condNode8.put("operatorName", "time.window.active");
        condNode8.put("params", Map.of("policyId", "business_hours_policy", "tz", "Asia/Ho_Chi_Minh"));
        condNode8.put("reasonCode", "TIME_WINDOW_BUSINESS");
        children3.add(condNode8);

        groupNode3.put("children", children3);
        nodes3.add(groupNode3);
        flashRule.put("nodes", nodes3);

        flashRule.put("notes", "Flash sale during business hours for eligible customers");
        flashRule.put("createdAt", LocalDateTime.now().toInstant(ZoneOffset.UTC));
        flashRule.put("createdBy", "system");
        flashRule.put("updatedAt", LocalDateTime.now().toInstant(ZoneOffset.UTC));
        flashRule.put("updatedBy", "system");
        rules.add(flashRule);

        for (Map<String, Object> rule : rules) {
            mongoTemplate.save(rule, "rules");
        }
    }

    @RollbackExecution
    public void rollback(MongoTemplate mongoTemplate) {
        mongoTemplate.remove(Query.query(Criteria.where("tenantId").is("DEFAULT")), "rules");
    }
}