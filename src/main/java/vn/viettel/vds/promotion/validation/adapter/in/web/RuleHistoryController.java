package vn.viettel.vds.promotion.validation.adapter.in.web;

import com.promix.platform.web.annotation.ResponseWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.viettel.vds.promotion.validation.adapter.in.web.dto.RuleHistoryEntryDto;
import vn.viettel.vds.promotion.validation.application.service.RuleHistoryService;
import vn.viettel.vds.promotion.validation.domain.model.RuleHistoryEntry;

import java.security.Principal;
import java.util.List;

/**
 * Exposes V2 rule history and restore endpoints:
 * <ul>
 *   <li>{@code GET  /v1/rules/{id}/history} — list all version snapshots for a rule.</li>
 *   <li>{@code POST /v1/rules/{id}/restore/{version}} — restore a rule to a prior version.</li>
 * </ul>
 */
@RestController
@ResponseWrapper
@RequestMapping("${spring.application.context-path}/v1/rules")
@Tag(name = "Rules", description = "Rule management API")
public class RuleHistoryController {

    private static final Logger logger = LoggerFactory.getLogger(RuleHistoryController.class);

    private final RuleHistoryService historyService;

    public RuleHistoryController(RuleHistoryService historyService) {
        this.historyService = historyService;
    }

    @Operation(
            summary = "Get rule version history",
            description = "Returns all immutable audit snapshots for a rule, ordered by version ascending.")
    @ApiResponse(responseCode = "200", description = "History returned")
    @ApiResponse(responseCode = "404", description = "Rule not found")
    @GetMapping("/{id}/history")
    public ResponseEntity<List<RuleHistoryEntryDto>> getHistory(
            @Parameter(description = "Rule ID") @PathVariable String id) {

        logger.info("GET /v1/rules/{}/history", id);

        List<RuleHistoryEntryDto> history = historyService.getHistory(id).stream()
                .map(this::toDto)
                .toList();

        return ResponseEntity.ok(history);
    }

    @Operation(
            summary = "Restore rule to a prior version",
            description = "Applies the DSL snapshot from the target version back to the current rule. "
                    + "Records the pre-restore state as a new UPDATE history entry and increments ruleVersion.")
    @ApiResponse(responseCode = "200", description = "Rule restored successfully")
    @ApiResponse(responseCode = "404", description = "Rule not found")
    @ApiResponse(responseCode = "400", description = "Target version not found in history")
    @PostMapping("/{id}/restore/{version}")
    public ResponseEntity<Void> restore(
            @Parameter(description = "Rule ID") @PathVariable String id,
            @Parameter(description = "Target version number to restore") @PathVariable long version,
            Principal principal) {

        String actor = principal != null ? principal.getName() : "system";
        logger.info("POST /v1/rules/{}/restore/{} by={}", id, version, actor);

        historyService.restore(id, version, actor);

        return ResponseEntity.ok().build();
    }

    private RuleHistoryEntryDto toDto(RuleHistoryEntry entry) {
        return new RuleHistoryEntryDto(
                entry.getId(),
                entry.getRuleId(),
                entry.getRuleVersion(),
                entry.getChangeType().name(),
                entry.getChangedBy(),
                entry.getChangedAt(),
                entry.getDslSnapshot(),
                entry.getBundleHash(),
                entry.getState()
        );
    }
}
