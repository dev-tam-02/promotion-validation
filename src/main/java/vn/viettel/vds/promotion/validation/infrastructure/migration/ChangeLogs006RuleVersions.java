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

@ChangeUnit(id = "006-create-rule-versions", order = "006", author = "system")
public class ChangeLogs006RuleVersions {

    @Execution
    public void createRuleVersions(MongoTemplate mongoTemplate) {
        List<Map<String, Object>> ruleVersions = new ArrayList<>();

        // VIP Weekend Rule Version 1
        Map<String, Object> vipVersion1 = new HashMap<>();
        vipVersion1.put("_id", IdGenerator.generateId());
        vipVersion1.put("tenantId", "DEFAULT");
        vipVersion1.put("ruleId", "vip_weekend_rule");
        vipVersion1.put("code", "VIP_WEEKEND_500K");
        vipVersion1.put("version", 1);
        vipVersion1.put("logic", "ALL");

        Map<String, Object> limits1 = new HashMap<>();
        limits1.put("perCodeTotal", 1000);
        limits1.put("perCustomer", 1);
        limits1.put("perDay", 100);
        vipVersion1.put("limits", limits1);

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
        vipVersion1.put("nodes", nodes1);

        vipVersion1.put("operatorsFingerprint", "sha256:abc123def456");

        List<Map<String, Object>> timeLinks1 = new ArrayList<>();
        Map<String, Object> timeLink1 = new HashMap<>();
        timeLink1.put("policyId", "weekend_policy");
        timeLink1.put("mode", "ALLOW");
        timeLinks1.add(timeLink1);
        vipVersion1.put("timeLinks", timeLinks1);

        vipVersion1.put("publishedAt", LocalDateTime.now().minusDays(7).toInstant(ZoneOffset.UTC));
        vipVersion1.put("publishedBy", "admin");

        Map<String, Object> compile1 = new HashMap<>();
        compile1.put("status", "SUCCESS");
        compile1.put("compilerId", "drools-8.5");
        compile1.put("bundleHash", "sha256:bundle_vip_weekend_v1_xyz");
        compile1.put("logs", List.of("Compilation successful", "3 operators resolved", "1 time policy linked"));
        vipVersion1.put("compile", compile1);

        ruleVersions.add(vipVersion1);

        // Welcome Customer Rule Version 1
        Map<String, Object> welcomeVersion1 = new HashMap<>();
        welcomeVersion1.put("_id", IdGenerator.generateId());
        welcomeVersion1.put("tenantId", "DEFAULT");
        welcomeVersion1.put("ruleId", "welcome_new_customer_rule");
        welcomeVersion1.put("code", "WELCOME_NEW_CUSTOMER");
        welcomeVersion1.put("version", 1);
        welcomeVersion1.put("logic", "ALL");

        Map<String, Object> limits2 = new HashMap<>();
        limits2.put("perCodeTotal", 5000);
        limits2.put("perCustomer", 1);
        limits2.put("perDay", 500);
        welcomeVersion1.put("limits", limits2);

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
        welcomeVersion1.put("nodes", nodes2);

        welcomeVersion1.put("operatorsFingerprint", "sha256:abc123def456");
        welcomeVersion1.put("timeLinks", new ArrayList<>());
        welcomeVersion1.put("publishedAt", LocalDateTime.now().minusDays(30).toInstant(ZoneOffset.UTC));
        welcomeVersion1.put("publishedBy", "admin");

        Map<String, Object> compile2 = new HashMap<>();
        compile2.put("status", "SUCCESS");
        compile2.put("compilerId", "drools-8.5");
        compile2.put("bundleHash", "sha256:bundle_welcome_v1_abc");
        compile2.put("logs", List.of("Compilation successful", "2 operators resolved", "No time policies"));
        welcomeVersion1.put("compile", compile2);

        ruleVersions.add(welcomeVersion1);

        for (Map<String, Object> ruleVersion : ruleVersions) {
            mongoTemplate.save(ruleVersion, "rule_versions");
        }
    }

    @RollbackExecution
    public void rollback(MongoTemplate mongoTemplate) {
        mongoTemplate.remove(Query.query(Criteria.where("tenantId").is("DEFAULT")), "rule_versions");
    }
}