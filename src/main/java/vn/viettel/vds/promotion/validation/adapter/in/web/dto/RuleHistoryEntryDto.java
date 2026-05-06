package vn.viettel.vds.promotion.validation.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

/**
 * DTO for a single history entry in {@code GET /v1/rules/{id}/history}.
 */
@Schema(description = "Single rule version snapshot from the history audit trail")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RuleHistoryEntryDto(
        @Schema(description = "History entry ID")
        String id,

        @Schema(description = "Rule ID this entry belongs to")
        String ruleId,

        @Schema(description = "Rule version number at the time of this entry", example = "2")
        long ruleVersion,

        @Schema(description = "Change type: CREATE or UPDATE", example = "UPDATE")
        String changeType,

        @Schema(description = "User who triggered the change")
        String changedBy,

        @Schema(description = "Timestamp when this history entry was recorded")
        Instant changedAt,

        @Schema(description = "Snapshot of the rule DSL map at this version; null for very early entries")
        Map<String, Object> dslSnapshot,

        @Schema(description = "bundleHash at the time of this entry; null if rule was DRAFT")
        String bundleHash,

        @Schema(description = "Rule state at this version (DRAFT / PUBLISHED / ARCHIVED)")
        String state
) {
}
