package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity;

import com.promix.platform.jpa.converter.ListStringConverter;
import com.promix.platform.jpa.converter.MapStringObjectConverter;
import com.promix.platform.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Entity
@Table(name = "rule_versions", indexes = {
        @Index(name = "idx_rule_versions_desc", columnList = "tenant_id, rule_id, rule_version", unique = true),
        @Index(name = "idx_rule_versions_bundle_hash", columnList = "tenant_id, bundle_hash"),
        @Index(name = "idx_rule_versions_code_version", columnList = "tenant_id, code, rule_version")
})
public class RuleVersionEntity extends BaseEntity {

    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    @Column(name = "rule_id", nullable = false, length = 100)
    private String ruleId;

    @Column(name = "code", nullable = false, length = 100)
    private String code;

    @Column(name = "rule_version", nullable = false)
    private Integer ruleVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "logic", length = 20)
    private LogicType logic;

    @Convert(converter = MapStringObjectConverter.class)
    @Column(name = "limits", columnDefinition = "TEXT")
    private Map<String, Object> limits;

    @Convert(converter = MapStringObjectConverter.class)
    @Column(name = "nodes", columnDefinition = "TEXT")
    private List<Map<String, Object>> nodes;

    @Column(name = "operators_fingerprint", length = 200)
    private String operatorsFingerprint;

    @OneToMany(mappedBy = "ruleVersion", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<TimeLinkEntity> timeLinks = new ArrayList<>();

    @Convert(converter = MapStringObjectConverter.class)
    @Column(name = "dsl", columnDefinition = "TEXT")
    private Map<String, Object> dsl;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "published_by", length = 100)
    private String publishedBy;

    @Embedded
    private CompileInfoEmbeddable compile;

    public enum LogicType {
        ALL, ANY, NONE
    }

    @Embeddable
    @Getter
    @Setter
    public static class CompileInfoEmbeddable {
        @Enumerated(EnumType.STRING)
        @Column(name = "compile_status", length = 20)
        private CompileStatus status;

        @Column(name = "compiler_id", length = 100)
        private String compilerId;

        @Column(name = "bundle_hash", length = 200)
        private String bundleHash;

        @Convert(converter = ListStringConverter.class)
        @Column(name = "compile_logs", columnDefinition = "TEXT")
        private List<String> logs;

        public enum CompileStatus {
            SUCCESS, FAILED
        }
    }
}
