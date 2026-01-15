package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.adapter;

import com.promix.platform.data.jpa.autoconfigure.condition.ConditionalOnPromixJpa;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.RuleTimeFrameEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.mapper.RuleTimeFrameMapper;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.RuleTimeFrameJpaRepository;
import vn.viettel.vds.promotion.validation.application.port.out.RuleTimeFramePersistencePort;
import vn.viettel.vds.promotion.validation.domain.model.RuleTimeFrame;

import java.util.List;
import java.util.Optional;

/**
 * JPA adapter implementing RuleTimeFramePersistencePort.
 * Converts between RuleTimeFrame domain model and JPA entities.
 * <p>
 * NOTE: This is legacy code kept for backward compatibility.
 */
@Component
@ConditionalOnPromixJpa
public class RuleTimeFrameJpaAdapter implements RuleTimeFramePersistencePort {

    private final RuleTimeFrameJpaRepository repository;
    private final RuleTimeFrameMapper mapper;

    public RuleTimeFrameJpaAdapter(RuleTimeFrameJpaRepository repository,
                                   RuleTimeFrameMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public RuleTimeFrame save(RuleTimeFrame ruleTimeFrame) {
        RuleTimeFrameEntity entity = mapper.toEntity(ruleTimeFrame);
        RuleTimeFrameEntity saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<RuleTimeFrame> findById(String id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<RuleTimeFrame> findByValidationRuleId(String ruleId) {
        List<RuleTimeFrameEntity> entities = repository.findByValidationRuleId(ruleId);
        return mapper.toDomainList(entities);
    }

    @Override
    public List<RuleTimeFrame> findByTimeFrameId(String timeFrameId) {
        List<RuleTimeFrameEntity> entities = repository.findByTimeFrameId(timeFrameId);
        return mapper.toDomainList(entities);
    }

    @Override
    public List<RuleTimeFrame> findByValidationRuleIdAndMode(String ruleId, String mode) {
        List<RuleTimeFrameEntity> entities = repository.findByValidationRuleIdAndMode(ruleId, mode);
        return mapper.toDomainList(entities);
    }

    @Override
    public void deleteByValidationRuleId(String ruleId) {
        repository.deleteByValidationRuleId(ruleId);
    }

    @Override
    public void deleteById(String id) {
        repository.deleteById(id);
    }
}
