package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity;

import com.promix.platform.jpa.converter.MapStringObjectConverter;
import com.promix.platform.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Entity
@Table(name = "operator_registry", indexes = {
        @Index(name = "idx_operator_registry_context", columnList = "context")
})
public class OperatorRegistryEntity extends BaseEntity {

    @Column(name = "context", nullable = false, length = 100)
    private String context;

    @Convert(converter = MapStringObjectConverter.class)
    @Column(name = "json_schema", columnDefinition = "TEXT")
    private transient Map<String, Object> jsonSchema;

    @Column(name = "compiler_id", length = 100)
    private String compilerId;

    // One-to-many relationship with operator resources
    @OneToMany(mappedBy = "operatorRegistry", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OperatorResourceEntity> operatorResources = new ArrayList<>();

    public OperatorRegistryEntity() {
        super();
    }

    public OperatorRegistryEntity(String context, Map<String, Object> jsonSchema, String compilerId) {
        super();
        this.context = context;
        this.jsonSchema = jsonSchema;
        this.compilerId = compilerId;
    }
}