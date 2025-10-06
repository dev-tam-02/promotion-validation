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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ChangeUnit(id = "011-product-applicability-operator", order = "011", author = "system")
public class ChangeLogs011ProductApplicabilityOperator {

    private static final Logger logger = LoggerFactory.getLogger(ChangeLogs011ProductApplicabilityOperator.class);

    @Execution
    public void createProductApplicabilityOperator(MongoTemplate mongoTemplate) {
        logger.info("Creating product.applicability.in operator");

        Map<String, Object> operator = new HashMap<>();
        operator.put("_id", IdGenerator.generateId());
        operator.put("tenantId", "DEFAULT");
        operator.put("name", "product.applicability.in");
        operator.put("version", 1);
        operator.put("context", "order");
        operator.put("status", "ACTIVE");

        // JSON Schema for params validation
        Map<String, Object> jsonSchema = new HashMap<>();
        jsonSchema.put("type", "object");

        Map<String, Object> properties = new HashMap<>();

        // included property schema
        Map<String, Object> includedSchema = new HashMap<>();
        includedSchema.put("type", "array");
        Map<String, Object> includedItems = new HashMap<>();
        includedItems.put("type", "object");
        Map<String, Object> includedProps = new HashMap<>();
        includedProps.put("object", Map.of(
                "type", "string",
                "enum", List.of("PRODUCT", "COLLECTION", "SKU"),
                "description", "Type of object to match"
        ));
        includedProps.put("id", Map.of(
                "type", "string",
                "description", "ID of the object"
        ));
        includedProps.put("effect", Map.of(
                "type", "string",
                "enum", List.of("APPLY_TO_EVERY", "APPLY_TO_CHEAPEST", "APPLY_TO_MOST_EXPENSIVE"),
                "default", "APPLY_TO_EVERY",
                "description", "How to apply the rule to matching items"
        ));
        includedProps.put("target", Map.of(
                "type", "string",
                "enum", List.of("ITEM", "ORDER", "CUSTOMER"),
                "default", "ITEM",
                "description", "Target level for rule application"
        ));
        includedProps.put("skipInitially", Map.of(
                "type", "integer",
                "default", 0,
                "description", "Number of items to skip initially"
        ));
        includedProps.put("repeat", Map.of(
                "type", "integer",
                "default", 1,
                "description", "Apply to every Nth item"
        ));
        includedItems.put("properties", includedProps);
        includedItems.put("required", List.of("object", "id"));
        includedSchema.put("items", includedItems);
        properties.put("included", includedSchema);

        // excluded property schema
        Map<String, Object> excludedSchema = new HashMap<>();
        excludedSchema.put("type", "array");
        Map<String, Object> excludedItems = new HashMap<>();
        excludedItems.put("type", "object");
        Map<String, Object> excludedProps = new HashMap<>();
        excludedProps.put("object", Map.of("type", "string"));
        excludedProps.put("id", Map.of("type", "string"));
        excludedItems.put("properties", excludedProps);
        excludedItems.put("required", List.of("object", "id"));
        excludedSchema.put("items", excludedItems);
        properties.put("excluded", excludedSchema);

        // includedAll property schema
        properties.put("includedAll", Map.of(
                "type", "boolean",
                "default", false,
                "description", "Whether to apply rule to all items (true) or only included items (false)"
        ));

        jsonSchema.put("properties", properties);
        jsonSchema.put("description", "Check if order items match product applicability criteria");

        operator.put("jsonSchema", jsonSchema);
        operator.put("createdAt", Instant.now());
        operator.put("updatedAt", Instant.now());

        mongoTemplate.save(operator, "operators");

        logger.info("Successfully created product.applicability.in operator");
    }

    @RollbackExecution
    public void rollback(MongoTemplate mongoTemplate) {
        logger.info("Rolling back product.applicability.in operator");
        mongoTemplate.remove(
                Query.query(Criteria.where("name").is("product.applicability.in")),
                "operators"
        );
        logger.info("Rollback completed");
    }
}