package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity;

import com.promix.platform.core.util.IdGenerator;
import com.promix.platform.jpa.entity.BaseEntity;
import jakarta.persistence.PrePersist;

/**
 * JPA entity listener that generates UUIDv7 IDs on persist.
 */
public class IdGenerationListener {

    @PrePersist
    public void generateId(Object entity) {
        if (entity instanceof BaseEntity baseEntity && baseEntity.getId() == null) {
            baseEntity.setId(IdGenerator.generateId());
        }
    }
}
