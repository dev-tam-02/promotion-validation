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

@ChangeUnit(id = "007-create-rule-temporal-links", order = "007", author = "system")
public class ChangeLogs007RuleTemporalLinks {

    @Execution
    public void createRuleTemporalLinks(MongoTemplate mongoTemplate) {
        List<Map<String, Object>> temporalLinks = new ArrayList<>();

        // VIP Weekend Rule <-> Weekend Policy Link
        Map<String, Object> link1 = new HashMap<>();
        link1.put("_id", IdGenerator.generateId());
        link1.put("tenantId", "DEFAULT");
        link1.put("ruleId", "vip_weekend_rule");
        link1.put("policyId", "weekend_policy");
        link1.put("mode", "ALLOW");
        link1.put("active", true);
        link1.put("createdAt", LocalDateTime.now().minusDays(7).toInstant(ZoneOffset.UTC));
        link1.put("updatedAt", LocalDateTime.now().minusDays(7).toInstant(ZoneOffset.UTC));
        temporalLinks.add(link1);

        // Flash Sale Rule <-> Business Hours Policy Link
        Map<String, Object> link2 = new HashMap<>();
        link2.put("_id", IdGenerator.generateId());
        link2.put("tenantId", "DEFAULT");
        link2.put("ruleId", "flash_sale_business_hours_rule");
        link2.put("policyId", "business_hours_policy");
        link2.put("mode", "ALLOW");
        link2.put("active", true);
        link2.put("createdAt", LocalDateTime.now().toInstant(ZoneOffset.UTC));
        link2.put("updatedAt", LocalDateTime.now().toInstant(ZoneOffset.UTC));
        temporalLinks.add(link2);

        // Flash Sale Rule <-> Flash Sale Policy Link (overlapping time windows)
        Map<String, Object> link3 = new HashMap<>();
        link3.put("_id", IdGenerator.generateId());
        link3.put("tenantId", "DEFAULT");
        link3.put("ruleId", "flash_sale_business_hours_rule");
        link3.put("policyId", "flash_sale_policy");
        link3.put("mode", "DENY");
        link3.put("active", false); // Disabled for now
        link3.put("createdAt", LocalDateTime.now().toInstant(ZoneOffset.UTC));
        link3.put("updatedAt", LocalDateTime.now().toInstant(ZoneOffset.UTC));
        temporalLinks.add(link3);

        for (Map<String, Object> link : temporalLinks) {
            mongoTemplate.save(link, "rule_temporal_links");
        }
    }

    @RollbackExecution
    public void rollback(MongoTemplate mongoTemplate) {
        mongoTemplate.remove(Query.query(Criteria.where("tenantId").is("DEFAULT")), "rule_temporal_links");
    }
}