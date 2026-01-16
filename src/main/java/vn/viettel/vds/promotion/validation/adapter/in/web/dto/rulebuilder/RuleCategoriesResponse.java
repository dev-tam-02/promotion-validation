package vn.viettel.vds.promotion.validation.adapter.in.web.dto.rulebuilder;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * Root response containing all rule categories.
 * Returned by GET /categories endpoint.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RuleCategoriesResponse(
        List<RuleCategoryResponse> categories,
        String version,
        Instant lastUpdated
) {
    public static RuleCategoriesResponse of(List<RuleCategoryResponse> categories, String version) {
        return new RuleCategoriesResponse(categories, version, Instant.now());
    }
}
