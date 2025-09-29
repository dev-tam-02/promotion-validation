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

@ChangeUnit(id = "002-create-temporal-policies", order = "002", author = "system")
public class ChangeLogs002TemporalPolicies {

    @Execution
    public void createTemporalPolicies(MongoTemplate mongoTemplate) {
        List<Map<String, Object>> policies = new ArrayList<>();

        // Weekend Policy
        Map<String, Object> weekendPolicy = new HashMap<>();
        weekendPolicy.put("_id", IdGenerator.generateId());
        weekendPolicy.put("tenantId", "DEFAULT");
        weekendPolicy.put("name", "Weekend Policy");
        weekendPolicy.put("tz", "Asia/Ho_Chi_Minh");
        weekendPolicy.put("startTs", LocalDateTime.now().minusDays(30).toInstant(ZoneOffset.UTC));
        weekendPolicy.put("endTs", LocalDateTime.now().plusDays(365).toInstant(ZoneOffset.UTC));
        weekendPolicy.put("rrule", "FREQ=WEEKLY;BYDAY=SA,SU");

        List<Map<String, Object>> timeWindows1 = new ArrayList<>();
        Map<String, Object> window1 = new HashMap<>();
        window1.put("start", "09:00");
        window1.put("end", "21:00");
        timeWindows1.add(window1);
        weekendPolicy.put("timeOfDayWindows", timeWindows1);

        weekendPolicy.put("metadata", Map.of("description", "Active on weekends 9AM-9PM"));
        weekendPolicy.put("createdAt", LocalDateTime.now().toInstant(ZoneOffset.UTC));
        weekendPolicy.put("updatedAt", LocalDateTime.now().toInstant(ZoneOffset.UTC));
        policies.add(weekendPolicy);

        // Business Hours Policy
        Map<String, Object> businessPolicy = new HashMap<>();
        businessPolicy.put("_id", IdGenerator.generateId());
        businessPolicy.put("tenantId", "DEFAULT");
        businessPolicy.put("name", "Business Hours Policy");
        businessPolicy.put("tz", "Asia/Ho_Chi_Minh");
        businessPolicy.put("startTs", LocalDateTime.now().minusDays(30).toInstant(ZoneOffset.UTC));
        businessPolicy.put("endTs", LocalDateTime.now().plusDays(365).toInstant(ZoneOffset.UTC));
        businessPolicy.put("rrule", "FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR");

        List<Map<String, Object>> timeWindows2 = new ArrayList<>();
        Map<String, Object> window2 = new HashMap<>();
        window2.put("start", "08:00");
        window2.put("end", "18:00");
        timeWindows2.add(window2);
        businessPolicy.put("timeOfDayWindows", timeWindows2);

        businessPolicy.put("metadata", Map.of("description", "Active during business hours Mon-Fri 8AM-6PM"));
        businessPolicy.put("createdAt", LocalDateTime.now().toInstant(ZoneOffset.UTC));
        businessPolicy.put("updatedAt", LocalDateTime.now().toInstant(ZoneOffset.UTC));
        policies.add(businessPolicy);

        // Flash Sale Policy (Limited time)
        Map<String, Object> flashPolicy = new HashMap<>();
        flashPolicy.put("_id", IdGenerator.generateId());
        flashPolicy.put("tenantId", "DEFAULT");
        flashPolicy.put("name", "Flash Sale Policy");
        flashPolicy.put("tz", "Asia/Ho_Chi_Minh");
        flashPolicy.put("startTs", LocalDateTime.now().minusHours(1).toInstant(ZoneOffset.UTC));
        flashPolicy.put("endTs", LocalDateTime.now().plusHours(23).toInstant(ZoneOffset.UTC));
        flashPolicy.put("rrule", "FREQ=DAILY");

        List<Map<String, Object>> timeWindows3 = new ArrayList<>();
        Map<String, Object> window3 = new HashMap<>();
        window3.put("start", "00:00");
        window3.put("end", "23:59");
        timeWindows3.add(window3);
        flashPolicy.put("timeOfDayWindows", timeWindows3);

        flashPolicy.put("metadata", Map.of("description", "24h flash sale window"));
        flashPolicy.put("createdAt", LocalDateTime.now().toInstant(ZoneOffset.UTC));
        flashPolicy.put("updatedAt", LocalDateTime.now().toInstant(ZoneOffset.UTC));
        policies.add(flashPolicy);

        for (Map<String, Object> policy : policies) {
            mongoTemplate.save(policy, "temporal_policies");
        }
    }

    @RollbackExecution
    public void rollback(MongoTemplate mongoTemplate) {
        mongoTemplate.remove(Query.query(Criteria.where("tenantId").is("DEFAULT")), "temporal_policies");
    }
}