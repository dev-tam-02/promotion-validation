package vn.viettel.vds.promotion.validation.application.service;

import org.junit.jupiter.api.Test;
import vn.viettel.vds.promotion.validation.domain.model.Rule;
import vn.viettel.vds.promotion.validation.domain.model.RuleNode;
import vn.viettel.vds.promotion.validation.event.ValidationRuleSettingAppliedEventPayload.AssignmentResult.ReasonCodeConfig;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ReasonCodeConfigMapperTest {

    private RuleNode cond(String reasonCode, String mode, String msg) {
        return RuleNode.builder()
                .type(RuleNode.NodeType.COND)
                .operatorName("op")
                .reasonCode(reasonCode)
                .violationDisplayMode(mode)
                .errorMessage(msg)
                .build();
    }

    @Test
    void skipsNullReasonCodeAndNonCond() {
        Rule rule = Rule.builder().id("r").logic(Rule.LogicType.ALL)
                .nodes(List.of(
                        cond(null, "DISABLED", "x"),
                        RuleNode.builder().type(RuleNode.NodeType.GROUP)
                                .groupLogic(Rule.LogicType.ALL).build()))
                .build();

        Map<String, ReasonCodeConfig> map =
                SettingValidationRuleEventPublisher.buildReasonCodeConfig(rule);

        assertThat(map).isEmpty();
    }

    @Test
    void hiddenWinsWhenDuplicateReasonCode() {
        Rule rule = Rule.builder().id("r").logic(Rule.LogicType.ALL)
                .nodes(List.of(
                        cond("MIN_ORDER", "DISABLED", "disabled-msg"),
                        cond("MIN_ORDER", "HIDDEN", "hidden-msg")))
                .build();

        Map<String, ReasonCodeConfig> map =
                SettingValidationRuleEventPublisher.buildReasonCodeConfig(rule);

        assertThat(map).containsOnlyKeys("MIN_ORDER");
        assertThat(map.get("MIN_ORDER").getViolationDisplayMode()).isEqualTo("HIDDEN");
        assertThat(map.get("MIN_ORDER").getErrorMessage()).isEqualTo("hidden-msg");
    }

    @Test
    void nullRuleReturnsEmpty() {
        assertThat(SettingValidationRuleEventPublisher.buildReasonCodeConfig(null)).isEmpty();
    }

    @Test
    void findsCondNestedUnderGroupTree() {
        // A resolved rule loaded from persistence is a nested tree: root GROUP with
        // COND children. buildReasonCodeConfig must recurse to reach the COND config.
        RuleNode condChild = cond("ORDER_TOTAL", "HIDDEN", "Đơn tối thiểu 500.000đ");
        RuleNode group = RuleNode.builder()
                .type(RuleNode.NodeType.GROUP)
                .groupLogic(Rule.LogicType.ALL)
                .children(List.of(condChild))
                .build();
        Rule rule = Rule.builder().id("r").logic(Rule.LogicType.ALL)
                .nodes(List.of(group))
                .build();

        Map<String, ReasonCodeConfig> map =
                SettingValidationRuleEventPublisher.buildReasonCodeConfig(rule);

        assertThat(map).containsOnlyKeys("ORDER_TOTAL");
        assertThat(map.get("ORDER_TOTAL").getViolationDisplayMode()).isEqualTo("HIDDEN");
        assertThat(map.get("ORDER_TOTAL").getErrorMessage()).isEqualTo("Đơn tối thiểu 500.000đ");
    }
}
