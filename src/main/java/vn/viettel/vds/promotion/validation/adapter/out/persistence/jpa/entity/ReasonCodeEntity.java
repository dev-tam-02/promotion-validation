package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity;

import com.promix.platform.jpa.converter.MapStringObjectConverter;
import com.promix.platform.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@Entity
@Table(name = "reason_codes")
public class ReasonCodeEntity extends BaseEntity {

    @Column(name = "category", length = 100)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", length = 20)
    private Severity severity;

    @Convert(converter = MapStringObjectConverter.class)
    @Column(name = "labels", columnDefinition = "TEXT")
    @SuppressWarnings("java:S1948") // Map content is converted to JSON by MapStringObjectConverter
    private Map<String, Object> labels;

    public enum Severity {
        INFO, WARN, ERROR
    }
}
