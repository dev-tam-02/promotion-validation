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
import com.promix.platform.validation.condition.CanonicalOperatorName;
import com.promix.platform.validation.condition.ConditionConstraintValidator;
import com.promix.platform.validation.condition.ConditionOperator;
import com.promix.platform.validation.condition.ConditionValueValidator;
import vn.viettel.vds.promotion.validation.domain.model.RuleNode;

import java.time.LocalDateTime;
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

    /** Key params chuẩn hoá theo ValueShape (SINGLE/RANGE → "value", MULTI → "values"). */
    private static final String VALUE = "value";
    private static final String VALUES = "values";

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
        // its params contract directly and stop. The effective operator_name is
        // composed as {@code metadata.access.<CANONICAL>} (e.g. "metadata.access.EQUALS")
        // when the field carries a comparator, so match on the bare field-path after
        // stripping any canonical suffix — not on the literal "metadata.access".
        if (METADATA_ACCESS_OPERATOR.equals(stripComparatorSuffix(operatorName))) {
            validateMetadataAccessParams(cond);
            return;
        }

        // Operator đã chuẩn hoá theo ValueShape: params mang "value"/"values" → validate
        // shape bằng ConditionValueValidator (một nguồn chân lý duy nhất), bỏ qua
        // params_schema cũ vốn khai theo key semantic (amount/segments/min/max). Operator
        // CHƯA chuẩn hoá (aggregate/api_key/user/boolean...) rơi xuống path params_schema
        // legacy bên dưới. metadata.access đã được bắt ở nhánh trên nên không lọt vào đây.
        var params = cond.getParams();
        boolean canonicalValueShape = params != null
                && (params.containsKey(VALUE) || params.containsKey(VALUES));
        Optional<String> comparatorOpt = CanonicalOperatorName.comparatorFromOperatorName(operatorName);
        if (canonicalValueShape && comparatorOpt.isPresent()) {
            ConditionOperator operator;
            try {
                operator = ConditionOperator.valueOf(comparatorOpt.get());
            } catch (IllegalArgumentException e) {
                throw new InvalidRuleStructureException(cond.getId(),
                        "Unknown comparator '" + comparatorOpt.get() + "' in operator '" + operatorName + "'");
            }
            ensureOperatorExists(cond, operatorName);
            Object operand = switch (operator.valueShape()) {
                case MULTI -> params.get(VALUES);
                case SINGLE, RANGE -> params.get(VALUE);
                case NONE -> null;
            };
            ConditionValueValidator.validate(operator, operand).ifPresent(msg -> {
                throw new InvalidRuleStructureException(cond.getId(),
                        "params invalid for operator '" + operatorName + "': " + msg);
            });
            return;
        }

        // --- Legacy path: operator chưa chuẩn hoá params (aggregate/api_key/user/boolean...) ---
        enforceRangeOrder(cond);

        OperatorOptionEntity option = ensureOperatorExists(cond, operatorName);
        String schemaJson = option.getParamsSchema();
        if (schemaJson == null || schemaJson.isBlank()) {
            log.debug("No params_schema for operator={}, skipping schema validation", operatorName);
            return;
        }

        JsonSchema schema;
        try {
            JsonNode schemaNode = objectMapper.readTree(schemaJson);
            schema = schemaFactory.getSchema(schemaNode);
        } catch (Exception e) {
            log.error("Malformed params_schema for operator={}", operatorName, e);
            throw new InvalidRuleStructureException(
                    cond.getId(),
                    "Operator '" + operatorName + "' has a malformed params_schema in the database (admin must fix)");
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
     * Resolve the {@link OperatorOptionEntity} backing this operator (exact name →
     * canonical base → canonical-prefixed row), throwing when none exists. Shared by
     * the canonical-shape path (existence check) and the legacy params_schema path.
     */
    private OperatorOptionEntity ensureOperatorExists(RuleNode cond, String operatorName) {
        String canonical = stripComparatorSuffix(operatorName);
        Optional<OperatorOptionEntity> optionOpt =
                operatorOptionRepo.findFirstByOperatorNameOrderByDisplayOrderAsc(operatorName);
        if (optionOpt.isEmpty()) {
            optionOpt = operatorOptionRepo.findFirstByOperatorNameOrderByDisplayOrderAsc(canonical);
        }
        if (optionOpt.isEmpty()) {
            optionOpt = operatorOptionRepo
                    .findFirstByOperatorNameStartingWithOrderByDisplayOrderAsc(canonical + ".");
        }
        if (optionOpt.isEmpty()) {
            throw new InvalidRuleStructureException(
                    cond.getId(),
                    "Unknown operator: '" + operatorName + "' (canonical='" + canonical + "' not found in operator_options)");
        }
        return optionOpt.get();
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
        // Range NGÀY/NGÀY-GIỜ: min/max là chuỗi ngày → dùng CHUNG parser của promix
        // (một nguồn định dạng ngày) để so sánh thứ tự nếu cả hai parse được.
        LocalDateTime minDate = ConditionConstraintValidator.parseTemporal(min);
        LocalDateTime maxDate = ConditionConstraintValidator.parseTemporal(max);
        if (minDate != null && maxDate != null && minDate.isAfter(maxDate)) {
            throw new InvalidRuleStructureException(
                    cond.getId(), "Range invalid: 'min' must be less than or equal to 'max'");
        }
    }

    private String stripComparatorSuffix(String operatorName) {
        return CanonicalOperatorName.stripCanonicalSuffix(operatorName);
    }
}
