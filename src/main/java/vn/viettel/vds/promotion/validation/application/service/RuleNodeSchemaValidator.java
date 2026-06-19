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

    /**
     * Virtual engine operator backing every metadata-category condition. Rows for
     * it are never persisted in operator_options — the FE rule builder synthesizes
     * one rule per pp-metadata schema field at request time (see
     * {@code RuleBuilderService.synthesizeMetadataRule}). So it must bypass the
     * operator_options lookup and be validated by its params contract instead.
     */
    private static final String METADATA_ACCESS_OPERATOR = "metadata.access";

    /**
     * Params required by {@code MetadataAccessOperatorTranslator} (pp-rule-engine)
     * to render a metadata.access condition into DRL.
     */
    private static final Set<String> METADATA_ACCESS_REQUIRED_PARAMS =
            Set.of("schema_type", "field_key", "data_type");

    private final OperatorOptionJpaRepository operatorOptionRepo;
    private final ObjectMapper objectMapper;
    private final JsonSchemaFactory schemaFactory;
    private final RuleBuilderService ruleBuilderService;

    public RuleNodeSchemaValidator(OperatorOptionJpaRepository operatorOptionRepo,
                                   ObjectMapper objectMapper,
                                   RuleBuilderService ruleBuilderService) {
        this.operatorOptionRepo = operatorOptionRepo;
        this.objectMapper = objectMapper;
        this.ruleBuilderService = ruleBuilderService;
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

        // Semantic check for virtual metadata.access conditions: every referenced
        // field must exist in the live pp-metadata schema with a matching data_type.
        // Degrades to a no-op when pp-metadata is unreachable (never blocks a save).
        ruleBuilderService.validateMetadataConditions(rootNodes);
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

        // metadata.access is a virtual operator: no operator_options row exists for
        // it, so the lookup below would always fail as "Unknown operator". Validate
        // its params contract directly and stop.
        if (METADATA_ACCESS_OPERATOR.equals(operatorName)) {
            validateMetadataAccessParams(cond);
            return;
        }

        enforceRangeOrder(cond);

        String canonical = stripComparatorSuffix(operatorName);

        // PROM-985: thử khớp CHÍNH XÁC operatorName trước. Một số operator có hậu
        // tố trông giống comparator (vd ".lte") nhưng thực ra là PHẦN của canonical
        // name trong operator_options (vd "budget.redemptions.per_customer.in_campaign.lte").
        // stripComparatorSuffix sẽ cắt nhầm ".lte" -> không tìm thấy -> báo
        // "Invalid rule structure". Nếu khớp chính xác thất bại mới fallback về tên
        // đã strip (operator thường: "order.total.is_more_than" -> "order.total").
        Optional<OperatorOptionEntity> optionOpt =
                operatorOptionRepo.findFirstByOperatorNameOrderByDisplayOrderAsc(operatorName);
        if (optionOpt.isEmpty()) {
            optionOpt = operatorOptionRepo.findFirstByOperatorNameOrderByDisplayOrderAsc(canonical);
        }
        if (optionOpt.isEmpty()) {
            // A row may store a suffixed canonical (e.g. operator_name="order.total.gte").
            // A different comparator on the same field (e.g. "order.total.between") shares
            // the canonical base, so resolve by canonical prefix to reuse that row's
            // params_schema instead of failing as "unknown operator".
            optionOpt = operatorOptionRepo
                    .findFirstByOperatorNameStartingWithOrderByDisplayOrderAsc(canonical + ".");
        }
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

    /**
     * Validate a {@code metadata.access} COND node by its params contract instead of
     * a persisted params_schema. The engine translator needs {@code schema_type},
     * {@code field_key} and {@code data_type} to render the condition; reject the
     * node when any is missing or blank.
     */
    private void validateMetadataAccessParams(RuleNode cond) {
        var params = cond.getParams();
        if (params == null || params.isEmpty()) {
            throw new InvalidRuleStructureException(
                    cond.getId(),
                    "Operator 'metadata.access' requires params: schema_type, field_key, data_type");
        }
        for (String key : METADATA_ACCESS_REQUIRED_PARAMS) {
            Object value = params.get(key);
            if (value == null || (value instanceof String s && s.isBlank())) {
                throw new InvalidRuleStructureException(
                        cond.getId(),
                        "Operator 'metadata.access' requires non-blank param '" + key + "'");
            }
        }
    }

    /**
     * Cross-field guard for range ("between") conditions: when both {@code min} and
     * {@code max} numeric params are present, require {@code min <= max}. JSON Schema
     * cannot express this relationship, so it is enforced here for every COND node
     * (a no-op for non-range operators that carry neither key).
     */
    private void enforceRangeOrder(RuleNode cond) {
        var params = cond.getParams();
        if (params == null) {
            return;
        }
        Object min = params.get("min");
        Object max = params.get("max");
        if (min instanceof Number minNum && max instanceof Number maxNum
                && minNum.doubleValue() > maxNum.doubleValue()) {
            throw new InvalidRuleStructureException(
                    cond.getId(), "Range invalid: 'min' must be less than or equal to 'max'");
        }
    }

    private String stripComparatorSuffix(String operatorName) {
        return ComparatorSuffix.comparatorFromOperatorName(operatorName)
                .map(c -> operatorName.substring(0, operatorName.lastIndexOf('.')))
                .orElse(operatorName);
    }
}
