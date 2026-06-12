package vn.viettel.vds.promotion.validation.domain.model;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

/**
 * Domain read model for a validation-rule context option.
 *
 * <p>Backed by the {@code rule_contexts} lookup table. Display text is NOT stored
 * on the context row — it is resolved from the service-wide {@code translations}
 * table, so {@link #names} maps a locale tag ({@code vi}, {@code en}, ...) to the
 * localized name. Loaded dynamically for the Step 1 dropdown of the rule builder
 * and the CMS list-screen filter; replaces the old hardcoded {@code RuleContextType}
 * enum.</p>
 */
@Value
@Builder
public class RuleContextOption {

    /** Context code — business key (e.g. GENERAL_USAGE). */
    String code;

    /** Lifecycle status (ACTIVE / INACTIVE). */
    String status;

    /** Localized display names keyed by BCP-47 locale tag (vi, en, ...). */
    Map<String, String> names;
}
