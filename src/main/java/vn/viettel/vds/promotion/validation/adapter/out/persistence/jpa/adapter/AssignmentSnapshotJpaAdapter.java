package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.adapter;

import com.promix.platform.data.jpa.autoconfigure.condition.ConditionalOnPromixJpa;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.AssignmentSnapshotEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.mapper.AssignmentSnapshotMapper;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.AssignmentSnapshotRepository;
import vn.viettel.vds.promotion.validation.application.port.out.AssignmentSnapshotPersistencePort;
import vn.viettel.vds.promotion.validation.domain.model.AssignmentSnapshot;

import java.time.Instant;
import java.util.Optional;

/**
 * JPA adapter implementing AssignmentSnapshotPersistencePort.
 * Converts between AssignmentSnapshot domain model and JPA entity.
 */
@Component
@ConditionalOnPromixJpa
public class AssignmentSnapshotJpaAdapter implements AssignmentSnapshotPersistencePort {

    private final AssignmentSnapshotRepository repository;
    private final AssignmentSnapshotMapper mapper;

    public AssignmentSnapshotJpaAdapter(AssignmentSnapshotRepository repository,
                                        AssignmentSnapshotMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public AssignmentSnapshot save(AssignmentSnapshot snapshot) {
        AssignmentSnapshotEntity entity = mapper.toEntity(snapshot);
        AssignmentSnapshotEntity saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<AssignmentSnapshot> findById(String id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<AssignmentSnapshot> findByAssignmentIdAndSnapshotVersion(String assignmentId, Long snapshotVersion) {
        return repository.findByAssignmentIdAndSnapshotVersion(assignmentId, snapshotVersion)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Long> findLatestVersionByAssignmentId(String assignmentId) {
        return repository.findLatestVersionByAssignmentId(assignmentId);
    }

    @Override
    public boolean existsByAssignmentIdAndSnapshotVersion(String assignmentId, Long snapshotVersion) {
        return repository.existsByAssignmentIdAndSnapshotVersion(assignmentId, snapshotVersion);
    }

    @Override
    @Transactional
    public int deleteExpiredSnapshots(Instant expirationTime) {
        return repository.deleteExpiredSnapshots(expirationTime);
    }

    @Override
    public void delete(AssignmentSnapshot snapshot) {
        repository.deleteById(snapshot.getId());
    }

    @Override
    public void deleteById(String id) {
        repository.deleteById(id);
    }
}
