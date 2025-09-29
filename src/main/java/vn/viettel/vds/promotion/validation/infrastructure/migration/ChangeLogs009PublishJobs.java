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

@ChangeUnit(id = "009-create-publish-jobs", order = "009", author = "system")
public class ChangeLogs009PublishJobs {

    @Execution
    public void createPublishJobs(MongoTemplate mongoTemplate) {
        List<Map<String, Object>> publishJobs = new ArrayList<>();

        // Successful Publish Job for VIP Weekend Rule
        Map<String, Object> job1 = new HashMap<>();
        job1.put("_id", IdGenerator.generateId());
        job1.put("tenantId", "DEFAULT");
        job1.put("ruleId", "vip_weekend_rule");
        job1.put("targetVersion", 1);
        job1.put("status", "SUCCESS");
        job1.put("startedAt", LocalDateTime.now().minusDays(7).toInstant(ZoneOffset.UTC));
        job1.put("completedAt", LocalDateTime.now().minusDays(7).plusMinutes(2).toInstant(ZoneOffset.UTC));
        job1.put("logs", List.of(
            "[INFO] Starting publish for rule: vip_weekend_rule",
            "[INFO] Freezing rule at version 1",
            "[INFO] Resolving time policy links: weekend_policy",
            "[INFO] Computing operators fingerprint: sha256:abc123def456",
            "[INFO] Calling Artifact Service for compilation",
            "[INFO] Bundle compilation successful: sha256:bundle_vip_weekend_v1_xyz",
            "[INFO] Publishing outbox event: RulePublished",
            "[SUCCESS] Rule published successfully"
        ));
        job1.put("bundleHash", "sha256:bundle_vip_weekend_v1_xyz");
        job1.put("publishedBy", "admin");
        publishJobs.add(job1);

        // Successful Publish Job for Welcome New Customer Rule
        Map<String, Object> job2 = new HashMap<>();
        job2.put("_id", IdGenerator.generateId());
        job2.put("tenantId", "DEFAULT");
        job2.put("ruleId", "welcome_new_customer_rule");
        job2.put("targetVersion", 1);
        job2.put("status", "SUCCESS");
        job2.put("startedAt", LocalDateTime.now().minusDays(30).toInstant(ZoneOffset.UTC));
        job2.put("completedAt", LocalDateTime.now().minusDays(30).plusMinutes(1).toInstant(ZoneOffset.UTC));
        job2.put("logs", List.of(
            "[INFO] Starting publish for rule: welcome_new_customer_rule",
            "[INFO] Freezing rule at version 1",
            "[INFO] No time policy links to resolve",
            "[INFO] Computing operators fingerprint: sha256:abc123def456",
            "[INFO] Calling Artifact Service for compilation",
            "[INFO] Bundle compilation successful: sha256:bundle_welcome_v1_abc",
            "[INFO] Publishing outbox event: RulePublished",
            "[SUCCESS] Rule published successfully"
        ));
        job2.put("bundleHash", "sha256:bundle_welcome_v1_abc");
        job2.put("publishedBy", "admin");
        publishJobs.add(job2);

        // Failed Publish Job for Flash Sale Rule
        Map<String, Object> job3 = new HashMap<>();
        job3.put("_id", IdGenerator.generateId());
        job3.put("tenantId", "DEFAULT");
        job3.put("ruleId", "flash_sale_business_hours_rule");
        job3.put("targetVersion", 1);
        job3.put("status", "FAILED");
        job3.put("startedAt", LocalDateTime.now().minusHours(2).toInstant(ZoneOffset.UTC));
        job3.put("completedAt", LocalDateTime.now().minusHours(2).plusMinutes(1).toInstant(ZoneOffset.UTC));
        job3.put("logs", List.of(
            "[INFO] Starting publish for rule: flash_sale_business_hours_rule",
            "[INFO] Freezing rule at version 1",
            "[INFO] Resolving time policy links: business_hours_policy",
            "[ERROR] Time policy 'business_hours_policy' validation failed",
            "[ERROR] RRULE validation error: invalid BYDAY value",
            "[FAILED] Publish failed due to time policy validation errors"
        ));
        job3.put("bundleHash", null);
        job3.put("publishedBy", "editor");
        publishJobs.add(job3);

        for (Map<String, Object> job : publishJobs) {
            mongoTemplate.save(job, "publish_jobs");
        }
    }

    @RollbackExecution
    public void rollback(MongoTemplate mongoTemplate) {
        mongoTemplate.remove(Query.query(Criteria.where("tenantId").is("DEFAULT")), "publish_jobs");
    }
}