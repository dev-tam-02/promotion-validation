package vn.viettel.vds.promotion.validation.application.port.in.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import vn.viettel.vds.promotion.validation.domain.model.Rule;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Command for creating a new validation rule
 */
@Data
@Builder
@SuppressWarnings("unused")
public class CreateRuleCommand {

    @NotBlank(message = "Rule code is required")
    private String code;

    @NotBlank(message = "Rule code is required")
    private String ruleCode;

    @NotBlank(message = "Rule name is required")
    private String name;

    private String description;

    @NotBlank(message = "Rule expression is required")
    private String expression;

    @NotNull(message = "Rule type is required")
    private Rule.RuleType type;

    @Builder.Default
    private boolean active = true;

    @Builder.Default
    private int priority = 100;

    private Map<String, Object> configuration;

    private Set<String> targetSegments;

    private String promotionId;

    private String ruleSetId;

    private Instant effectiveFrom;

    private Instant effectiveUntil;

    private String createdBy;

    private List<RuleNodeCommand> nodes;

    private String logicType;

    /**
     * Validate the command
     */
    public void validate() {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Rule code is required");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Rule name is required");
        }
    }
}
