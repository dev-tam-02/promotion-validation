package vn.viettel.vds.promotion.validation.application.service;

import org.junit.jupiter.api.Test;
import vn.viettel.vds.promotion.validation.domain.model.Rule;

import static org.assertj.core.api.Assertions.assertThat;

class CommandProcessingResultTest {

    @Test
    void success_carriesResolvedRule() {
        Rule rule = Rule.builder().id("r1").logic(Rule.LogicType.ALL).build();

        SettingValidationRuleCommandHandler.CommandProcessingResult result =
                SettingValidationRuleCommandHandler.CommandProcessingResult.success(
                        null, null, null, rule);

        assertThat(result.getResolvedRule()).isSameAs(rule);
    }

    @Test
    void success_allowsNullResolvedRule_forRulelessBinding() {
        SettingValidationRuleCommandHandler.CommandProcessingResult result =
                SettingValidationRuleCommandHandler.CommandProcessingResult.success(
                        null, null, null, null);

        assertThat(result.getResolvedRule()).isNull();
    }
}
