package vn.viettel.vds.promotion.validation.application.port.out;

import vn.viettel.vds.promotion.validation.domain.model.RuleContextOption;

import java.util.List;

/**
 * Port for reading validation-rule contexts from the {@code rule_contexts} lookup
 * table, with display names resolved from the service-wide {@code translations} table.
 */
public interface RuleContextPersistencePort {

    /**
     * List active contexts with their localized names attached, ordered by code.
     */
    List<RuleContextOption> findActiveContexts();

    /**
     * Whether the given code is an ACTIVE context (case-insensitive).
     * Used by the create/update validators to accept only persisted contexts.
     */
    boolean isActiveContext(String code);
}
