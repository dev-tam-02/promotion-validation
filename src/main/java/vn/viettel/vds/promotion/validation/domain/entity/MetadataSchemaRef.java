package vn.viettel.vds.promotion.validation.domain.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

@Document(collection = "metadata_schemas_refs")
public class MetadataSchemaRef {

    @Id
    private String id;

    @Field("tenantId")
    private String tenantId;

    @Field("resourceType")
    private String resourceType;

    @Field("schemaService")
    private String schemaService;

    @Field("schemaId")
    private String schemaId;

    @Field("active")
    private Boolean active;

    @Field("createdAt")
    private Instant createdAt;

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getSchemaService() {
        return schemaService;
    }

    public void setSchemaService(String schemaService) {
        this.schemaService = schemaService;
    }

    public String getSchemaId() {
        return schemaId;
    }

    public void setSchemaId(String schemaId) {
        this.schemaId = schemaId;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}