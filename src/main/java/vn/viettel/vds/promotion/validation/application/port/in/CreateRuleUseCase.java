package vn.viettel.vds.promotion.validation.application.port.in;

import vn.viettel.vds.promotion.validation.application.port.in.command.CreateRuleCommand;
import vn.viettel.vds.promotion.validation.application.port.in.dto.RuleResponse;

/**
 * Use case interface for creating a new validation rule
 */
public interface CreateRuleUseCase {

    /**
     * Execute the create rule use case
     *
     * @param command The create rule command
     * @return The created rule response
     */
    RuleResponse execute(CreateRuleCommand command);
}