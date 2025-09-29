package vn.viettel.vds.promotion.validation.domain.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.Map;

@Document(collection = "operators")
@CompoundIndexes({
    @CompoundIndex(name = "by_name_version", def = "{'tenantId': 1, 'name': 1, 'version': -1}", unique = true),
    @CompoundIndex(name = "list_context_status", def = "{'tenantId': 1, 'context': 1, 'status': 1}")
})
public class Operator {

    @Id
    private String id;

    @Field("tenantId")
    private String tenantId;

    @Field("name")
    private String name;

    @Field("version")
    private Integer version;

    @Field("context")
    private String context;

    @Field("jsonSchema")
    private Map<String, Object> jsonSchema;

    @Field("compilerId")
    private String compilerId;

    @Field("status")
    private OperatorStatus status;

    @Field("createdAt")
    private Instant createdAt;

    @Field("updatedAt")
    private Instant updatedAt;

    public enum OperatorStatus {
        ACTIVE, DEPRECATED
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    public String getContext() { return context; }
    public void setContext(String context) { this.context = context; }

    public Map<String, Object> getJsonSchema() { return jsonSchema; }
    public void setJsonSchema(Map<String, Object> jsonSchema) { this.jsonSchema = jsonSchema; }

    public String getCompilerId() { return compilerId; }
    public void setCompilerId(String compilerId) { this.compilerId = compilerId; }

    public OperatorStatus getStatus() { return status; }
    public void setStatus(OperatorStatus status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}