package vn.viettel.vds.promotion.validation.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.viettel.vds.promotion.validation.adapter.in.web.dto.ApplicabilityRuleResponse;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.AssignmentApplicabilityRuleEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.AssignmentApplicabilityRuleJpaRepository;

import java.util.List;

/**
 * Service for managing applicability rules.
 * Provides methods to query applicability rules by object type and object ID.
 */
@Service
@Transactional(readOnly = true)
public class ApplicabilityRuleService {

    private static final Logger logger = LoggerFactory.getLogger(ApplicabilityRuleService.class);

    private final AssignmentApplicabilityRuleJpaRepository applicabilityRuleRepository;

    public ApplicabilityRuleService(AssignmentApplicabilityRuleJpaRepository applicabilityRuleRepository) {
        this.applicabilityRuleRepository = applicabilityRuleRepository;
    }

    /**
     * Find all applicability rules by object type and object ID.
     *
     * @param objectType the object type (COLLECTION, PRODUCT, SKU)
     * @param objectId   the object identifier
     * @return list of applicability rule responses
     */
    public List<ApplicabilityRuleResponse> findByObjectTypeAndObjectId(String objectType, String objectId) {
        logger.debug("Finding applicability rules for objectType={}, objectId={}", objectType, objectId);

        List<AssignmentApplicabilityRuleEntity> entities = applicabilityRuleRepository
                .findByObjectTypeAndObjectId(objectType, objectId);

        logger.debug("Found {} applicability rules for objectType={}, objectId={}",
                entities.size(), objectType, objectId);

        return entities.stream()
                .map(this::toResponse)
                .toList();
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
