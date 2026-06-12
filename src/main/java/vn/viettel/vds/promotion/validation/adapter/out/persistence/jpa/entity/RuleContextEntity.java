package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * JPA entity for the {@code rule_contexts} lookup table.
 *
 * <p>Primary key is the context {@code code} (no surrogate id). Display text lives
 * in the service-wide {@code translations} table, not here.</p>
 */
@Getter
@Setter
@Entity
@Table(name = "rule_contexts")
public class RuleContextEntity {

    @Id
    @Column(name = "code", length = 100, nullable = false)
    private String code;

    @Column(name = "status", length = 20, nullable = false)
    private String status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
