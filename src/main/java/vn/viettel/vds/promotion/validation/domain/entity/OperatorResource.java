package vn.viettel.vds.promotion.validation.domain.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "operator_resources")
@CompoundIndex(name = "by_operator", def = "{'tenantId': 1, 'operatorName': 1, 'operatorVersion': 1}")
public class OperatorResource {

    @Id
    private String id;

    @Field("tenantId")
    private String tenantId;

    @Field("operatorName")
    private String operatorName;

    @Field("operatorVersion")
    private Integer operatorVersion;

    @Field("resourceId")
    private String resourceId;

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getOperatorName() { return operatorName; }
    public void setOperatorName(String operatorName) { this.operatorName = operatorName; }

    public Integer getOperatorVersion() { return operatorVersion; }
    public void setOperatorVersion(Integer operatorVersion) { this.operatorVersion = operatorVersion; }

    public String getResourceId() { return resourceId; }
    public void setResourceId(String resourceId) { this.resourceId = resourceId; }
}