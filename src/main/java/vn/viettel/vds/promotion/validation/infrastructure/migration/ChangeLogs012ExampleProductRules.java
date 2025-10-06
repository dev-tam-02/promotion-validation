package vn.viettel.vds.promotion.validation.infrastructure.migration;

import com.promix.platform.core.util.IdGenerator;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ChangeUnit(id = "012-example-product-rules", order = "012", author = "system")
public class ChangeLogs012ExampleProductRules {

    private static final Logger logger = LoggerFactory.getLogger(ChangeLogs012ExampleProductRules.class);

    @Execution
    public void createExampleProductRules(MongoTemplate mongoTemplate) {
        logger.info("Creating example validation rules with product.applicability.in operator");

        // Example 1: Include specific products
        createIncludeProductsRule(mongoTemplate);

        // Example 2: Include products with exclude list
        createIncludeWithExcludeRule(mongoTemplate);

        // Example 3: Include all products except specific ones
        createIncludeAllWithExcludeRule(mongoTemplate);

        // Example 4: Include collection with special effect
        createCollectionWithEffectRule(mongoTemplate);

        logger.info("Successfully created example product rules");
    }

    /**
     * Example 1: Rule that applies to specific products
     * Use case: Campaign for specific product IDs only
     */
    private void createIncludeProductsRule(MongoTemplate mongoTemplate) {
        String ruleId = IdGenerator.generateId();

        Map<String, Object> rule = new HashMap<>();
        rule.put("_id", ruleId);
        rule.put("tenantId", "DEFAULT");
        rule.put("name", "Example: Include Specific Products");
        rule.put("description", "Rule applies to PRODUCT-001, PRODUCT-002, PRODUCT-003");
        rule.put("version", 1);
        rule.put("context", "order");
        rule.put("status", "ACTIVE");

        // Root GROUP node with AND logic
        Map<String, Object> rootNode = createGroupNode("AND", Arrays.asList(
                // Product applicability condition
                createProductApplicabilityNode(
                        Arrays.asList(
                                createIncludedItem("PRODUCT", "PRODUCT-001", "APPLY_TO_EVERY", "ITEM", 0, 1),
                                createIncludedItem("PRODUCT", "PRODUCT-002", "APPLY_TO_EVERY", "ITEM", 0, 1),
                                createIncludedItem("PRODUCT", "PRODUCT-003", "APPLY_TO_EVERY", "ITEM", 0, 1)
                        ),
                        Arrays.asList(),
                        false
                ),
                // Additional condition: Order amount >= 100000
                createConditionNode("order.amount.gte", Map.of("value", 100000))
        ));

        rule.put("nodes", List.of(rootNode));
        rule.put("createdAt", Instant.now());
        rule.put("updatedAt", Instant.now());

        mongoTemplate.save(rule, "rules");
        logger.info("Created example rule: Include Specific Products (ruleId={})", ruleId);
    }

    /**
     * Example 2: Include specific products but exclude some
     * Use case: Campaign for product category but exclude sale items
     */
    private void createIncludeWithExcludeRule(MongoTemplate mongoTemplate) {
        String ruleId = IdGenerator.generateId();

        Map<String, Object> rule = new HashMap<>();
        rule.put("_id", ruleId);
        rule.put("tenantId", "DEFAULT");
        rule.put("name", "Example: Include with Exclude List");
        rule.put("description", "Include COLLECTION-001 but exclude PRODUCT-999");
        rule.put("version", 1);
        rule.put("context", "order");
        rule.put("status", "ACTIVE");

        Map<String, Object> rootNode = createGroupNode("AND", Arrays.asList(
                createProductApplicabilityNode(
                        Arrays.asList(
                                createIncludedItem("COLLECTION", "COLLECTION-001", "APPLY_TO_EVERY", "ITEM", 0, 1)
                        ),
                        Arrays.asList(
                                createExcludedItem("PRODUCT", "PRODUCT-999")
                        ),
                        false
                )
        ));

        rule.put("nodes", List.of(rootNode));
        rule.put("createdAt", Instant.now());
        rule.put("updatedAt", Instant.now());

        mongoTemplate.save(rule, "rules");
        logger.info("Created example rule: Include with Exclude List (ruleId={})", ruleId);
    }

    /**
     * Example 3: Include all products except specific ones
     * Use case: Site-wide campaign excluding premium products
     */
    private void createIncludeAllWithExcludeRule(MongoTemplate mongoTemplate) {
        String ruleId = IdGenerator.generateId();

        Map<String, Object> rule = new HashMap<>();
        rule.put("_id", ruleId);
        rule.put("tenantId", "DEFAULT");
        rule.put("name", "Example: Include All Except");
        rule.put("description", "Apply to all products except premium collection");
        rule.put("version", 1);
        rule.put("context", "order");
        rule.put("status", "ACTIVE");

        Map<String, Object> rootNode = createGroupNode("AND", Arrays.asList(
                createProductApplicabilityNode(
                        Arrays.asList(),
                        Arrays.asList(
                                createExcludedItem("COLLECTION", "PREMIUM-COLLECTION")
                        ),
                        true  // includedAll = true
                ),
                // Customer must be in VIP segment
                createConditionNode("customer.segment.in", Map.of("segments", List.of("VIP")))
        ));

        rule.put("nodes", List.of(rootNode));
        rule.put("createdAt", Instant.now());
        rule.put("updatedAt", Instant.now());

        mongoTemplate.save(rule, "rules");
        logger.info("Created example rule: Include All Except (ruleId={})", ruleId);
    }

    /**
     * Example 4: Buy X Get Y - Apply to cheapest item
     * Use case: Buy 3 get 1 free (apply discount to cheapest)
     */
    private void createCollectionWithEffectRule(MongoTemplate mongoTemplate) {
        String ruleId = IdGenerator.generateId();

        Map<String, Object> rule = new HashMap<>();
        rule.put("_id", ruleId);
        rule.put("tenantId", "DEFAULT");
        rule.put("name", "Example: Buy 3 Get Cheapest Free");
        rule.put("description", "Apply discount to cheapest item in collection");
        rule.put("version", 1);
        rule.put("context", "order");
        rule.put("status", "ACTIVE");

        Map<String, Object> rootNode = createGroupNode("AND", Arrays.asList(
                createProductApplicabilityNode(
                        Arrays.asList(
                                // Apply to cheapest item, skip first 2, then repeat every 3rd
                                createIncludedItem("COLLECTION", "ELECTRONICS", "APPLY_TO_CHEAPEST", "ITEM", 2, 3)
                        ),
                        Arrays.asList(),
                        false
                ),
                // Order must have at least 3 items
                createConditionNode("order.item.count.gte", Map.of("value", 3))
        ));

        rule.put("nodes", List.of(rootNode));
        rule.put("createdAt", Instant.now());
        rule.put("updatedAt", Instant.now());

        mongoTemplate.save(rule, "rules");
        logger.info("Created example rule: Buy 3 Get Cheapest Free (ruleId={})", ruleId);
    }

    // ========== Helper Methods ==========

    private Map<String, Object> createGroupNode(String operator, List<Map<String, Object>> children) {
        Map<String, Object> node = new HashMap<>();
        node.put("type", "GROUP");
        node.put("operator", operator);
        node.put("children", children);
        return node;
    }

    private Map<String, Object> createConditionNode(String operatorName, Map<String, Object> params) {
        Map<String, Object> node = new HashMap<>();
        node.put("type", "COND");
        node.put("operatorName", operatorName);
        node.put("params", params);
        return node;
    }

    private Map<String, Object> createProductApplicabilityNode(
            List<Map<String, Object>> included,
            List<Map<String, Object>> excluded,
            boolean includedAll) {

        Map<String, Object> params = new HashMap<>();
        params.put("included", included);
        params.put("excluded", excluded);
        params.put("includedAll", includedAll);

        return createConditionNode("product.applicability.in", params);
    }

    private Map<String, Object> createIncludedItem(
            String object,
            String id,
            String effect,
            String target,
            int skipInitially,
            int repeat) {

        Map<String, Object> item = new HashMap<>();
        item.put("object", object);
        item.put("id", id);
        item.put("effect", effect);
        item.put("target", target);
        item.put("skipInitially", skipInitially);
        item.put("repeat", repeat);
        return item;
    }

    private Map<String, Object> createExcludedItem(String object, String id) {
        Map<String, Object> item = new HashMap<>();
        item.put("object", object);
        item.put("id", id);
        return item;
    }

    @RollbackExecution
    public void rollback(MongoTemplate mongoTemplate) {
        logger.info("Rolling back example product rules");
        mongoTemplate.remove(
                Query.query(Criteria.where("name").regex("^Example:")),
                "rules"
        );
        logger.info("Rollback completed");
    }
}