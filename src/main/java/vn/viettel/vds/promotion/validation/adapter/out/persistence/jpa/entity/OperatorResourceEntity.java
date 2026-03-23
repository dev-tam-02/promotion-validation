package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity;

import com.promix.platform.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "operator_resources", indexes = {
        @Index(name = "idx_operator_resources_operator_name", columnList = "operator_name"),
        @Index(name = "idx_operator_resources_resource_id", columnList = "resource_id")
})
@EntityListeners(IdGenerationListener.class)
@EqualsAndHashCode(callSuper = true)
public class OperatorResourceEntity extends BaseEntity {

    @Column(name = "operator_name", nullable = false, length = 100)
    private String operatorName;

    // Many-to-one relationship with operator registry
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operator_registry_id", nullable = false)
    private OperatorRegistryEntity operatorRegistry;

    // Many-to-one relationship with resource
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id", nullable = false)
    private ResourceEntity resource;

    public OperatorResourceEntity() {
        super();
    }

    public OperatorResourceEntity(String operatorName) {
        super();
        this.operatorName = operatorName;
    }
}