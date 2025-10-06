package vn.viettel.vds.promotion.validation.application.port.in;

import vn.viettel.vds.promotion.validation.application.port.in.command.UpdateRuleCommand;
import vn.viettel.vds.promotion.validation.application.port.in.dto.RuleResponse;

/**
 * Use case interface for updating an existing validation rule
 */
public interface UpdateRuleUseCase {

    /**
     * Execute the update rule use case
     *
     * @param command The update rule command
     * @return The updated rule response
     */
    RuleResponse execute(UpdateRuleCommand command);
}