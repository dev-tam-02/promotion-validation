package vn.viettel.vds.promotion.validation.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import vn.viettel.vds.promotion.schema.validation.command.ApplicabilityRule;
import vn.viettel.vds.promotion.schema.validation.command.ApplicabilityScope;

import java.util.List;

/**
 * Service for mapping and analyzing command data.
 * <p>
 * Note: This service previously contained deprecated methods for creating Assignment.Subject
 * from applicableTo data. Those methods have been removed. Use
 * SettingValidationRuleCommandHandler.getCampaignIdFromCommand() instead for proper
 * campaign ID extraction from command.subject.
 */
@Service
public class CommandMappingService {

    private static final Logger logger = LoggerFactory.getLogger(CommandMappingService.class);

    /**
     * Calculate applicability statistics for event reporting.
     * <p>
     * This method analyzes the applicability scope and returns statistics about
     * included/excluded items. Note that this is for reporting purposes only.
     * The actual rule applicability is determined by the product.applicability.in
     * operator in the rule tree.
     */
    public ApplicabilityStats calculateApplicabilityStats(ApplicabilityScope applicableToData) {
        if (applicableToData == null) {
            return new ApplicabilityStats(0, 0, false);
        }

        try {
            List<ApplicabilityRule> included = applicableToData.getIncluded();
            List<ApplicabilityRule> excluded = applicableToData.getExcluded();
            Boolean includedAll = applicableToData.getIncludedAll();

            int includedCount = included != null ? included.size() : 0;
            int excludedCount = excluded != null ? excluded.size() : 0;

            return new ApplicabilityStats(
                    includedCount,
                    excludedCount,
                    includedAll != null ? includedAll : false
            );

        } catch (Exception e) {
            logger.error("Error calculating applicability stats", e);
            return new ApplicabilityStats(0, 0, false);
        }
    }

    /**
     * Applicability statistics for event reporting.
     * Contains metrics about the scope of rule applicability.
     */
    public static class ApplicabilityStats {
        private final int includedItemsCount;
        private final int excludedItemsCount;
        private final boolean includedAll;

        public ApplicabilityStats(int includedItemsCount, int excludedItemsCount, boolean includedAll) {
            this.includedItemsCount = includedItemsCount;
            this.excludedItemsCount = excludedItemsCount;
            this.includedAll = includedAll;
        }

        public int getIncludedItemsCount() {
            return includedItemsCount;
        }

        public int getExcludedItemsCount() {
            return excludedItemsCount;
        }

        public boolean isIncludedAll() {
            return includedAll;
        }

        @Override
        public String toString() {
            return String.format("ApplicabilityStats{included=%d, excluded=%d, includedAll=%b}",
                    includedItemsCount, excludedItemsCount, includedAll);
        }
    }
}