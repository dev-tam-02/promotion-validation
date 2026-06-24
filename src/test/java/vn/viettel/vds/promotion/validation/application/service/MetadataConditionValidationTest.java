package vn.viettel.vds.promotion.validation.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.viettel.vds.promotion.validation.adapter.out.external.MetadataServiceFeignClient;
import vn.viettel.vds.promotion.validation.application.port.out.RuleOptionsLookupPort;
import vn.viettel.vds.promotion.validation.domain.exception.InvalidRuleStructureException;
import vn.viettel.vds.promotion.validation.domain.model.OperatorCategory;
import vn.viettel.vds.promotion.validation.domain.model.RuleNode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RuleBuilderService#validateMetadataConditions} — verifies
 * that metadata.access conditions are checked against the live pp-metadata schema:
 * field existence, data_type match, and value constraints (string length/enum,
 * number range/enum), comparator-aware. Live schemas carry no constraints, so this
 * is the only place the value-constraint logic can be exercised.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RuleBuilderService — validateMetadataConditions()")
class MetadataConditionValidationTest {

    @Mock
    private OperatorConfigService operatorConfigService;
    @Mock
    private RuleOptionsLookupPort ruleOptionsLookupPort;
    @Mock
    private MetadataServiceFeignClient metadataServiceFeignClient;
    @Mock
    private vn.viettel.vds.promotion.validation.application.port.out.OperatorLabelPort operatorLabelPort;

    private RuleBuilderService sut;

    private static final String SCHEMA_ID = "standard-customer";

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().when(operatorLabelPort.loadOperatorLabels())
                .thenReturn(java.util.Map.of());
        OperatorLabelService operatorLabelService = new OperatorLabelService(operatorLabelPort);
        sut = new RuleBuilderService(operatorConfigService, ruleOptionsLookupPort,
                metadataServiceFeignClient, operatorLabelService);

        OperatorCategory customerCat = OperatorCategory.builder()
                .id("cat-customer-meta")
                .code("CUSTOMER")
                .name("Customer")
                .active(true)
                .metadataCategory(true)
                .metadataSchemaType("customer")
                .metadataSchemaId(SCHEMA_ID)
                .build();
        lenient().when(operatorConfigService.getAllCategoriesWithOptions(any()))
                .thenReturn(List.of(customerCat));

        // Schema with a constrained field set: enum string, length-only string,
        // ranged number, and a boolean.
        List<Map<String, Object>> fields = List.of(
                field("tier", "STRING", stringValidation(null, null, null, List.of("GOLD", "SILVER"))),
                field("code", "STRING", stringValidation(2, 3, null, null)),
                field("age", "NUMBER", numberValidation(0, 120)),
                field("score", "NUMBER", numberEnum(List.of(10, 20, 30), null)),
                field("level", "NUMBER", numberEnum(null, List.of(0))),
                field("is_merchant", "BOOLEAN", null)
        );
        lenient().when(metadataServiceFeignClient.getSchemaById(eq(SCHEMA_ID), eq(0), eq(100)))
                .thenReturn(schemaResponse(fields));
    }

    // -------------------------------------------------------------------------
    // Field existence + data_type
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("unknown field_key -> rejected")
    void unknownField() {
        RuleNode node = cond("customer", "no_such_field", "STRING", "EQUALS", "x");
        assertThatThrownBy(() -> validate(node))
                .isInstanceOf(InvalidRuleStructureException.class);
    }

    @Test
    @DisplayName("unknown schema_type -> rejected")
    void unknownSchema() {
        RuleNode node = cond("ghost", "tier", "STRING", "EQUALS", "GOLD");
        assertThatThrownBy(() -> validate(node))
                .isInstanceOf(InvalidRuleStructureException.class);
    }

    @Test
    @DisplayName("data_type mismatch -> rejected")
    void typeMismatch() {
        RuleNode node = cond("customer", "is_merchant", "STRING", "EQUALS", "x");
        assertThatThrownBy(() -> validate(node))
                .isInstanceOf(InvalidRuleStructureException.class);
    }

    // -------------------------------------------------------------------------
    // String constraints
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("enum: equals an allowed value passes; not-allowed rejected")
    void stringEnum() {
        assertThatCode(() -> validate(cond("customer", "tier", "STRING", "EQUALS", "GOLD")))
                .doesNotThrowAnyException();
        RuleNode rejected = cond("customer", "tier", "STRING", "EQUALS", "PURPLE");
        assertThatThrownBy(() -> validate(rejected))
                .isInstanceOf(InvalidRuleStructureException.class);
    }

    @Test
    @DisplayName("enum: negative comparator (not_equals) skips membership")
    void stringEnumNegativeSkipped() {
        assertThatCode(() -> validate(cond("customer", "tier", "STRING", "NOT_EQUALS", "PURPLE")))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("length: within bounds passes; too long/short rejected")
    void stringLength() {
        assertThatCode(() -> validate(cond("customer", "code", "STRING", "EQUALS", "AB")))
                .doesNotThrowAnyException();
        RuleNode tooLong = cond("customer", "code", "STRING", "EQUALS", "TOOLONG");
        assertThatThrownBy(() -> validate(tooLong))
                .isInstanceOf(InvalidRuleStructureException.class);
        RuleNode tooShort = cond("customer", "code", "STRING", "EQUALS", "A");
        assertThatThrownBy(() -> validate(tooShort))
                .isInstanceOf(InvalidRuleStructureException.class);
    }

    @Test
    @DisplayName("length: partial comparator (contains) skips length")
    void stringLengthPartialSkipped() {
        assertThatCode(() -> validate(cond("customer", "code", "STRING", "CONTAINS", "TOOLONG")))
                .doesNotThrowAnyException();
    }

    // -------------------------------------------------------------------------
    // Number constraints
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("number: equals within range passes; out of range rejected")
    void numberRange() {
        assertThatCode(() -> validate(cond("customer", "age", "NUMBER", "EQUALS", 50)))
                .doesNotThrowAnyException();
        RuleNode outOfRange = cond("customer", "age", "NUMBER", "EQUALS", 200);
        assertThatThrownBy(() -> validate(outOfRange))
                .isInstanceOf(InvalidRuleStructureException.class);
    }

    @Test
    @DisplayName("number: non-numeric value rejected")
    void numberType() {
        RuleNode node = cond("customer", "age", "NUMBER", "EQUALS", "abc");
        assertThatThrownBy(() -> validate(node))
                .isInstanceOf(InvalidRuleStructureException.class);
    }

    @Test
    @DisplayName("number: ordinal comparator (gte) skips range — threshold may exceed bounds")
    void numberOrdinalSkipsRange() {
        assertThatCode(() -> validate(cond("customer", "age", "NUMBER", "GREATER_OR_EQUAL", 200)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("number enum: equals an allowed value passes; not-allowed rejected")
    void numberEnumAllowed() {
        assertThatCode(() -> validate(cond("customer", "score", "NUMBER", "EQUALS", 20)))
                .doesNotThrowAnyException();
        RuleNode notAllowed = cond("customer", "score", "NUMBER", "EQUALS", 25);
        assertThatThrownBy(() -> validate(notAllowed))
                .isInstanceOf(InvalidRuleStructureException.class);
    }

    @Test
    @DisplayName("number enum: excluded value rejected; other value passes")
    void numberEnumExcluded() {
        RuleNode excluded = cond("customer", "level", "NUMBER", "EQUALS", 0);
        assertThatThrownBy(() -> validate(excluded))
                .isInstanceOf(InvalidRuleStructureException.class);
        assertThatCode(() -> validate(cond("customer", "level", "NUMBER", "EQUALS", 5)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("number enum: ordinal comparator (gte) skips enum membership")
    void numberEnumOrdinalSkipped() {
        assertThatCode(() -> validate(cond("customer", "score", "NUMBER", "GREATER_OR_EQUAL", 25)))
                .doesNotThrowAnyException();
    }

    // -------------------------------------------------------------------------
    // Boolean / no-value
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("boolean is_true carries no value -> passes")
    void booleanNoValue() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("schema_type", "customer");
        params.put("field_key", "is_merchant");
        params.put("data_type", "BOOLEAN");
        params.put("comparator", "IS_TRUE");
        assertThatCode(() -> validate(condNode(params))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("metadata service unreachable (empty fields) -> degrades to no-op")
    void gracefulDegrade() {
        when(metadataServiceFeignClient.getSchemaById(SCHEMA_ID, 0, 100))
                .thenReturn(schemaResponse(List.of()));
        assertThatCode(() -> validate(cond("customer", "anything", "STRING", "EQUALS", "x")))
                .doesNotThrowAnyException();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void validate(RuleNode cond) {
        sut.validateMetadataConditions(List.of(cond));
    }

    private RuleNode cond(String schemaType, String fieldKey, String dataType, String comparator, Object value) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("schema_type", schemaType);
        params.put("field_key", fieldKey);
        params.put("data_type", dataType);
        params.put("comparator", comparator);
        params.put("value", value);
        return condNode(params);
    }

    private RuleNode condNode(Map<String, Object> params) {
        return RuleNode.builder()
                .nodeId("c1")
                .type(RuleNode.NodeType.COND)
                .operatorName("metadata.access")
                .reasonCode("METADATA_ACCESS")
                .params(params)
                .build();
    }

    private static Map<String, Object> field(String name, String type, Map<String, Object> validation) {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("name", name);
        f.put("type", type);
        f.put("cardinality", "SINGLE");
        f.put("validation", validation);
        return f;
    }

    private static Map<String, Object> stringValidation(Integer min, Integer max, Integer exact, List<String> enumValues) {
        Map<String, Object> sv = new LinkedHashMap<>();
        sv.put("minLength", min);
        sv.put("maxLength", max);
        sv.put("exactLength", exact);
        sv.put("equalToAnyOf", enumValues);
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("stringValidation", sv);
        return v;
    }

    private static Map<String, Object> numberValidation(Integer gte, Integer lte) {
        Map<String, Object> nv = new LinkedHashMap<>();
        nv.put("greaterThanOrEqual", gte);
        nv.put("lessThanOrEqual", lte);
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("numberValidation", nv);
        return v;
    }

    private static Map<String, Object> numberEnum(List<Integer> allowed, List<Integer> excluded) {
        Map<String, Object> nv = new LinkedHashMap<>();
        nv.put("equalToAnyOf", allowed);
        nv.put("notEqualToAnyOf", excluded);
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("numberValidation", nv);
        return v;
    }

    private static Map<String, Object> schemaResponse(List<Map<String, Object>> fields) {
        Map<String, Object> definitions = new LinkedHashMap<>();
        definitions.put("content", fields);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("definitions", definitions);
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("data", data);
        return resp;
    }
}
