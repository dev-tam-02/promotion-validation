package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity;

import com.promix.platform.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "time_exceptions", indexes = {
        @Index(name = "idx_time_exceptions_policy_from_ts", columnList = "temporal_policy_id, from_ts")
})
public class TimeExceptionEntity extends BaseEntity {

    @Column(name = "from_ts", nullable = false)
    private Instant fromTs;

    @Column(name = "to_ts", nullable = false)
    private Instant toTs;

    @Column(name = "mode", nullable = false, length = 20)
    private String mode; // "DENY" | "ALLOW"

    @Column(name = "reason", length = 500)
    private String reason;

    // Many-to-one relationship with temporal policy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "temporal_policy_id", nullable = false)
    private TemporalPolicyEntity temporalPolicy;

    public TimeExceptionEntity() {
        super();
    }

    public TimeExceptionEntity(Instant fromTs, Instant toTs, String mode, String reason) {
        super();
        this.fromTs = fromTs;
        this.toTs = toTs;
        this.mode = mode;
        this.reason = reason;
    }
}