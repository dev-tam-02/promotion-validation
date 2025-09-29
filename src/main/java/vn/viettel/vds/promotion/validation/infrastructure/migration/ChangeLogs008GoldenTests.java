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

@ChangeUnit(id = "008-create-golden-tests", order = "008", author = "system")
public class ChangeLogs008GoldenTests {

    @Execution
    public void createGoldenTests(MongoTemplate mongoTemplate) {
        List<Map<String, Object>> goldenTests = new ArrayList<>();

        // Golden Test for VIP Weekend Rule
        Map<String, Object> test1 = new HashMap<>();
        test1.put("_id", IdGenerator.generateId());
        test1.put("tenantId", "DEFAULT");
        test1.put("ruleId", "vip_weekend_rule");
        test1.put("version", 1);
        test1.put("name", "VIP Weekend - Pass Case");
        test1.put("description", "VIP customer with 600K order on Saturday");

        Map<String, Object> input1 = new HashMap<>();
        input1.put("now", "2025-01-25T14:00:00Z"); // Saturday 2PM
        input1.put("tz", "Asia/Ho_Chi_Minh");
        Map<String, Object> order1 = new HashMap<>();
        order1.put("total", 600000);
        order1.put("currency", "VND");
        input1.put("order", order1);
        Map<String, Object> customer1 = new HashMap<>();
        customer1.put("id", "CUST_VIP_001");
        customer1.put("segments", List.of("VIP"));
        input1.put("customer", customer1);
        test1.put("input", input1);

        Map<String, Object> expected1 = new HashMap<>();
        expected1.put("decision", "ALLOW");
        expected1.put("reasonCodes", List.of("CUSTOMER_SEGMENT_VIP", "ORDER_AMOUNT_MIN", "TIME_WINDOW_WEEKEND"));
        test1.put("expected", expected1);

        test1.put("active", true);
        test1.put("createdAt", LocalDateTime.now().toInstant(ZoneOffset.UTC));
        goldenTests.add(test1);

        // Golden Test for VIP Weekend Rule - Fail Case
        Map<String, Object> test2 = new HashMap<>();
        test2.put("_id", IdGenerator.generateId());
        test2.put("tenantId", "DEFAULT");
        test2.put("ruleId", "vip_weekend_rule");
        test2.put("version", 1);
        test2.put("name", "VIP Weekend - Fail Case (Weekday)");
        test2.put("description", "VIP customer with 600K order on Monday (not weekend)");

        Map<String, Object> input2 = new HashMap<>();
        input2.put("now", "2025-01-27T14:00:00Z"); // Monday 2PM
        input2.put("tz", "Asia/Ho_Chi_Minh");
        Map<String, Object> order2 = new HashMap<>();
        order2.put("total", 600000);
        order2.put("currency", "VND");
        input2.put("order", order2);
        Map<String, Object> customer2 = new HashMap<>();
        customer2.put("id", "CUST_VIP_002");
        customer2.put("segments", List.of("VIP"));
        input2.put("customer", customer2);
        test2.put("input", input2);

        Map<String, Object> expected2 = new HashMap<>();
        expected2.put("decision", "DENY");
        expected2.put("reasonCodes", List.of("TIME_WINDOW_WEEKEND"));
        test2.put("expected", expected2);

        test2.put("active", true);
        test2.put("createdAt", LocalDateTime.now().toInstant(ZoneOffset.UTC));
        goldenTests.add(test2);

        // Golden Test for Welcome New Customer Rule
        Map<String, Object> test3 = new HashMap<>();
        test3.put("_id", IdGenerator.generateId());
        test3.put("tenantId", "DEFAULT");
        test3.put("ruleId", "welcome_new_customer_rule");
        test3.put("version", 1);
        test3.put("name", "Welcome New Customer - Pass Case");
        test3.put("description", "New customer with 150K order");

        Map<String, Object> input3 = new HashMap<>();
        Map<String, Object> order3 = new HashMap<>();
        order3.put("total", 150000);
        order3.put("currency", "VND");
        input3.put("order", order3);
        Map<String, Object> customer3 = new HashMap<>();
        customer3.put("id", "CUST_NEW_001");
        customer3.put("segments", List.of("NEW"));
        input3.put("customer", customer3);
        test3.put("input", input3);

        Map<String, Object> expected3 = new HashMap<>();
        expected3.put("decision", "ALLOW");
        expected3.put("reasonCodes", List.of("CUSTOMER_SEGMENT_NEW", "ORDER_AMOUNT_MIN"));
        test3.put("expected", expected3);

        test3.put("active", true);
        test3.put("createdAt", LocalDateTime.now().toInstant(ZoneOffset.UTC));
        goldenTests.add(test3);

        for (Map<String, Object> test : goldenTests) {
            mongoTemplate.save(test, "golden_tests");
        }
    }

    @RollbackExecution
    public void rollback(MongoTemplate mongoTemplate) {
        mongoTemplate.remove(Query.query(Criteria.where("tenantId").is("DEFAULT")), "golden_tests");
    }
}