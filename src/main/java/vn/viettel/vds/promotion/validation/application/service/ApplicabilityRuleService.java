package vn.viettel.vds.promotion.validation.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.viettel.vds.promotion.validation.adapter.in.web.dto.ApplicabilityRuleResponse;
import vn.viettel.vds.promotion.validation.application.port.out.ApplicabilityRulePersistencePort;
import vn.viettel.vds.promotion.validation.application.port.out.AssignmentPersistencePort;
import vn.viettel.vds.promotion.validation.domain.model.ApplicabilityRule;
import vn.viettel.vds.promotion.validation.domain.model.Assignment;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for managing applicability rules.
 * Provides methods to query applicability rules by assignment's entity type and entity ID.
 */
@Service
@Transactional(readOnly = true)
public class ApplicabilityRuleService {

    private static final Logger logger = LoggerFactory.getLogger(ApplicabilityRuleService.class);

    private final AssignmentPersistencePort assignmentPort;
    private final ApplicabilityRulePersistencePort applicabilityRulePort;

    public ApplicabilityRuleService(AssignmentPersistencePort assignmentPort,
                                    ApplicabilityRulePersistencePort applicabilityRulePort) {
        this.assignmentPort = assignmentPort;
        this.applicabilityRulePort = applicabilityRulePort;
    }

    /**
     * Find all applicability rules by assignment's entity type and entity ID.
     * Flow:
     * 1. Find assignments by entityType (objectType) and entityId (objectId)
     * 2. Get applicability rules from each assignment
     *
     * @param objectType the assignment's entity type (e.g., CAMPAIGN, PROMOTION)
     * @param objectId   the assignment's entity identifier
     * @return list of applicability rule responses
     */
    public List<ApplicabilityRuleResponse> findByObjectTypeAndObjectId(String objectType, String objectId) {
        logger.debug("Finding applicability rules for assignment with entityType={}, entityId={}", objectType, objectId);

        // Step 1: Find assignments by entityType (subjectType) and entityId (subjectKey)
        List<Assignment> assignments = assignmentPort.findAllBySubjectTypeAndSubjectKey(objectType, objectId);

        if (assignments.isEmpty()) {
            logger.debug("No assignments found for entityType={}, entityId={}", objectType, objectId);
            return List.of();
        }

        logger.debug("Found {} assignments for entityType={}, entityId={}",
                assignments.size(), objectType, objectId);

        // Step 2: Collect all applicability rules from all assignments via port
        List<ApplicabilityRuleResponse> result = new ArrayList<>();
        for (Assignment assignment : assignments) {
            List<ApplicabilityRule> rules = applicabilityRulePort.findByAssignmentId(assignment.getId());
            if (!rules.isEmpty()) {
                logger.debug("Assignment {} has {} applicability rules",
                        assignment.getId(), rules.size());
                for (ApplicabilityRule rule : rules) {
                    result.add(toResponse(rule));
                }
            }
        }

        logger.info("Found {} total applicability rules for entityType={}, entityId={}",
                result.size(), objectType, objectId);

        return result;
    }

    /**
     * Convert domain model to response DTO.
     */
    private ApplicabilityRuleResponse toResponse(ApplicabilityRule rule) {
        ApplicabilityRuleResponse response = new ApplicabilityRuleResponse();
        response.setId(rule.getId());
        response.setAssignmentId(rule.getAssignmentId());
        response.setRuleType(rule.getRuleType() != null ? rule.getRuleType().name() : null);
        response.setObjectType(rule.getObjectType() != null ? rule.getObjectType().name() : null);
        response.setObjectId(rule.getObjectId());
        response.setEffect(rule.getEffect() != null ? rule.getEffect().name() : null);
        response.setTarget(rule.getTarget() != null ? rule.getTarget().name() : null);
        response.setSkipInitially(rule.getSkipInitially());
        response.setRepeatCount(rule.getRepeatCount());
        response.setCreatedAt(rule.getCreatedAt());
        response.setUpdatedAt(rule.getUpdatedAt());
        return response;
    }
}
