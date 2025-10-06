package vn.viettel.vds.promotion.validation.adapter.out.persistence.mongo.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "operator_resources")
public class OperatorResource {

    @Id
    private String id;

    @Indexed
    private String operatorName;

    @Indexed
    private String resourceId;

    public OperatorResource() {
    }

    public OperatorResource(String id, String operatorName, String resourceId) {
        this.id = id;
        this.operatorName = operatorName;
        this.resourceId = resourceId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }
}