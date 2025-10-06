package vn.viettel.vds.promotion.validation.adapter.out.persistence.mongo.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.Map;

@Document(collection = "admin_audit_logs")
@CompoundIndex(name = "by_action_time", def = "{'tenantId': 1, 'action': 1, 'at': -1}")
public class AuditLog {

    @Id
    private String id;

    @Field("tenantId")
    private String tenantId;

    @Field("actor")
    private String actor;

    @Field("action")
    private AuditAction action;

    @Field("target")
    private AuditTarget target;

    @Field("diff")
    private Map<String, Object> diff;

    @Field("at")
    private Instant at;

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

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public AuditAction getAction() {
        return action;
    }

    public void setAction(AuditAction action) {
        this.action = action;
    }

    public AuditTarget getTarget() {
        return target;
    }

    public void setTarget(AuditTarget target) {
        this.target = target;
    }

    public Map<String, Object> getDiff() {
        return diff;
    }

    public void setDiff(Map<String, Object> diff) {
        this.diff = diff;
    }

    public Instant getAt() {
        return at;
    }

    public void setAt(Instant at) {
        this.at = at;
    }

    public enum AuditAction {
        RULE_CREATE, RULE_EDIT, RULE_PUBLISH, OP_CREATE, ASSIGN_UPDATE
    }

    public static class AuditTarget {
        @Field("type")
        private String type;

        @Field("id")
        private String id;

        // Getters and setters
        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }
}