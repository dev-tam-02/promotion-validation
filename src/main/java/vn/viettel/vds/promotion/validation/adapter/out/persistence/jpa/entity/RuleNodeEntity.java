package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity;

import com.promix.platform.jpa.converter.ListStringConverter;
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
@Table(name = "rule_nodes", indexes = {
        @Index(name = "idx_rule_nodes_validation_rule_id", columnList = "validation_rule_id"),
        @Index(name = "idx_rule_nodes_parent_id", columnList = "parent_id"),
        @Index(name = "idx_rule_nodes_node_order", columnList = "node_order")
})
public class RuleNodeEntity extends BaseEntity {

    @Column(name = "node_id", nullable = false, length = 100)
    private String nodeId;

    @Column(name = "type", nullable = false, length = 20)
    private String type; // "GROUP" | "COND"

    @Column(name = "group_logic", length = 20)
    private String groupLogic; // "ALL" | "ANY" | "NONE" (for GROUP type)

    @Convert(converter = ListStringConverter.class)
    @Column(name = "children_ids", columnDefinition = "TEXT")
    private List<String> childrenIds = new ArrayList<>(); // child node IDs (for GROUP type)

    @Column(name = "node_order")
    private Integer order;

    @Column(name = "operator_name", length = 100)
    private String operatorName; // (for COND type)

    @Convert(converter = MapStringObjectConverter.class)
    @Column(name = "params", columnDefinition = "TEXT")
    @Transient
    private Map<String, Object> params; // (for COND type)

    @Column(name = "reason_code", length = 100)
    private String reasonCode; // (for COND type)

    // Many-to-one relationship with validation rule
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validation_rule_id", nullable = false)
    private ValidationRuleEntity validationRule;

    // Self-referencing parent-child relationship
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private RuleNodeEntity parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RuleNodeEntity> children = new ArrayList<>();

    public RuleNodeEntity() {
        super();
    }

    public RuleNodeEntity(String nodeId, String type) {
        super();
        this.nodeId = nodeId;
        this.type = type;
    }
}