package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.adapter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.ValidationRuleAssignmentDeletedEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.ValidationRuleAssignmentEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.mapper.ValidationRuleAssignmentDeletedEntityMapper;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.mapper.ValidationRuleAssignmentEntityMapper;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.ValidationRuleAssignmentDeletedJpaRepository;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.ValidationRuleAssignmentJpaRepository;
import vn.viettel.vds.promotion.validation.application.port.out.ValidationRuleAssignmentPort;
import vn.viettel.vds.promotion.validation.domain.model.ValidationRuleAssignment;
import vn.viettel.vds.promotion.validation.domain.model.ValidationRuleAssignmentDeleted;

import java.util.Optional;

/**
 * JPA Adapter implementation cho ValidationRuleAssignmentPort.
 * Component này implement outbound port để kết nối application layer với persistence layer.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ValidationRuleAssignmentJpaAdapter implements ValidationRuleAssignmentPort {

    private final ValidationRuleAssignmentJpaRepository assignmentRepository;
    private final ValidationRuleAssignmentDeletedJpaRepository deletedRepository;
    private final ValidationRuleAssignmentEntityMapper assignmentMapper;
    private final ValidationRuleAssignmentDeletedEntityMapper deletedMapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<ValidationRuleAssignment> findByValidationRuleIdAndObjectId(
            String validationRuleId,
            String objectId) {

        log.debug("Finding assignment: validationRuleId={}, objectId={}", validationRuleId, objectId);

        return assignmentRepository
                .findByValidationRuleIdAndObjectIdAndDeletedFalse(validationRuleId, objectId)
                .map(entity -> {
                    ValidationRuleAssignment domain = assignmentMapper.toDomain(entity);
                    log.debug("Found assignment: id={}", domain.getId());
                    return domain;
                });
    }

    @Override
    @Transactional
    public ValidationRuleAssignment save(ValidationRuleAssignment assignment) {
        log.debug("Saving assignment: id={}, validationRuleId={}, objectId={}",
                assignment.getId(), assignment.getValidationRuleId(), assignment.getObjectId());

        ValidationRuleAssignmentEntity entity = assignmentMapper.toEntity(assignment);
        ValidationRuleAssignmentEntity savedEntity = assignmentRepository.save(entity);

        ValidationRuleAssignment savedDomain = assignmentMapper.toDomain(savedEntity);
        log.info("Assignment saved successfully: id={}", savedDomain.getId());

        return savedDomain;
    }

    @Override
    @Transactional
    public void delete(ValidationRuleAssignment assignment) {
        log.debug("Deleting assignment (hard delete): id={}", assignment.getId());

        ValidationRuleAssignmentEntity entity = assignmentMapper.toEntity(assignment);
        assignmentRepository.delete(entity);

        log.info("Assignment deleted successfully: id={}", assignment.getId());
    }

    @Override
    @Transactional
    public ValidationRuleAssignmentDeleted saveDeleted(ValidationRuleAssignmentDeleted deleted) {
        log.debug("Saving deleted assignment to archive: assignmentId={}", deleted.getAssignmentId());

        ValidationRuleAssignmentDeletedEntity entity = deletedMapper.toEntity(deleted);
        ValidationRuleAssignmentDeletedEntity savedEntity = deletedRepository.save(entity);

        ValidationRuleAssignmentDeleted savedDomain = deletedMapper.toDomain(savedEntity);
        log.info("Deleted assignment archived successfully: id={}, assignmentId={}",
                savedDomain.getId(), savedDomain.getAssignmentId());

        return savedDomain;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean exists(String validationRuleId, String objectId) {
        log.debug("Checking if assignment exists: validationRuleId={}, objectId={}",
                validationRuleId, objectId);

        boolean exists = assignmentRepository.existsByValidationRuleIdAndObjectIdAndDeletedFalse(
                validationRuleId, objectId);

        log.debug("Assignment exists: {}", exists);
        return exists;
    }
}
