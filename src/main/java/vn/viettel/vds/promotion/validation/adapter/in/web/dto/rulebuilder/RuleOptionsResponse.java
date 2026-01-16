package vn.viettel.vds.promotion.validation.adapter.in.web.dto.rulebuilder;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Paginated response for rule options.
 * Used when fetching options for a specific rule (e.g., list of segments).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RuleOptionsResponse(
        String ruleId,
        List<RuleOptionResponse> options,
        Long totalElements,
        Integer page,
        Integer size
) {
    public static RuleOptionsResponse of(String ruleId, List<RuleOptionResponse> options) {
        return new RuleOptionsResponse(
                ruleId,
                options,
                (long) options.size(),
                0,
                options.size()
        );
    }

    public static RuleOptionsResponse paginated(String ruleId, List<RuleOptionResponse> options, long total, int page, int size) {
        return new RuleOptionsResponse(ruleId, options, total, page, size);
    }
}
