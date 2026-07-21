package vn.viettel.vds.promotion.validation.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.OperatorOptionJpaRepository;
import vn.viettel.vds.promotion.validation.domain.exception.InvalidRuleStructureException;
import vn.viettel.vds.promotion.validation.domain.model.RuleNode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * PROM-1166 — card metadata.access nhiều điều kiện (FE tách thành GROUP ALL + N COND).
 *
 * <p>Trước fix FE, mỗi COND con chỉ mang {@code {value}} (mất template
 * {schema_type, field_key, data_type}) nên BE từ chối. Test này chốt hợp đồng
 * hai chiều: payload thiếu template PHẢI bị từ chối (BE đang đúng), payload FE
 * sinh sau fix PHẢI qua; đồng thời thông điệp lỗi phải đọc được (không còn chỉ
 * là một UUID trần).</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PROM-1166 — metadata.access multi-condition group")
class Prom1166MetadataMultiCondTest {

    @Mock
    private OperatorOptionJpaRepository operatorOptionRepo;
    @Mock
    private RuleBuilderService ruleBuilderService;

    private RuleNodeSchemaValidator sut;

    @BeforeEach
    void setUp() {
        sut = new RuleNodeSchemaValidator(operatorOptionRepo, new ObjectMapper(), ruleBuilderService);
    }

    @Test
    @DisplayName("payload FE cũ (chỉ có value) → từ chối vì thiếu schema_type")
    void rejectsCondWithoutMetadataTemplate() {
        RuleNode group = group(List.of(
                cond("c1", "LESS_OR_EQUAL", Map.of("value", 90)),
                cond("c2", "EQUALS", Map.of("value", 1000))
        ));

        assertThatThrownBy(() -> sut.validate(List.of(group)))
                .isInstanceOf(InvalidRuleStructureException.class)
                .hasMessageContaining("schema_type");
    }

    @Test
    @DisplayName("payload FE sau fix (đủ template mỗi COND con) → hợp lệ")
    void acceptsCondsCarryingMetadataTemplate() {
        RuleNode group = group(List.of(
                cond("c1", "LESS_OR_EQUAL", meta("redemption", "string_single", "NUMBER", "LESS_OR_EQUAL", 90)),
                cond("c2", "EQUALS", meta("redemption", "string_single", "NUMBER", "EQUALS", 1000)),
                cond("c3", "GREATER_OR_EQUAL", meta("redemption", "string_single", "NUMBER", "GREATER_OR_EQUAL", 90000))
        ));

        assertThatCode(() -> sut.validate(List.of(group))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("COND boolean IS_TRUE không có value vẫn hợp lệ khi đủ template")
    void acceptsBooleanCondWithoutValue() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("schema_type", "redemption");
        params.put("field_key", "Boolean");
        params.put("data_type", "BOOLEAN");
        params.put("comparator", "IS_TRUE");

        RuleNode group = group(List.of(
                cond("c1", "IS_TRUE", params),
                cond("c2", "EQUALS", meta("redemption", "Date", "DATE", "EQUALS", "2026-07-28"))
        ));

        assertThatCode(() -> sut.validate(List.of(group))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("params lỗi chỉ có 1 key 'detail' để {0} luôn render ra lý do, không phải UUID")
    void errorParamsCarryReadableDetail() {
        InvalidRuleStructureException ex =
                new InvalidRuleStructureException("019f6fad-89c5-767c-9f66-748b8c77baed",
                        "Operator 'metadata.access' requires non-blank param 'schema_type'");

        assertThat(ex.getParams()).hasSize(1).containsKey("detail");
        String detail = String.valueOf(ex.getParams().get("detail"));
        assertThat(detail).contains("requires non-blank param 'schema_type'")
                .contains("019f6fad-89c5-767c-9f66-748b8c77baed");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static Map<String, Object> meta(String schemaType, String fieldKey, String dataType,
                                            String comparator, Object value) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("schema_type", schemaType);
        p.put("field_key", fieldKey);
        p.put("data_type", dataType);
        p.put("comparator", comparator);
        p.put("value", value);
        return p;
    }

    private static RuleNode cond(String id, String comparator, Map<String, Object> params) {
        return RuleNode.builder()
                .nodeId(id)
                .type(RuleNode.NodeType.COND)
                .operatorName("metadata.access")
                .comparator(comparator)
                .reasonCode("METADATA_ACCESS_" + comparator)
                .params(params)
                .build();
    }

    private static RuleNode group(List<RuleNode> children) {
        return RuleNode.builder()
                .nodeId("g1")
                .type(RuleNode.NodeType.GROUP)
                .groupLogic(vn.viettel.vds.promotion.validation.domain.model.Rule.LogicType.ALL)
                .children(children)
                .build();
    }
}
