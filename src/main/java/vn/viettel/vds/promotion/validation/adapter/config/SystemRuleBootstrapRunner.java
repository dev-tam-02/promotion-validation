package vn.viettel.vds.promotion.validation.adapter.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.application.service.RuleManagementService;

/**
 * Runs on application startup (after Liquibase migrations and bean wiring) to
 * populate {@code bundle_hash} for any PUBLISHED rules that were seeded without
 * a compiled bundle.
 *
 * <p>This covers the {@code rule-sys-owner-only} seed (changelog 031) which ships
 * with {@code bundle_hash=NULL} — the actual compile+register with pp-rule-engine
 * happens here on first boot.
 *
 * <p>{@code @Order(LOWEST_PRECEDENCE)} ensures this runs after Liquibase and all
 * other ApplicationRunners that set up infrastructure.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class SystemRuleBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SystemRuleBootstrapRunner.class);

    private final RuleManagementService ruleManagementService;

    public SystemRuleBootstrapRunner(RuleManagementService ruleManagementService) {
        this.ruleManagementService = ruleManagementService;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("SystemRuleBootstrapRunner: starting bootstrap republish of system rules");
        try {
            ruleManagementService.republishSystemRules();
        } catch (Exception ex) {
            log.error("SystemRuleBootstrapRunner: bootstrap failed (non-fatal): {}", ex.getMessage());
        }
        log.info("SystemRuleBootstrapRunner: bootstrap complete");
    }
}
