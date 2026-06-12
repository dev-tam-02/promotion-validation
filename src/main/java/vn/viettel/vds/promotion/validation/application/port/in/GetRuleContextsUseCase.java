package vn.viettel.vds.promotion.validation.application.port.in;

import vn.viettel.vds.promotion.validation.domain.model.RuleContextOption;

import java.util.List;

/**
 * Use case for listing validation-rule contexts.
 *
 * <p>Populates the Step 1 dropdown of the rule builder (create/edit) and the
 * context filter on the CMS rule-list screen. Loaded dynamically from the
 * {@code rule_contexts} table — not from a hardcoded enum.</p>
 */
public interface GetRuleContextsUseCase {

    /**
     * List active contexts with localized display names.
     */
    List<RuleContextOption> listContexts();

    /**
     * Whether the given code is an ACTIVE context (case-insensitive).
     * Backs the create/update request validator.
     */
    boolean isActiveContext(String code);
}
