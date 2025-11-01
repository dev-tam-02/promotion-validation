package vn.viettel.vds.promotion.validation.application.service;

import com.promix.platform.core.exception.factory.ExceptionFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.viettel.vds.promotion.validation.adapter.in.web.dto.ValidationRuleAssignmentResponse;
import vn.viettel.vds.promotion.validation.application.port.in.GetValidationRuleAssignmentUseCase;
import vn.viettel.vds.promotion.validation.application.port.out.ValidationRuleAssignmentPort;
import vn.viettel.vds.promotion.validation.domain.model.ValidationRuleAssignment;

/**
 * Service implementation cho việc lấy validation rule assignment details.
 * Cung cấp thông tin đầy đủ về assignment cho external services.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GetValidationRuleAssignmentService implements GetValidationRuleAssignmentUseCase {

    private final ValidationRuleAssignmentPort assignmentPort;

    @Override
    @Transactional(readOnly = true)
    public ValidationRuleAssignmentResponse getAssignment(String objectType, String objectId) {
        log.info("Getting validation rule assignment for objectType: {}, objectId: {}", objectType, objectId);

        // Tìm assignment theo object type và object ID
        ValidationRuleAssignment assignment = assignmentPort.findByObjectTypeAndObjectId(objectType, objectId)
                .orElseThrow(() -> {
                    log.warn("No validation rule assignment found for objectType: {}, objectId: {}",
                            objectType, objectId);
                    return ExceptionFactory.createNotFound(
                            "VALIDATION_RULE_ASSIGNMENT_NOT_FOUND",
                            String.format("No validation rule assignment found for %s with ID: %s",
                                    objectType, objectId)
                    );
                });

        log.debug("Successfully retrieved validation rule assignment - assignmentId: {}, validationRuleId: {}, objectType: {}, objectId: {}",
                assignment.getId(),
                assignment.getValidationRuleId(),
                objectType,
                objectId);

        // Map to response DTO
        // Note: Validation rule name, priority, notes sẽ cần được lấy từ ValidationRule entity
        // Để đơn giản, tạm thời return với data có sẵn trong assignment
        return new ValidationRuleAssignmentResponse(
                assignment.getId(),
                assignment.getValidationRuleId(),
                null, // validationRuleName - TODO: join with ValidationRule entity
                assignment.getObjectType(),
                assignment.getObjectId(),
                true, // active - TODO: get from assignment when field exists
                100, // trafficPercent - TODO: get from assignment when field exists
                1, // priority - TODO: get from assignment when field exists
                null, // notes - TODO: get from assignment when field exists
                assignment.getCreatedAt(),
                assignment.getLastModifiedAt(),
                assignment.getCreatedBy(),
                assignment.getLastModifiedBy()
        );
    }
}
