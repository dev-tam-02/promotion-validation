package vn.viettel.vds.promotion.validation.application.port.in.command;

import lombok.Builder;
import lombok.Data;
import vn.viettel.vds.promotion.validation.domain.model.Rule;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

/**
 * Command for updating an existing validation rule
 */
@Data
@Builder
@SuppressWarnings("unused")
public class UpdateRuleCommand {

    private String name;
    private String description;
    private String expression;
    private Rule.RuleType type;
    private Boolean active;
    private Integer priority;
    private Map<String, Object> configuration;
    private Set<String> targetSegments;
    private Instant effectiveFrom;
    private Instant effectiveUntil;
    private String modifiedBy;
}