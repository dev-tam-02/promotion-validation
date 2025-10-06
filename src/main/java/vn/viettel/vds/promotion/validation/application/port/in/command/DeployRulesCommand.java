package vn.viettel.vds.promotion.validation.application.port.in.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Command for deploying validation rules to engine
 */
@Data
@Builder
public class DeployRulesCommand {

    @NotBlank(message = "Rule set ID is required")
    private String ruleSetId;

    @NotEmpty(message = "At least one rule ID is required")
    private List<String> ruleIds;

    private String environment;

    private boolean warmUp;

    private Map<String, Object> deploymentConfig;

    private String deployedBy;
}