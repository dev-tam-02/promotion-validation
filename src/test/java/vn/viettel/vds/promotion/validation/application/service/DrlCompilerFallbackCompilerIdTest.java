package vn.viettel.vds.promotion.validation.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.viettel.vds.promotion.validation.domain.model.Rule;
import vn.viettel.vds.promotion.validation.domain.model.RuleNode;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fallback compilerId khi {@code operator_registry} khong co dong cho operator.
 *
 * <p><b>Boi canh do that tren 229 ngay 28/08/2026.</b> Ten operator ma CMS luu dung comparator
 * DAY DU viet hoa ({@code order.total.GREATER_OR_EQUAL}), con file template lai dung dang VIET TAT
 * viet thuong ({@code tpl_order_total_gte_v1.drl.mustache}). Fallback cu ghep thang
 * {@code tpl_ + ten + _v1} nen tra ra {@code tpl_order_total_GREATER_OR_EQUAL_v1} — file khong ton
 * tai — va ca luot publish rule chet:
 *
 * <pre>
 * ERROR RuleManagementService republishSystemRules: failed for rule 01a0467f-e043-…:
 *   Cannot load or render DRL template for compilerId=tpl_order_total_GREATER_OR_EQUAL_v1
 * </pre>
 *
 * <p>Cac test duoi day di qua {@code compile()} that (khong goi thang helper private) va khong
 * truyen operator registry — dung duong ma bug da di.
 */
@DisplayName("DrlCompiler — fallback compilerId khop ten file template that")
class DrlCompilerFallbackCompilerIdTest {

    private final DrlCompiler compiler = new DrlCompiler();

    private String compileVoiOperator(String operatorName, Map<String, Object> params) {
        RuleNode cond = RuleNode.builder()
                .nodeId("n1")
                .type(RuleNode.NodeType.COND)
                .operatorName(operatorName)
                .params(params)
                .build();
        Rule rule = Rule.builder()
                .id("rule-fallback")
                .logic(Rule.LogicType.ALL)
                .build();
        // operators = null ⇒ khong tra duoc compilerId ⇒ di dung nhanh fallback
        return compiler.compile(rule, List.of(cond), null);
    }

    @Test
    @DisplayName("GREATER_OR_EQUAL dung template gte thay vi ten viet hoa khong ton tai")
    void greaterOrEqualDungTemplateGte() {
        String drl = compileVoiOperator("order.total.GREATER_OR_EQUAL",
                Map.of("amount", 100_000, "currency", "VND"));

        assertThat(drl)
                .as("phai render duoc — truoc day nem 'Cannot load or render DRL template for"
                        + " compilerId=tpl_order_total_GREATER_OR_EQUAL_v1'")
                .contains("OrderFact")
                .contains("100000");
    }

    @Test
    @DisplayName("GREATER_THAN dung template gt")
    void greaterThanDungTemplateGt() {
        String drl = compileVoiOperator("order.initial.amount.GREATER_THAN",
                Map.of("amount", 50_000, "currency", "VND"));

        assertThat(drl).contains("50000");
    }

    @Test
    @DisplayName("LESS_OR_EQUAL dung template lte")
    void lessOrEqualDungTemplateLte() {
        String drl = compileVoiOperator("order.total.LESS_OR_EQUAL",
                Map.of("amount", 900_000, "currency", "VND"));

        assertThat(drl).contains("900000");
    }

    @Test
    @DisplayName("EQUALS dung template equals")
    void equalsDungTemplateEquals() {
        String drl = compileVoiOperator("order.total.EQUALS",
                Map.of("amount", 12_345, "currency", "VND"));

        assertThat(drl).contains("12345");
    }
}
