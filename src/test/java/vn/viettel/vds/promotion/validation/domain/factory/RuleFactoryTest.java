package vn.viettel.vds.promotion.validation.domain.factory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import vn.viettel.vds.promotion.validation.domain.model.LogicType;
import vn.viettel.vds.promotion.validation.domain.model.RuleStatus;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RuleFactory Tests")
class RuleFactoryTest {

    private RuleFactory sut;

    @BeforeEach
    void setUp() {
        sut = new RuleFactory();
    }

    @Nested
    @DisplayName("createRule()")
    class CreateRuleTests {

        @Test
        @DisplayName("Should create rule with DRAFT status")
        void shouldCreateDraftRule() {
            var rule = sut.createRule("RULE_TEST", "Test Rule", LogicType.AND, "creator");

            assertThat(rule).isNotNull();
            assertThat(rule.getCode().getValue()).isEqualTo("RULE_TEST");
            assertThat(rule.getName().getValue()).isEqualTo("Test Rule");
            assertThat(rule.getLogicType()).isEqualTo(LogicType.AND);
            assertThat(rule.getStatus()).isEqualTo(RuleStatus.DRAFT);
            assertThat(rule.getCreatedBy()).isEqualTo("creator");
        }

        @Test
        @DisplayName("Should throw when code is null")
        void shouldThrow_whenCodeNull() {
            assertThatThrownBy(() -> sut.createRule(null, "Name", LogicType.AND, "creator"))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Should throw when name is null")
        void shouldThrow_whenNameNull() {
            assertThatThrownBy(() -> sut.createRule("RULE_X", null, LogicType.AND, "creator"))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Should throw when logicType is null")
        void shouldThrow_whenLogicTypeNull() {
            assertThatThrownBy(() -> sut.createRule("RULE_X", "Name", null, "creator"))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Should throw when createdBy is null")
        void shouldThrow_whenCreatedByNull() {
            assertThatThrownBy(() -> sut.createRule("RULE_X", "Name", LogicType.AND, null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("createRuleWithNodes()")
    class CreateRuleWithNodesTests {

        @Test
        @DisplayName("Should create rule with description and nodes")
        void shouldCreateWithDescAndNodes() {
            var rule = sut.createRuleWithNodes(
                    "RULE_NODES", "Node Rule", "A description",
                    LogicType.OR, null, "creator"
            );

            assertThat(rule.getDescription()).isEqualTo("A description");
            assertThat(rule.getLogicType()).isEqualTo(LogicType.OR);
        }

        @Test
        @DisplayName("Should create rule without description when null")
        void shouldCreateWithoutDesc_whenNull() {
            var rule = sut.createRuleWithNodes(
                    "RULE_NODES", "Node Rule", null,
                    LogicType.AND, null, "creator"
            );

            assertThat(rule).isNotNull();
            assertThat(rule.getCode().getValue()).isEqualTo("RULE_NODES");
        }
    }

    @Nested
    @DisplayName("createSimpleConditionRule()")
    class CreateSimpleConditionRuleTests {

        @Test
        @DisplayName("Should create rule with single condition node")
        void shouldCreateSimpleRule() {
            var rule = sut.createSimpleConditionRule(
                    "RULE_SIMPLE", "Simple Rule",
                    "segment", "EQUALS", "VIP",
                    "creator"
            );

            assertThat(rule).isNotNull();
            assertThat(rule.getDescription()).contains("segment");
            assertThat(rule.getDescription()).contains("EQUALS");
            assertThat(rule.getNodes()).hasSize(1);
            assertThat(rule.getNodes().get(0).getField()).isEqualTo("segment");
            assertThat(rule.getNodes().get(0).getOperator()).isEqualTo("EQUALS");
            assertThat(rule.getNodes().get(0).getValue()).isEqualTo("VIP");
        }
    }

    @Nested
    @DisplayName("createCompositeRule()")
    class CreateCompositeRuleTests {

        @Test
        @DisplayName("Should create rule with multiple condition nodes")
        void shouldCreateCompositeRule() {
            var conditions = List.of(
                    new RuleFactory.ConditionDefinition("c1", "segment", "EQUALS", "VIP", "Check segment"),
                    new RuleFactory.ConditionDefinition("c2", "tier", "EQUALS", "GOLD", "Check tier")
            );

            var rule = sut.createCompositeRule(
                    "RULE_COMP", "Composite Rule",
                    LogicType.AND, conditions, "creator"
            );

            assertThat(rule.getNodes()).hasSize(2);
            assertThat(rule.getDescription()).contains("2 conditions");
        }
    }

    @Nested
    @DisplayName("createFromTemplate()")
    class CreateFromTemplateTests {

        @Test
        @DisplayName("Should create rule from template with parameter substitution")
        void shouldCreateFromTemplate() {
            var nodeTemplates = List.of(
                    new RuleFactory.NodeTemplate("n1", "segment", "EQUALS", "${targetSegment}", "Check segment")
            );

            var template = new RuleFactory.RuleTemplate(
                    "tmpl-1", "VIP Template", "Template description",
                    LogicType.AND, nodeTemplates,
                    Map.of("targetSegment", "VIP")
            );

            var rule = sut.createFromTemplate(template, "RULE_TMPL", "Template Rule", "creator");

            assertThat(rule).isNotNull();
            assertThat(rule.getDescription()).isEqualTo("Template description");
            assertThat(rule.getNodes()).hasSize(1);
            assertThat(rule.getNodes().get(0).getValue()).isEqualTo("VIP");
        }

        @Test
        @DisplayName("Should create rule from template without node templates")
        void shouldCreateFromTemplate_withoutNodes() {
            var template = new RuleFactory.RuleTemplate(
                    "tmpl-1", "Empty Template", "No nodes",
                    LogicType.OR, null,
                    Map.of()
            );

            var rule = sut.createFromTemplate(template, "RULE_TMPL", "Template Rule", "creator");

            assertThat(rule).isNotNull();
            assertThat(rule.getLogicType()).isEqualTo(LogicType.OR);
        }
    }

    @Nested
    @DisplayName("ConditionDefinition")
    class ConditionDefinitionTests {

        @Test
        @DisplayName("Should store all fields correctly")
        void shouldStoreAllFields() {
            var condition = new RuleFactory.ConditionDefinition(
                    "c1", "field", "EQUALS", "value", "description"
            );

            assertThat(condition.getId()).isEqualTo("c1");
            assertThat(condition.getField()).isEqualTo("field");
            assertThat(condition.getOperator()).isEqualTo("EQUALS");
            assertThat(condition.getValue()).isEqualTo("value");
            assertThat(condition.getDescription()).isEqualTo("description");
        }
    }

    @Nested
    @DisplayName("RuleTemplate")
    class RuleTemplateTests {

        @Test
        @DisplayName("Should store all fields correctly")
        void shouldStoreAllFields() {
            var template = new RuleFactory.RuleTemplate(
                    "tmpl-1", "Template Name", "Description",
                    LogicType.AND, List.of(), Map.of("key", "value")
            );

            assertThat(template.getTemplateId()).isEqualTo("tmpl-1");
            assertThat(template.getTemplateName()).isEqualTo("Template Name");
            assertThat(template.getDescription()).isEqualTo("Description");
            assertThat(template.getLogicType()).isEqualTo(LogicType.AND);
            assertThat(template.getNodeTemplates()).isEmpty();
            assertThat(template.getParameters()).containsEntry("key", "value");
        }
    }
}
