package vn.viettel.vds.promotion.validation.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import vn.viettel.vds.promotion.schema.validation.command.ApplicabilityRule;
import vn.viettel.vds.promotion.schema.validation.command.ApplicabilityScope;
import vn.viettel.vds.promotion.validation.domain.entity.Assignment;

import java.util.List;

/**
 * Service for mapping command data to domain entities
 */
@Service
public class CommandMappingService {

    private static final Logger logger = LoggerFactory.getLogger(CommandMappingService.class);

    /**
     * Create Assignment.Subject from applicableTo command data
     */
    public Assignment.Subject createSubjectFromApplicableTo(ApplicabilityScope applicableToData) {
        if (applicableToData == null) {
            logger.warn("ApplicableTo data is null, creating default subject");
            Assignment.Subject subject = new Assignment.Subject();
            subject.setType("campaign");
            subject.setKey("default");
            return subject;
        }

        try {
            // For now, we'll extract the primary subject from applicableTo
            // In a full implementation, this might need to handle complex applicability rules

            String subjectType = determineSubjectType(applicableToData);
            String subjectKey = determineSubjectKey(applicableToData);

            Assignment.Subject subject = new Assignment.Subject();
            subject.setType(subjectType);
            subject.setKey(subjectKey);
            return subject;

        } catch (Exception e) {
            logger.error("Error creating subject from applicableTo data", e);
            Assignment.Subject subject = new Assignment.Subject();
            subject.setType("campaign");
            subject.setKey("unknown");
            return subject;
        }
    }

    /**
     * Determine subject type from applicableTo data
     */
    private String determineSubjectType(ApplicabilityScope applicableToData) {
        // Check if includedAll is true - applies to everything
        Boolean includedAll = applicableToData.getIncludedAll();
        if (includedAll != null && includedAll) {
            return "campaign"; // Global rule
        }

        // Check included items for primary subject type
        List<ApplicabilityRule> included = applicableToData.getIncluded();
        if (included != null && !included.isEmpty()) {
            ApplicabilityRule firstIncluded = included.get(0);
            if (firstIncluded.getObject() != null) {
                return firstIncluded.getObject().toString().toLowerCase();
            }
        }

        // Default to campaign level
        return "campaign";
    }

    /**
     * Determine subject key from applicableTo data
     */
    private String determineSubjectKey(ApplicabilityScope applicableToData) {
        // Check if includedAll is true
        Boolean includedAll = applicableToData.getIncludedAll();
        if (includedAll != null && includedAll) {
            return "all";
        }

        // Get the first included item's ID as primary key
        List<ApplicabilityRule> included = applicableToData.getIncluded();
        if (included != null && !included.isEmpty()) {
            ApplicabilityRule firstIncluded = included.get(0);
            if (firstIncluded.getId() != null) {
                return firstIncluded.getId().toString();
            }
        }

        // Default key
        return "default";
    }

    /**
     * Calculate applicability statistics for event reporting
     */
    public ApplicabilityStats calculateApplicabilityStats(ApplicabilityScope applicableToData) {
        if (applicableToData == null) {
            return new ApplicabilityStats(0, 0, false, "campaign", "default");
        }

        try {
            List<ApplicabilityRule> included = applicableToData.getIncluded();
            List<ApplicabilityRule> excluded = applicableToData.getExcluded();
            Boolean includedAll = applicableToData.getIncludedAll();

            int includedCount = included != null ? included.size() : 0;
            int excludedCount = excluded != null ? excluded.size() : 0;

            String subjectType = determineSubjectType(applicableToData);
            String subjectKey = determineSubjectKey(applicableToData);

            return new ApplicabilityStats(includedCount, excludedCount,
                    includedAll != null ? includedAll : false, subjectType, subjectKey);

        } catch (Exception e) {
            logger.error("Error calculating applicability stats", e);
            return new ApplicabilityStats(0, 0, false, "campaign", "error");
        }
    }

    /**
     * Applicability statistics for event reporting
     */
    public static class ApplicabilityStats {
        private final int includedItemsCount;
        private final int excludedItemsCount;
        private final boolean includedAll;
        private final String subjectType;
        private final String subjectKey;

        public ApplicabilityStats(int includedItemsCount, int excludedItemsCount, boolean includedAll,
                                String subjectType, String subjectKey) {
            this.includedItemsCount = includedItemsCount;
            this.excludedItemsCount = excludedItemsCount;
            this.includedAll = includedAll;
            this.subjectType = subjectType;
            this.subjectKey = subjectKey;
        }

        public int getIncludedItemsCount() { return includedItemsCount; }
        public int getExcludedItemsCount() { return excludedItemsCount; }
        public boolean isIncludedAll() { return includedAll; }
        public String getSubjectType() { return subjectType; }
        public String getSubjectKey() { return subjectKey; }
    }
}