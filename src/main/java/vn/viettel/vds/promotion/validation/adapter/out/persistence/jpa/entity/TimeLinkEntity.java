package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity;

import com.promix.platform.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "time_links", indexes = {
        @Index(name = "idx_time_links_rule_version", columnList = "rule_version_id")
})
public class TimeLinkEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_version_id", nullable = false)
    private RuleVersionEntity ruleVersion;

    @Column(name = "policy_id", nullable = false, length = 100)
    private String policyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false, length = 20)
    private TimeLinkMode mode;

    public enum TimeLinkMode {
        ALLOW, DENY
    }
}
