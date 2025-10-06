package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity;

import com.promix.platform.jpa.converter.ListStringConverter;
import com.promix.platform.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "publish_jobs", indexes = {
        @Index(name = "idx_publish_jobs_rule_ver", columnList = "tenant_id, rule_id, target_version"),
        @Index(name = "idx_publish_jobs_status_time", columnList = "tenant_id, status, requested_at")
})
public class PublishJobEntity extends BaseEntity {

    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    @Column(name = "rule_id", nullable = false, length = 100)
    private String ruleId;

    @Column(name = "target_version", nullable = false)
    private Integer targetVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private JobStatus status;

    @Column(name = "requested_by", length = 100)
    private String requestedBy;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Embedded
    private CompileJobInfoEmbeddable compile;

    @Convert(converter = ListStringConverter.class)
    @Column(name = "errors", columnDefinition = "TEXT")
    private List<String> errors;

    public enum JobStatus {
        RUNNING, SUCCESS, FAILED
    }

    @Embeddable
    @Getter
    @Setter
    public static class CompileJobInfoEmbeddable {
        @Column(name = "compiler_id", length = 100)
        private String compilerId;

        @Column(name = "operators_fingerprint", length = 200)
        private String operatorsFingerprint;

        @Convert(converter = ListStringConverter.class)
        @Column(name = "compile_logs", columnDefinition = "TEXT")
        private List<String> logs;

        @Column(name = "bundle_hash", length = 200)
        private String bundleHash;
    }
}
