package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity;

import com.promix.platform.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "rule_time_frames", indexes = {
        @Index(name = "idx_rule_time_frames_rule_id", columnList = "validation_rule_id"),
        @Index(name = "idx_rule_time_frames_frame_id", columnList = "time_frame_id")
})
@EntityListeners(IdGenerationListener.class)
@EqualsAndHashCode(callSuper = true)
public class RuleTimeFrameEntity extends BaseEntity {

    @Column(name = "time_frame_id", nullable = false, length = 100)
    private String timeFrameId;

    @Column(name = "mode", nullable = false, length = 20)
    private String mode; // "ALLOW" | "DENY" (blackout)

    // Many-to-one relationship with validation rule
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validation_rule_id", nullable = false)
    private ValidationRuleEntity validationRule;

    public RuleTimeFrameEntity() {
        super();
    }

    public RuleTimeFrameEntity(String timeFrameId, String mode) {
        super();
        this.timeFrameId = timeFrameId;
        this.mode = mode;
    }
}