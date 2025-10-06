package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity;

import com.promix.platform.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "temporal_policy_windows", indexes = {
        @Index(name = "idx_temporal_policy_windows_policy_id", columnList = "temporal_policy_id")
})
public class TemporalPolicyWindowEntity extends BaseEntity {

    @Column(name = "start_time", nullable = false, length = 10)
    private String start; // "HH:mm"

    @Column(name = "end_time", nullable = false, length = 10)
    private String end; // "HH:mm"

    // Many-to-one relationship with temporal policy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "temporal_policy_id", nullable = false)
    private TemporalPolicyEntity temporalPolicy;

    public TemporalPolicyWindowEntity() {
        super();
    }

    public TemporalPolicyWindowEntity(String start, String end) {
        super();
        this.start = start;
        this.end = end;
    }
}