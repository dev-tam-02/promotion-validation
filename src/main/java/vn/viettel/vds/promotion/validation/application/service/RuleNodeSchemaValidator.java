package vn.viettel.vds.promotion.validation.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.OperatorOptionEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.OperatorOptionJpaRepository;
import vn.viettel.vds.promotion.validation.domain.exception.InvalidRuleStructureException;
import vn.viettel.vds.promotion.validation.domain.model.ComparatorSuffix;
import vn.viettel.vds.promotion.validation.domain.model.RuleNode;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Validates COND nodes against the operator's params_schema (JSON Schema draft-07)
 * persisted in operator_options. Also enforces that the rule tree contains at least
 * one COND node (a rule with only GROUP wrappers carries no business logic and is
 * rejected as malformed).
 */
@Service
public class RuleNodeSchemaValidator {

    private static final Logger log = LoggerFactory.getLogger(RuleNodeSchemaValidator.class);

    private final OperatorOptionJpaRepository operatorOptionRepo;
    private final ObjectMapper objectMapper;
    private final JsonSchemaFactory schemaFactory;

    public RuleNodeSchemaValidator(OperatorOptionJpaRepository operatorOptionRepo,
                                   ObjectMapper objectMapper) {
        this.operatorOptionRepo = operatorOptionRepo;
        this.objectMapper = objectMapper;
        this.schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
    }

    /**
     * Walk the rule tree, ensure at least one COND node exists, and validate every
     * COND node's params against the operator's params_schema when one is defined.
     *
     * @throws InvalidRuleStructureException on any structural or schema violation
     */
    public void validate(List<RuleNode> rootNodes) {
        if (rootNodes == null || rootNodes.isEmpty()) {
            throw new InvalidRuleStructureException("Rule must have at least one node");
        }

        long condCount = countConds(rootNodes);
        if (condCount == 0) {
            throw new InvalidRuleStructureException(
                    "Rule must contain at least one COND node (a rule made of GROUP wrappers alone carries no logic)");
        }

        for (RuleNode root : rootNodes) {
            validateNode(root);
        }
    }

    private long countConds(List<RuleNode> nodes) {
        long total = 0;
        for (RuleNode n : nodes) {
            if (n.getType() == RuleNode.NodeType.COND) {
                total++;
            }
            if (n.getChildren() != null && !n.getChildren().isEmpty()) {
                total += countConds(n.getChildren());
            }
        }
        return total;
    }

    private void validateNode(RuleNode node) {
        if (node.getType() == RuleNode.NodeType.COND) {
            validateCond(node);
        }
        if (node.getChildren() != null) {
            for (RuleNode child : node.getChildren()) {
                validateNode(child);
            }
        }
    }

    private void validateCond(RuleNode cond) {
        String operatorName = cond.getOperatorName();
        if (operatorName == null || operatorName.isBlank()) {
            throw new InvalidRuleStructureException(cond.getId(), "Operator name is required for COND nodes");
        }

        String canonical = stripComparatorSuffix(operatorName);

        Optional<OperatorOptionEntity> optionOpt = operatorOptionRepo.findByOperatorName(canonical);
        if (optionOpt.isEmpty()) {
            throw new InvalidRuleStructureException(
                    cond.getId(),
                    "Unknown operator: '" + operatorName + "' (canonical='" + canonical + "' not found in operator_options)");
        }

        OperatorOptionEntity option = optionOpt.get();
        String schemaJson = option.getParamsSchema();
        if (schemaJson == null || schemaJson.isBlank()) {
            log.debug("No params_schema for operator={}, skipping schema validation", canonical);
            return;
        }

        JsonSchema schema;
        try {
            JsonNode schemaNode = objectMapper.readTree(schemaJson);
            schema = schemaFactory.getSchema(schemaNode);
        } catch (Exception e) {
            log.error("Malformed params_schema for operator={}", canonical, e);
            throw new InvalidRuleStructureException(
                    cond.getId(),
                    "Operator '" + canonical + "' has a malformed params_schema in the database (admin must fix)");
        }

        JsonNode paramsNode = objectMapper.valueToTree(cond.getParams() != null ? cond.getParams() : java.util.Map.of());
        Set<ValidationMessage> errors = schema.validate(paramsNode);
        if (!errors.isEmpty()) {
            String msg = errors.stream()
                    .map(ValidationMessage::getMessage)
                    .reduce((a, b) -> a + "; " + b)
                    .orElse("schema validation failed");
            throw new InvalidRuleStructureException(
                    cond.getId(),
                    "params do not match schema for operator '" + operatorName + "': " + msg);
        }
    }

    private String stripComparatorSuffix(String operatorName) {
        return ComparatorSuffix.comparatorFromOperatorName(operatorName)
                .map(c -> operatorName.substring(0, operatorName.lastIndexOf('.')))
                .orElse(operatorName);
    }
}
