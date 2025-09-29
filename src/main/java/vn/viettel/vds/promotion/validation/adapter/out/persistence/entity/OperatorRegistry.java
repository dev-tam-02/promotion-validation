package vn.viettel.vds.promotion.validation.adapter.out.persistence.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Document(collection = "operator_registry")
public class OperatorRegistry {

    @Id
    private String id;

    @Indexed
    private String context;

    private Map<String, Object> jsonSchema;

    private String compilerId;

    private Instant createdAt;

    private Instant updatedAt;

    public OperatorRegistry() {
    }

    public OperatorRegistry(String id, String context, Map<String, Object> jsonSchema, String compilerId) {
        this.id = id;
        this.context = context;
        this.jsonSchema = jsonSchema;
        this.compilerId = compilerId;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public Map<String, Object> getJsonSchema() {
        return jsonSchema;
    }

    public void setJsonSchema(Map<String, Object> jsonSchema) {
        this.jsonSchema = jsonSchema;
    }

    public String getCompilerId() {
        return compilerId;
    }

    public void setCompilerId(String compilerId) {
        this.compilerId = compilerId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}