package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity;

import com.promix.platform.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "rule_usage_limits")
@EntityListeners(IdGenerationListener.class)
@EqualsAndHashCode(callSuper = true)
public class RuleUsageLimitsEntity extends BaseEntity {

    @Column(name = "per_code_total")
    private Integer perCodeTotal;

    @Column(name = "per_customer")
    private Integer perCustomer;

    @Column(name = "per_day")
    private Integer perDay;

    // One-to-one relationship with validation rule
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validation_rule_id", nullable = false)
    private ValidationRuleEntity validationRule;

    public RuleUsageLimitsEntity() {
        super();
    }

    public RuleUsageLimitsEntity(Integer perCodeTotal, Integer perCustomer, Integer perDay) {
        super();
        this.perCodeTotal = perCodeTotal;
        this.perCustomer = perCustomer;
        this.perDay = perDay;
    }
}