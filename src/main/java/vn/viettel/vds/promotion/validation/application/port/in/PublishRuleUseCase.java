package vn.viettel.vds.promotion.validation.application.port.in;

import vn.viettel.vds.promotion.validation.application.port.in.command.PublishRuleCommand;
import vn.viettel.vds.promotion.validation.application.port.in.dto.RuleResponse;

/**
 * Use case interface for publishing a validation rule
 */
public interface PublishRuleUseCase {

    /**
     * Execute the publish rule use case
     *
     * @param command The publish rule command
     * @return The published rule response
     */
    RuleResponse execute(PublishRuleCommand command);
}