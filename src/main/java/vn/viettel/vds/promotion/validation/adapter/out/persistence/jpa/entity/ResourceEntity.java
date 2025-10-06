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
@Table(name = "resources")
public class ResourceEntity extends BaseEntity {

    @Column(name = "label", nullable = false, length = 500)
    private String label;

    @Column(name = "endpoint", nullable = false, length = 500)
    private String endpoint;

    @Column(name = "method", nullable = false, length = 20)
    private String method;

    @Column(name = "auth_method", length = 50)
    private String authMethod;

    @Column(name = "auth_config", length = 1000)
    private String authConfig;

    @Convert(converter = MapStringObjectConverter.class)
    @Column(name = "headers", columnDefinition = "TEXT")
    private Map<String, Object> headers;

    @Column(name = "field_key", length = 100)
    private String fieldKey;

    @Column(name = "field_value", length = 200)
    private String fieldValue;

    @Column(name = "timeout_seconds", nullable = false)
    private Integer timeoutSeconds = 5;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 2;

    @Column(name = "backoff_ms", nullable = false)
    private Integer backoffMs = 200;

    // One-to-many relationship with operator resources
    @OneToMany(mappedBy = "resource", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OperatorResourceEntity> operatorResources = new ArrayList<>();

    public ResourceEntity() {
        super();
    }

    public ResourceEntity(String label, String endpoint, String method) {
        super();
        this.label = label;
        this.endpoint = endpoint;
        this.method = method;
        this.timeoutSeconds = 5;
        this.retryCount = 2;
        this.backoffMs = 200;
    }
}