package vn.viettel.vds.promotion.validation.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.viettel.vds.promotion.validation.domain.model.Operator;
import vn.viettel.vds.promotion.validation.domain.model.Rule;
import vn.viettel.vds.promotion.validation.domain.model.RuleNode;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the {@code customer.is_owner} operator (Task 15, gap G3).
 *
 * <p>Covers:
 * <ul>
 *   <li>Template {@code tpl_customer_is_owner_v1} renders {@code VoucherFact} + {@code CustomerFact} patterns.</li>
 *   <li>Template renders with empty params map (no-params operator).</li>
 *   <li>DrlCompiler correctly compiles a single COND node with {@code customer.is_owner}.</li>
 *   <li>Compiled DRL contains expected fact class names and field conditions.</li>
 * </ul>
 */
@DisplayName("customer.is_owner operator — template + DrlCompiler")
class OwnerOnlyOperatorTest {

    private DrlCompiler compiler;
    private Rule baseRule;

    @BeforeEach
    void setUp() {
        compiler = new DrlCompiler();
        baseRule = Rule.builder()
                .id("rule-sys-owner-only")
                .logic(Rule.LogicType.ALL)
                .build();
    }

    // ─── Template rendering ────────────────────────────────────────────────────

    @Test
    @DisplayName("tpl_customer_is_owner_v1 renders VoucherFact + CustomerFact patterns")
    void template_rendersVoucherFactAndCustomerFact() {
        String snippet = compiler.renderCondTemplate(
                "tpl_customer_is_owner_v1",
                Map.of());   // no params for this operator

        assertThat(snippet)
                .contains("VoucherFact")
                .contains("CustomerFact")
                .contains("ownerCustomerId");
    }

    @Test
    @DisplayName("tpl_customer_is_owner_v1 renders with null params (empty map fallback)")
    void template_rendersWithNullParams() {
        String snippet = compiler.renderCondTemplate(
                "tpl_customer_is_owner_v1",
                null);   // null params → DrlCompiler passes Map.of()

        assertThat(snippet)
                .contains("VoucherFact")
                .contains("CustomerFact");
    }

    @Test
    @DisplayName("tpl_customer_is_owner_v1 guard condition: ownerCustomerId != null")
    void template_containsOwnerNullGuard() {
        String snippet = compiler.renderCondTemplate(
                "tpl_customer_is_owner_v1",
                Map.of());

        assertThat(snippet).contains("ownerCustomerId != null");
    }

    @Test
    @DisplayName("tpl_customer_is_owner_v1 cross-field equality: CustomerFact(id == $v.ownerCustomerId)")
    void template_containsCrossFieldEquality() {
        String snippet = compiler.renderCondTemplate(
                "tpl_customer_is_owner_v1",
                Map.of());

        assertThat(snippet).contains("id == $v.ownerCustomerId");
    }

    // ─── DrlCompiler with customer.is_owner node ───────────────────────────────

    @Test
    @DisplayName("DrlCompiler compiles single COND customer.is_owner node → valid DRL snippet")
    void compile_singleOwnerOnlyCond_producesValidDrl() {
        RuleNode cond = makeOwnerOnlyCond("n1");
        Map<String, Operator> operators = buildOperatorMap();

        String drl = compiler.compile(baseRule, List.of(cond), operators);

        assertThat(drl)
                .contains("rule \"rule-sys-owner-only\"")
                .contains("VoucherFact")
                .contains("CustomerFact")
                .contains("ownerCustomerId");
    }

    @Test
    @DisplayName("DrlCompiler falls back to tpl_customer_is_owner_v1 when no operator provided")
    void compile_noOperatorMap_usesNamingConventionFallback() {
        RuleNode cond = makeOwnerOnlyCond("n1");

        // Passing null operators → DrlCompiler derives compilerId from operator name
        String drl = compiler.compile(baseRule, List.of(cond), null);

        assertThat(drl)
                .contains("VoucherFact")
                .contains("CustomerFact");
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private RuleNode makeOwnerOnlyCond(String nodeId) {
        return RuleNode.builder()
                .nodeId(nodeId)
                .type(RuleNode.NodeType.COND)
                .operatorName("customer.is_owner")
                .params(Map.of())
                .reasonCode("VOUCHER_NOT_OWNED_BY_CUSTOMER")
                .build();
    }

    private Map<String, Operator> buildOperatorMap() {
        return Map.of(
                "customer.is_owner", Operator.builder()
                        .id("spec-op-customer-is-owner-001")
                        .name("customer.is_owner")
                        .compilerId("tpl_customer_is_owner_v1")
                        .status(Operator.OperatorStatus.ACTIVE)
                        .build()
        );
    }
}
