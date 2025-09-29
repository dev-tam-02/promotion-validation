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

@ChangeUnit(id = "005-create-assignments", order = "005", author = "system")
public class ChangeLogs005Assignments {

    @Execution
    public void createAssignments(MongoTemplate mongoTemplate) {
        List<Map<String, Object>> assignments = new ArrayList<>();

        // VIP Weekend Rule Assignment to SAVE20 voucher
        Map<String, Object> assignment1 = new HashMap<>();
        assignment1.put("_id", IdGenerator.generateId());
        assignment1.put("tenantId", "DEFAULT");
        assignment1.put("ruleId", "vip_weekend_rule");
        assignment1.put("ruleVersionPinned", null); // Always use latest

        Map<String, Object> subject1 = new HashMap<>();
        subject1.put("type", "voucher");
        subject1.put("key", "SAVE20");
        assignment1.put("subject", subject1);

        assignment1.put("assignmentVersion", 1);
        assignment1.put("active", true);
        assignment1.put("validFrom", LocalDateTime.now().minusDays(7).toInstant(ZoneOffset.UTC));
        assignment1.put("validTo", LocalDateTime.now().plusDays(30).toInstant(ZoneOffset.UTC));
        assignment1.put("trafficPercent", 100);
        assignment1.put("stickyKeyStrategy", "CUSTOMER_ID");
        assignment1.put("createdAt", LocalDateTime.now().toInstant(ZoneOffset.UTC));
        assignment1.put("updatedAt", LocalDateTime.now().toInstant(ZoneOffset.UTC));
        assignments.add(assignment1);

        // Welcome Rule Assignment to WELCOME10 voucher
        Map<String, Object> assignment2 = new HashMap<>();
        assignment2.put("_id", IdGenerator.generateId());
        assignment2.put("tenantId", "DEFAULT");
        assignment2.put("ruleId", "welcome_new_customer_rule");
        assignment2.put("ruleVersionPinned", 1); // Pinned to version 1

        Map<String, Object> subject2 = new HashMap<>();
        subject2.put("type", "voucher");
        subject2.put("key", "WELCOME10");
        assignment2.put("subject", subject2);

        assignment2.put("assignmentVersion", 1);
        assignment2.put("active", true);
        assignment2.put("validFrom", LocalDateTime.now().minusDays(30).toInstant(ZoneOffset.UTC));
        assignment2.put("validTo", LocalDateTime.now().plusDays(60).toInstant(ZoneOffset.UTC));
        assignment2.put("trafficPercent", 100);
        assignment2.put("stickyKeyStrategy", "CUSTOMER_ID");
        assignment2.put("createdAt", LocalDateTime.now().toInstant(ZoneOffset.UTC));
        assignment2.put("updatedAt", LocalDateTime.now().toInstant(ZoneOffset.UTC));
        assignments.add(assignment2);

        // Flash Sale Assignment to FLASH50 campaign (Canary rollout)
        Map<String, Object> assignment3 = new HashMap<>();
        assignment3.put("_id", IdGenerator.generateId());
        assignment3.put("tenantId", "DEFAULT");
        assignment3.put("ruleId", "flash_sale_business_hours_rule");
        assignment3.put("ruleVersionPinned", null);

        Map<String, Object> subject3 = new HashMap<>();
        subject3.put("type", "campaign");
        subject3.put("key", "FLASH50");
        assignment3.put("subject", subject3);

        assignment3.put("assignmentVersion", 2);
        assignment3.put("active", false); // Not active yet (draft rule)
        assignment3.put("validFrom", LocalDateTime.now().toInstant(ZoneOffset.UTC));
        assignment3.put("validTo", LocalDateTime.now().plusDays(1).toInstant(ZoneOffset.UTC));
        assignment3.put("trafficPercent", 10); // Canary rollout
        assignment3.put("stickyKeyStrategy", "ORDER_ID");
        assignment3.put("createdAt", LocalDateTime.now().toInstant(ZoneOffset.UTC));
        assignment3.put("updatedAt", LocalDateTime.now().toInstant(ZoneOffset.UTC));
        assignments.add(assignment3);

        for (Map<String, Object> assignment : assignments) {
            mongoTemplate.save(assignment, "assignments");
        }
    }

    @RollbackExecution
    public void rollback(MongoTemplate mongoTemplate) {
        mongoTemplate.remove(Query.query(Criteria.where("tenantId").is("DEFAULT")), "assignments");
    }
}