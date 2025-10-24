package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.adapter;

import com.promix.platform.data.jpa.autoconfigure.condition.ConditionalOnPromixJpa;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.PublishJobEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.mapper.PublishJobEntityMapper;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.PublishJobJpaRepository;
import vn.viettel.vds.promotion.validation.application.port.out.PublishJobPersistencePort;
import vn.viettel.vds.promotion.validation.domain.model.PublishJob;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
@ConditionalOnPromixJpa
public class PublishJobJpaAdapter implements PublishJobPersistencePort {

    private final PublishJobJpaRepository repository;
    private final PublishJobEntityMapper mapper;

    public PublishJobJpaAdapter(PublishJobJpaRepository repository,
                                PublishJobEntityMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public PublishJob save(PublishJob publishJob) {
        PublishJobEntity entity = mapper.toEntity(publishJob);
        PublishJobEntity saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<PublishJob> findById(String id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<PublishJob> findByRuleIdAndTargetVersion(String ruleId, Integer targetVersion) {
        return repository.findByRuleIdAndTargetVersion(ruleId, targetVersion)
                .map(mapper::toDomain);
    }

    @Override
    public List<PublishJob> findByRuleIdOrderByTargetVersionDesc(String ruleId) {
        return repository.findByRuleIdOrderByTargetVersionDesc(ruleId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Page<PublishJob> findByStatusOrderByRequestedAtDesc(PublishJob.JobStatus status, Pageable pageable) {
        // Map enum
        PublishJobEntity.JobStatus entityStatus = PublishJobEntity.JobStatus.valueOf(status.name());

        List<PublishJobEntity> all = repository.findByStatusOrderByRequestedAtDesc(entityStatus);
        return convertToPage(all, pageable);
    }

    @Override
    public List<PublishJob> findRunningJobsOlderThan(Instant cutoffTime) {
        // JPA doesn't have this query - filter manually
        PublishJobEntity.JobStatus running = PublishJobEntity.JobStatus.RUNNING;
        return repository.findAll().stream()
                .filter(e -> running.equals(e.getStatus()) &&
                        e.getRequestedAt() != null &&
                        e.getRequestedAt().isBefore(cutoffTime))
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<PublishJob> findFirstByRuleIdOrderByRequestedAtDesc(String ruleId) {
        // Manual filtering and sorting
        return repository.findByRuleIdOrderByTargetVersionDesc(ruleId).stream()
                .max((e1, e2) -> e1.getRequestedAt().compareTo(e2.getRequestedAt()))
                .map(mapper::toDomain);
    }

    @Override
    public Page<PublishJob> findWithFilters(PublishJob.JobStatus status, Instant from, Instant to, Pageable pageable) {
        // Manual filtering
        List<PublishJobEntity> all = repository.findAll();
        List<PublishJobEntity> filtered = all.stream()
                .filter(e -> status == null ||
                        (e.getStatus() != null && e.getStatus().name().equals(status.name())))
                .filter(e -> e.getRequestedAt() == null ||
                        (e.getRequestedAt().isAfter(from) && e.getRequestedAt().isBefore(to)))
                .toList();
        return convertToPage(filtered, pageable);
    }

    @Override
    public long countByStatus(PublishJob.JobStatus status) {
        // JPA doesn't have this exact method
        PublishJobEntity.JobStatus entityStatus = PublishJobEntity.JobStatus.valueOf(status.name());
        return repository.findByStatusOrderByRequestedAtDesc(entityStatus).size();
    }

    @Override
    public List<PublishJob> findJobsForCleanup(Instant cutoffTime) {
        // Filter completed jobs older than cutoff
        return repository.findAll().stream()
                .filter(e -> (PublishJobEntity.JobStatus.SUCCESS.equals(e.getStatus()) ||
                        PublishJobEntity.JobStatus.FAILED.equals(e.getStatus())) &&
                        e.getCompletedAt() != null &&
                        e.getCompletedAt().isBefore(cutoffTime))
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Page<PublishJob> findAllOrderByRequestedAtDesc(Pageable pageable) {
        // Manual filtering and sorting
        List<PublishJobEntity> all = repository.findAll().stream()
                .sorted((e1, e2) -> e2.getRequestedAt().compareTo(e1.getRequestedAt()))
                .toList();
        return convertToPage(all, pageable);
    }

    @Override
    public boolean existsByRuleIdAndTargetVersion(String ruleId, Integer targetVersion) {
        return repository.findByRuleIdAndTargetVersion(ruleId, targetVersion).isPresent();
    }

    @Override
    public void delete(PublishJob publishJob) {
        repository.deleteById(publishJob.getId());
    }

    @Override
    public void deleteById(String id) {
        repository.deleteById(id);
    }

    private Page<PublishJob> convertToPage(List<PublishJobEntity> entities, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), entities.size());
        List<PublishJob> pageContent = entities.subList(start, end)
                .stream()
                .map(mapper::toDomain)
                .toList();
        return new PageImpl<>(pageContent, pageable, entities.size());
    }
}
