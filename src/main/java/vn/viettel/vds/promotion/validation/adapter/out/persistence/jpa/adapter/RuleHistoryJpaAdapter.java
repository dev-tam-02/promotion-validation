package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.promix.platform.data.jpa.autoconfigure.condition.ConditionalOnPromixJpa;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.ValidationRuleHistoryEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.RuleHistoryJpaRepository;
import vn.viettel.vds.promotion.validation.application.port.out.RuleHistoryPersistencePort;
import vn.viettel.vds.promotion.validation.domain.model.RuleHistoryEntry;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * JPA adapter for the {@link RuleHistoryPersistencePort}.
 *
 * <p>Maps between the {@link RuleHistoryEntry} domain object and
 * {@link ValidationRuleHistoryEntity} JPA entity. The {@code dslSnapshot} field
 * is serialised to/from JSON string via Jackson.
 */
@Component
@ConditionalOnPromixJpa
public class RuleHistoryJpaAdapter implements RuleHistoryPersistencePort {

    private static final Logger log = LoggerFactory.getLogger(RuleHistoryJpaAdapter.class);

    private final RuleHistoryJpaRepository repository;
    private final ObjectMapper objectMapper;

    public RuleHistoryJpaAdapter(RuleHistoryJpaRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void save(RuleHistoryEntry entry) {
        ValidationRuleHistoryEntity entity = toEntity(entry);
        repository.save(entity);
        log.debug("Saved rule history: ruleId={}, version={}, changeType={}",
                entry.getRuleId(), entry.getRuleVersion(), entry.getChangeType());
    }

    @Override
    public List<RuleHistoryEntry> findByRuleId(String ruleId) {
        return repository.findByRuleIdOrderByRuleVersionAsc(ruleId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<RuleHistoryEntry> findByRuleIdAndVersion(String ruleId, long ruleVersion) {
        return repository.findByRuleIdAndRuleVersion(ruleId, ruleVersion)
                .map(this::toDomain);
    }

    // ------------------------------------------------------------------

    private ValidationRuleHistoryEntity toEntity(RuleHistoryEntry entry) {
        String dslJson = null;
        if (entry.getDslSnapshot() != null) {
            try {
                dslJson = objectMapper.writeValueAsString(entry.getDslSnapshot());
            } catch (JsonProcessingException ex) {
                log.warn("Failed to serialise dslSnapshot for ruleId={}: {}", entry.getRuleId(), ex.getMessage());
            }
        }

        return ValidationRuleHistoryEntity.builder()
                .id(entry.getId())
                .ruleId(entry.getRuleId())
                .ruleVersion(entry.getRuleVersion())
                .changeType(entry.getChangeType().name())
                .changedBy(entry.getChangedBy())
                .changedAt(entry.getChangedAt())
                .dslSnapshot(dslJson)
                .bundleHash(entry.getBundleHash())
                .state(entry.getState())
                .changeReason(entry.getChangeReason())
                .build();
    }

    private RuleHistoryEntry toDomain(ValidationRuleHistoryEntity entity) {
        Map<String, Object> dslMap = null;
        if (entity.getDslSnapshot() != null) {
            try {
                dslMap = objectMapper.readValue(entity.getDslSnapshot(),
                        new TypeReference<Map<String, Object>>() {});
            } catch (JsonProcessingException ex) {
                log.warn("Failed to deserialise dslSnapshot for id={}: {}", entity.getId(), ex.getMessage());
            }
        }

        return new RuleHistoryEntry(
                entity.getId(),
                entity.getRuleId(),
                entity.getRuleVersion() != null ? entity.getRuleVersion() : 0L,
                parseChangeType(entity.getChangeType()),
                entity.getChangedBy(),
                entity.getChangedAt(),
                dslMap,
                entity.getBundleHash(),
                entity.getState(),
                entity.getChangeReason()
        );
    }

    private RuleHistoryEntry.ChangeType parseChangeType(String value) {
        try {
            return RuleHistoryEntry.ChangeType.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException ex) {
            return RuleHistoryEntry.ChangeType.UPDATE;
        }
    }
}
