package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.mapper.RuleBindingMapper;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.RuleBindingJpaRepository;
import vn.viettel.vds.promotion.validation.application.port.out.CampaignArchivePort;
import vn.viettel.vds.promotion.validation.domain.model.RuleBinding;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Xoá mềm rule binding của chiến dịch khi lưu trữ (PRM_KBNV_EVT_ARCH_002 Bước 5, Mục 4.1).
 *
 * <p>Xoá mềm = chép nguyên bản ghi sang {@code rule_bindings_deleted} với {@code deleted_at},
 * {@code deleted_by = SYSTEM_ARCHIVE} rồi xoá khỏi {@code rule_bindings}, trong cùng một transaction riêng cho
 * mỗi lô (lỗi giữa chừng thì các lô đã xong giữ nguyên). Danh sách cột chép lấy từ {@code information_schema}
 * (phần chung của hai bảng) để không phụ thuộc thứ tự cột.
 */
@Slf4j
@Component
public class CampaignArchiveJpaAdapter implements CampaignArchivePort {

    @PersistenceContext
    private EntityManager entityManager;

    private final RuleBindingJpaRepository bindingRepository;
    private final RuleBindingMapper bindingMapper;
    private final TransactionTemplate newTransaction;
    private final ObjectMapper objectMapper;

    public CampaignArchiveJpaAdapter(RuleBindingJpaRepository bindingRepository, RuleBindingMapper bindingMapper,
                                     PlatformTransactionManager transactionManager, ObjectMapper objectMapper) {
        this.bindingRepository = bindingRepository;
        this.bindingMapper = bindingMapper;
        this.newTransaction = new TransactionTemplate(transactionManager);
        this.newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.objectMapper = objectMapper;
    }

    @Override
    public List<RuleBinding> findBindingsOfCampaign(String campaignId) {
        return newTransaction.execute(status -> {
            List<RuleBinding> bindings = new ArrayList<>();
            for (String objectType : List.of("CAMPAIGN", "DISCOUNT_COUPON")) {
                bindings.addAll(bindingMapper.toDomainList(
                        bindingRepository.findByObjectTypeAndObjectId(objectType, campaignId)));
            }
            return bindings;
        });
    }

    @Override
    public int softDeleteBindings(List<String> bindingIds, Instant deletedAt) {
        if (bindingIds == null || bindingIds.isEmpty()) {
            return 0;
        }
        String columns = String.join(", ", commonColumns());
        Integer moved = newTransaction.execute(status -> {
            entityManager.createNativeQuery(
                            "INSERT INTO rule_bindings_deleted (" + columns + ", deleted_at, deleted_by) "
                                    + "SELECT " + columns + ", :deletedAt, :deletedBy FROM rule_bindings "
                                    + "WHERE id IN (:ids)")
                    .setParameter("deletedAt", Timestamp.from(deletedAt))
                    .setParameter("deletedBy", ARCHIVE_ACTOR)
                    .setParameter("ids", bindingIds)
                    .executeUpdate();
            return entityManager.createNativeQuery("DELETE FROM rule_bindings WHERE id IN (:ids)")
                    .setParameter("ids", bindingIds)
                    .executeUpdate();
        });
        return moved != null ? moved : 0;
    }

    /** Cột có ở cả bảng gốc lẫn bảng xoá mềm, theo thứ tự bảng gốc; trừ deleted_at/deleted_by (ghi riêng). */
    private List<String> commonColumns() {
        List<String> base = columnsOf("rule_bindings");
        Set<String> shadow = new LinkedHashSet<>(columnsOf("rule_bindings_deleted"));
        List<String> common = new ArrayList<>();
        for (String column : base) {
            if (shadow.contains(column) && !"deleted_at".equals(column) && !"deleted_by".equals(column)) {
                common.add(column);
            }
        }
        if (common.isEmpty()) {
            throw new IllegalStateException("Không xác định được cột của bảng rule_bindings_deleted");
        }
        return common;
    }

    private List<String> columnsOf(String table) {
        return newTransaction.execute(status -> {
            List<?> rows = entityManager.createNativeQuery(
                            "SELECT column_name FROM information_schema.columns "
                                    + "WHERE table_schema = DATABASE() AND table_name = :table ORDER BY ordinal_position")
                    .setParameter("table", table)
                    .getResultList();
            List<String> columns = new ArrayList<>(rows.size());
            rows.forEach(row -> columns.add(row.toString()));
            return columns;
        });
    }

    @Override
    public Optional<StoredResult> findResult(String commandId) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = newTransaction.execute(status -> entityManager.createNativeQuery(
                        "SELECT command_id, campaign_id, result, deleted_counts, error_message "
                                + "FROM campaign_archive_command_log WHERE command_id = :commandId")
                .setParameter("commandId", commandId)
                .getResultList());
        if (rows == null || rows.isEmpty()) {
            return Optional.empty();
        }
        Object[] row = rows.get(0);
        return Optional.of(new StoredResult((String) row[0], (String) row[1], (String) row[2],
                readCounts((String) row[3]), (String) row[4]));
    }

    @Override
    public void saveResult(StoredResult result) {
        newTransaction.executeWithoutResult(status -> entityManager.createNativeQuery(
                        "INSERT INTO campaign_archive_command_log "
                                + "(command_id, campaign_id, result, deleted_counts, error_message, created_at) "
                                + "VALUES (:commandId, :campaignId, :result, :counts, :error, :createdAt)")
                .setParameter("commandId", result.commandId())
                .setParameter("campaignId", result.campaignId())
                .setParameter("result", result.result())
                .setParameter("counts", writeCounts(result.softDeletedCounts()))
                .setParameter("error", result.errorMessage())
                .setParameter("createdAt", Timestamp.from(Instant.now()))
                .executeUpdate());
    }

    private String writeCounts(Map<String, Long> counts) {
        try {
            return objectMapper.writeValueAsString(counts != null ? counts : Map.of());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Không ghi được số bản ghi đã xoá mềm", e);
        }
    }

    private Map<String, Long> readCounts(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, Long>>() {
            });
        } catch (JsonProcessingException e) {
            log.warn("Không đọc được số bản ghi đã xoá mềm đã lưu: {}", json);
            return Map.of();
        }
    }
}
