package vn.viettel.vds.promotion.validation.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.viettel.vds.promotion.validation.adapter.in.web.dto.ApplicabilityRuleResponse;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.AssignmentApplicabilityRuleEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.AssignmentEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.AssignmentJpaRepository;

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

    private final AssignmentJpaRepository assignmentRepository;

    public ApplicabilityRuleService(AssignmentJpaRepository assignmentRepository) {
        this.assignmentRepository = assignmentRepository;
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

        // Step 1: Find assignments by entityType and entityId
        List<AssignmentEntity> assignments = assignmentRepository
                .findByEntityTypeAndEntityId(objectType, objectId);

        if (assignments.isEmpty()) {
            logger.debug("No assignments found for entityType={}, entityId={}", objectType, objectId);
            return List.of();
        }

        logger.debug("Found {} assignments for entityType={}, entityId={}",
                assignments.size(), objectType, objectId);

        // Step 2: Collect all applicability rules from all assignments
        List<ApplicabilityRuleResponse> result = new ArrayList<>();
        for (AssignmentEntity assignment : assignments) {
            List<AssignmentApplicabilityRuleEntity> rules = assignment.getApplicabilityRules();
            if (rules != null && !rules.isEmpty()) {
                logger.debug("Assignment {} has {} applicability rules",
                        assignment.getId(), rules.size());
                for (AssignmentApplicabilityRuleEntity rule : rules) {
                    result.add(toResponse(rule));
                }
            }
        }

        logger.info("Found {} total applicability rules for entityType={}, entityId={}",
                result.size(), objectType, objectId);

        return result;
    }

    /**
     * Convert entity to response DTO.
     */
    private ApplicabilityRuleResponse toResponse(AssignmentApplicabilityRuleEntity entity) {
        ApplicabilityRuleResponse response = new ApplicabilityRuleResponse();
        response.setId(entity.getId());
        response.setAssignmentId(entity.getAssignment() != null ? entity.getAssignment().getId() : null);
        response.setRuleType(entity.getRuleType());
        response.setObjectType(entity.getObjectType());
        response.setObjectId(entity.getObjectId());
        response.setEffect(entity.getEffect());
        response.setTarget(entity.getTarget());
        response.setSkipInitially(entity.getSkipInitially());
        response.setRepeatCount(entity.getRepeatCount());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }
}
