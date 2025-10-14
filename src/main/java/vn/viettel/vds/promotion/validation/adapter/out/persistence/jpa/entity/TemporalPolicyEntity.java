package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity;

import com.promix.platform.jpa.converter.ListStringConverter;
import com.promix.platform.jpa.converter.MapStringObjectConverter;
import com.promix.platform.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Entity
@Table(name = "temporal_policies")
public class TemporalPolicyEntity extends BaseEntity {

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "tz", length = 50)
    private String tz;

    @Column(name = "start_ts")
    private Instant startTs;

    @Column(name = "end_ts")
    private Instant endTs;

    @Column(name = "rrule", length = 1000)
    private String rrule; // RRULE string

    @Convert(converter = ListStringConverter.class)
    @Column(name = "rdate", columnDefinition = "TEXT")
    @SuppressWarnings("java:S1948") // List content is converted to JSON by ListStringConverter
    private List<String> rdate = new ArrayList<>(); // List of RDATE strings

    @Column(name = "exrule", length = 1000)
    private String exrule; // EXRULE string

    @Convert(converter = ListStringConverter.class)
    @Column(name = "exdate", columnDefinition = "TEXT")
    @SuppressWarnings("java:S1948") // List content is converted to JSON by ListStringConverter
    private List<String> exdate = new ArrayList<>(); // List of EXDATE strings

    @Convert(converter = MapStringObjectConverter.class)
    @Column(name = "metadata", columnDefinition = "TEXT")
    @SuppressWarnings("java:S1948") // Map content is converted to JSON by MapStringObjectConverter
    private Map<String, Object> metadata;

    // One-to-many relationship with time of day windows
    @OneToMany(mappedBy = "temporalPolicy", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @SuppressWarnings("java:S1948") // JPA managed relationship, not serialized directly
    private List<TemporalPolicyWindowEntity> timeOfDayWindows = new ArrayList<>();

    // One-to-many relationship with rule temporal links
    @OneToMany(mappedBy = "temporalPolicy", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @SuppressWarnings("java:S1948") // JPA managed relationship, not serialized directly
    private List<RuleTemporalLinkEntity> ruleLinks = new ArrayList<>();

    // One-to-many relationship with time exceptions
    @OneToMany(mappedBy = "temporalPolicy", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @SuppressWarnings("java:S1948") // JPA managed relationship, not serialized directly
    private List<TimeExceptionEntity> timeExceptions = new ArrayList<>();

    public TemporalPolicyEntity() {
        super();
    }

    public TemporalPolicyEntity(String name, String tz) {
        super();
        this.name = name;
        this.tz = tz;
    }
}