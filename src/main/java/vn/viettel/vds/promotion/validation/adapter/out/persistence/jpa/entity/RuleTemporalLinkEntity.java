package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity;

import com.promix.platform.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "rule_temporal_links", indexes = {
        @Index(name = "idx_rule_temporal_links_rule_id", columnList = "validation_rule_id"),
        @Index(name = "idx_rule_temporal_links_policy_id", columnList = "temporal_policy_id")
})
public class RuleTemporalLinkEntity extends BaseEntity {

    @Column(name = "mode", nullable = false, length = 20)
    private String mode; // "ALLOW" | "DENY"

    // Many-to-one relationship with validation rule
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validation_rule_id", nullable = false)
    private ValidationRuleEntity validationRule;

    // Many-to-one relationship with temporal policy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "temporal_policy_id", nullable = false)
    private TemporalPolicyEntity temporalPolicy;

    public RuleTemporalLinkEntity() {
        super();
    }

    public RuleTemporalLinkEntity(String mode) {
        super();
        this.mode = mode;
    }
}