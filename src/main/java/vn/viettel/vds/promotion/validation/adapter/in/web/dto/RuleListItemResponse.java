package vn.viettel.vds.promotion.validation.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Light-weight response DTO for the rule list endpoint ({@code GET /v1/rules}).
 *
 * <p>Intentionally omits heavy fields that are only needed on the detail page:</p>
 * <ul>
 *   <li>{@code description} — long text, not shown in list table</li>
 *   <li>{@code fallbackErrorMessage} — long text, not shown in list table</li>
 *   <li>{@code nodes} — nested tree, expensive to serialize for 50+ rows</li>
 *   <li>{@code notes} — not shown in list table</li>
 *   <li>{@code limits} — not shown in list table</li>
 *   <li>{@code logic} — not shown in list table</li>
 *   <li>{@code dsl} — internal field</li>
 * </ul>
 *
 * <p>Use {@link RuleResponse} for the single-rule detail endpoint ({@code GET /v1/rules/{id}}).</p>
 */
@Schema(description = "Light-weight rule item for list views")
public record RuleListItemResponse(

        @Schema(description = "Rule ID")
        @JsonProperty("id")
        String id,

        @Schema(description = "Rule code", example = "WEEKEND_VIP_500K")
        @JsonProperty("code")
        String code,

        @Schema(description = "Rule name", example = "Weekend VIP ≥500k promotion")
        @JsonProperty("name")
        String name,

        @Schema(description = "Rule state", example = "draft",
                allowableValues = {"draft", "published", "archived"})
        @JsonProperty("state")
        String state,

        @Schema(description = "Rule context", example = "GENERAL_USAGE",
                allowableValues = {"GENERAL_USAGE"})
        @JsonProperty("context")
        String context,

        @Schema(description = "Published rule version", example = "3")
        @JsonProperty("ruleVersion")
        Long ruleVersion,

        @Schema(description = "Optimistic locking version")
        @JsonProperty("version")
        Long version,

        @Schema(description = "Total number of condition nodes in this rule")
        @JsonProperty("nodeCount")
        Integer nodeCount,

        @Schema(description = "Number of campaigns/products this rule is assigned to")
        @JsonProperty("assignmentCount")
        Long assignmentCount,

        @Schema(description = "Creation timestamp")
        @JsonProperty("createdAt")
        Instant createdAt,

        @Schema(description = "Creator user ID")
        @JsonProperty("createdBy")
        String createdBy,

        @Schema(description = "Creator display name")
        @JsonProperty("createdByName")
        String createdByName,

        @Schema(description = "Last update timestamp")
        @JsonProperty("updatedAt")
        Instant updatedAt,

        @Schema(description = "Last updater user ID")
        @JsonProperty("updatedBy")
        String updatedBy,

        @Schema(description = "Last updater display name")
        @JsonProperty("updatedByName")
        String updatedByName
) {
}
